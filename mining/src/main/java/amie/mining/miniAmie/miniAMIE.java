package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.output.comparisonToGroundTruth.CompareToGT;
import amie.mining.utils.GlobalSearchResult;
import amie.rules.PruningMetric;
import amie.rules.Rule;

import java.sql.Time;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.mining.miniAmie.output.OutputRules.*;
import static amie.mining.miniAmie.output.comparisonToGroundTruth.CompareToGT.PrintComparisonCSV;
import static amie.mining.miniAmie.utils.*;
import static amie.mining.utils.GlobalSearchResult.*;


public abstract class miniAMIE {
    public static AbstractKB Kb;

    public static int MaxRuleSize;
    public static PruningMetric PM;
    public static int MinSup;
    public static double MinHC;
    public static boolean EnableVariableSwitch = false ;
    public static boolean EnableConstants = false ;
    public static boolean UseDirectionalSelectivity = false ;
    public static boolean ComputeActualMetrics = false;
    public static int NThreads = 1;
    public static boolean Verbose = false;
    public static boolean CompareToGroundTruth = false;
    public static boolean OutputRules = true ;
    public static boolean UseAnyBurlOutputFormat = false ;
    public static String RestrainedHead;
    public static String PathToGroundTruthRules;

    public static boolean CustomRulesPath = false ;
    public static String OutputRulesPath ;

    public static String OutputComparisonCsvPath = "./comparison-" + Suffix;

    public static final int CORRECTION_FACTOR_CLOSURE = 2;
    public static final int CORRECTION_FACTOR_OPENING = 4;

    public static List<Integer> SelectedRelations = new ArrayList<>();
    protected static ThreadPoolExecutor executor;

    protected static AtomicInteger old_SearchSPace = new AtomicInteger();
    protected static AtomicInteger searchSpace = new AtomicInteger();
    protected static Lock lock = new ReentrantLock();
    protected static CountDownLatch headLatch ;
    protected static List<MiniAmieClosedRule> finalRulesUninstantiated = new ArrayList<>() ;
    protected static List<MiniAmieClosedRule> finalRulesAcyclicInstantiatedVariables = new ArrayList<>() ;

    // SubtreeExploration explores a subtree of rules from a head start
    protected static class SubtreeExploration implements Callable<Void> {
        MiniAmieRule initRule ;
        List<MiniAmieClosedRule> finalRules ;
        public SubtreeExploration(MiniAmieRule initRule, List<MiniAmieClosedRule> finalRules) {
            this.initRule = initRule;
            this.finalRules = finalRules;
        }
        @Override
        public Void call() throws Exception {
            ExplorationResult exploreChildrenResult = InitExploreChildren(initRule);
            old_SearchSPace.addAndGet(exploreChildrenResult.sumExploredRules);
            searchSpace.addAndGet
                    (exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
            lock.lock();
            finalRules.addAll(List.copyOf(exploreChildrenResult.finalRules));
            lock.unlock();
            headLatch.countDown();
            return null ;
        }
    }


    private static void RunSearchTreeMonoCore(Collection<MiniAmieRule> initRules, List<MiniAmieClosedRule> finalRules) {
        for (MiniAmieRule rule : initRules) {
            try {
                if (RestrainedHead == null || rule.toString().contains(RestrainedHead)) {
                    ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                    old_SearchSPace.addAndGet(exploreChildrenResult.sumExploredRules);
                    searchSpace
                            .addAndGet(exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
                    finalRules.addAll(exploreChildrenResult.finalRules);
                }
            } catch (Exception e) {
                System.err.println("Exception while exploring " + rule + " subtree");
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    private static void RunSearchTreeMultiCore
            (Collection<MiniAmieRule> initRules, List<MiniAmieClosedRule> finalRules) {
        try {
            headLatch = new CountDownLatch(initRules.size());
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NThreads);

            for (MiniAmieRule initRule : initRules) {
                if (RestrainedHead == null || initRule.toString().contains(RestrainedHead))
                    executor
                            .submit(new SubtreeExploration(initRule, finalRules))
                            .get();
                else
                    System.out.println("Skipping subtree " + initRule + ".");
            }
            if (!headLatch.await(10, TimeUnit.MICROSECONDS))
                throw new TimeoutException(("Latch Timeout: "
                        + "\n latch value " + headLatch.getCount()
//                            + "\n subtree(s) left : " + Arrays.toString(exploringRules.toArray())
                        + "\n executor completed jobs : " + executor.getCompletedTaskCount()
                        + "/" + initRules.size()
                        + "\n queue size " + executor.getQueue().size())) ;
        } catch (Exception e) {
            throw new IllegalArgumentException("Mini-AMIE multicore error.", e) ;
        }
    }

    public static void ResetSelectivity() {
        if (UseDirectionalSelectivity) {
            MiniAmieRule.setSelectivity(new utils.SurvivalRateSelectivity()) ;
        } else {
            MiniAmieRule.setSelectivity(new utils.JacquardSelectivity()) ;
        }
    }

    public static void Run() {

        System.out.println(UseAnyBurlOutputFormat ? "Using AnyBurl output format." : "Using CSV output format.") ;
        if (!CustomRulesPath) {
            if (UseAnyBurlOutputFormat) {
                OutputRulesPath = Timestamp + RulesExtension;
                System.out.println("Using default output path for AnyBurl-style rules :" + OutputRulesPath);
            } else {
                OutputRulesPath = "./rules-" + Timestamp + CSVExtension ;
                System.out.println("Using default CSV rules output path :" + OutputRulesPath);
            }
        }

        // Choosing selectivity method for support approximation

        ResetSelectivity();
        System.out.println("Selectivity set to " +
                (UseDirectionalSelectivity ? "Survival Rate" : "Jacquard")) ;

        List<Rule> groundTruthRules = new ArrayList<>();
        if (CompareToGroundTruth) {
            // Generating comparison map
            groundTruthRules = CompareToGT.LoadGroundTruthRules();
        }
        long startTime = System.currentTimeMillis();

        // Init mining assistant (temp solution)
        miningAssistant = new DefaultMiningAssistant(Kb);

        SelectedRelations = SelectRelations();

        System.out.println("Using " + PM + " as pruning metric with minimum threshold " +
                (PM == PruningMetric.ApproximateSupport || PM == PruningMetric.Support ? MinSup : MinHC));

        Collection<MiniAmieRule> initRulesUninstantiated = GetInitRules(MinSup);

        Collection<MiniAmieRule> initRulesInstantiatedParameter = EnableConstants ?
                GetInitRulesWithInstantiatedParameter(MinSup) : new ArrayList<>() ;

        // Multicore execution
        System.out.println("Running mini-AMIE with " + NThreads + " threads.");
        if (NThreads == 1) {
                RunSearchTreeMonoCore(initRulesUninstantiated, finalRulesUninstantiated);
                if (EnableConstants) RunSearchTreeMonoCore(initRulesInstantiatedParameter, finalRulesAcyclicInstantiatedVariables);
        } else {
            System.out.println("Exploring uninstantiated rules...");
            RunSearchTreeMultiCore(initRulesUninstantiated, finalRulesUninstantiated);
            if (EnableConstants) {
                System.out.println("Exploring acyclic instantiated rules...");
                RunSearchTreeMultiCore(initRulesInstantiatedParameter, finalRulesAcyclicInstantiatedVariables);
            }
        }
        old_SearchSPace.addAndGet(initRulesUninstantiated.size());
        searchSpace.addAndGet(initRulesUninstantiated.size());
        old_SearchSPace.addAndGet(initRulesInstantiatedParameter.size());
        searchSpace.addAndGet(initRulesInstantiatedParameter.size());

        if (OutputRules) {

            System.out.println("Mini-AMIE rules output: ");
            List<MiniAmieClosedRule> closedRules ;
            if (EnableConstants){
                closedRules = new ArrayList<>() ;
                closedRules.addAll(finalRulesUninstantiated) ;
                closedRules.addAll(finalRulesAcyclicInstantiatedVariables) ;
            } else {
                closedRules = finalRulesUninstantiated ;
            }

            if (UseAnyBurlOutputFormat) {
                PrintOutputAnyBurlFormat(closedRules, OutputRulesPath);
            } else {
                PrintOutputCSV(closedRules, OutputRulesPath) ;
            }

        }

        if (CompareToGroundTruth) {
            System.out.println("\nComparison to ground truth: ");
            PrintComparisonCSV(finalRulesUninstantiated, groundTruthRules) ;
        }

        // Displaying result
        long duration = System.currentTimeMillis() - startTime;
        printMiniAmieResultInfo(duration) ;

        PrintGlobalSearchResultToCSV(
                MaxRuleSize,
                PM,
                MinSup,
                MinHC,
                NThreads,
                duration,
                searchSpace.get()
        ) ;

        System.out.println("Thank you for using mini-Amie. See you next time");

        if(NThreads > 1)
            executor.shutdown();
    }

    private static void printMiniAmieResultInfo(long duration) {
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - 24 * days;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - 60 * hours;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 60 * minutes;
        long milliseconds = duration - 1000 * seconds;

        System.out.println("Search duration: " +
                (days > 0 ? days + " days " : "") +
                (hours > 0 ? hours + " hours " : "") +
                (minutes > 0 ? minutes + " minutes " : "") +
                (seconds > 0 ? seconds + " seconds " : "") +
                milliseconds + " milliseconds" +
                ".");
        System.out.println("Search space size approximation: " + searchSpace + " possibilities.");

        System.out.print("Bidirectional relations (range-dom Jaccard >= " + BidirectionalityJaccardThreshold + "):");
        for (int relation : bidirectionalityMap.keySet())
            if (bidirectionalityMap.get(relation)) {
                System.out.print(" " + Kb.unmap(relation) );
            }
        System.out.println();
    }

    private static ExplorationResult ExploreClosedChildren(MiniAmieRule rule) {

        ArrayList<MiniAmieClosedRule> keptRules = new ArrayList<>();
        int searchSpaceEstimatedSize = 0;
        int searchSpaceEstimatedAdjustedWithBidirectionalitySize = 0;

        ArrayList<MiniAmieClosedRule> closedChildren = rule.AddClosure();

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE;

            for (MiniAmieClosedRule closedChild : closedChildren) {
                searchSpaceEstimatedAdjustedWithBidirectionalitySize +=
                        closedChild.getCorrectingFactor();

                if (closedChild.IsNotPruned()) {
                    keptRules.add(closedChild);
                }
            }
        }

        return new ExplorationResult(
                searchSpaceEstimatedSize,
                searchSpaceEstimatedAdjustedWithBidirectionalitySize,
                keptRules
        ) ;

    }

    private static ExplorationResult ExploreOpenChildren(MiniAmieRule rule,
                                                         ExplorationResult explorationResult) {

        ArrayList<MiniAmieRule> openChildren = rule.AddDangling();
        if (openChildren != null) {
            explorationResult.sumExploredRules += openChildren.size() * CORRECTION_FACTOR_OPENING;

            for (MiniAmieRule openChild : openChildren) {
                explorationResult.sumExploredRulesAdjustedWithBidirectionality +=
                        openChild.getCorrectingFactor();

                if (openChild.IsNotPruned()) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    explorationResult.finalRules.addAll(exploreOpenChildResult.finalRules);
                    explorationResult.sumExploredRules += exploreOpenChildResult.sumExploredRules;
                    explorationResult.sumExploredRulesAdjustedWithBidirectionality +=
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality;
                }
            }
        }

        return explorationResult ;
    }

    private static ExplorationResult InitExploreChildren(MiniAmieRule rule) {
        ExplorationResult explorationResult = ExploreClosedChildren(rule) ;
        return ExploreOpenChildren(rule, explorationResult) ;
    }

    private static ExplorationResult ExploreChildren(MiniAmieRule rule) {

        if (rule.ExceedsLengthWhenClosing())
            return new ExplorationResult(0, 0,
                    Collections.emptyList());

        ExplorationResult explorationResult = ExploreClosedChildren(rule) ;

        if (rule.ExceedsLengthWhenOpening())
            return explorationResult;

        return ExploreOpenChildren(rule, explorationResult);
    }

}
