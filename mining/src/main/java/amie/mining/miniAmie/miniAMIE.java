package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.output.comparisonToGroundTruth.CompareToGT;
import amie.rules.PruningMetric;
import amie.rules.Rule;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.mining.miniAmie.output.comparisonToGroundTruth.CompareToGT.PrintComparisonCSV;
import static amie.mining.miniAmie.output.GlobalSearchResult.PrintGlobalSearchResultCSV;
import static amie.mining.miniAmie.output.OutputRules.PrintOutputCSV;
import static amie.mining.miniAmie.utils.*;


public abstract class miniAMIE {
    public static AbstractKB Kb;

    public static int MaxRuleSize;
    public static PruningMetric PM;
    public static int MinSup;
    public static double MinHC;
    public static boolean EnableVariableSwitch = false ;
    public static boolean EnableConstants = false ;
    public static int NThreads = 1;
    public static boolean ShowRealSupport = false;
    public static boolean ShowExplorationLayers = false;
    public static boolean Verbose = false;
    public static double ErrorRateThreshold = 0.5;
    public static boolean CompareToGroundTruth = false;
    public static boolean OutputRules = true ;
    public static String RestrainedHead;
    public static String PathToGroundTruthRules;
    
    public static String Timestamp = Instant.now().toString().replace(" ", "_") ;
    public static String Suffix =   Timestamp + ".csv";

    public static String OutputRulesCsvPath = "./rules-" + Suffix;
    public static String OutputComparisonCsvPath = "./comparison-" + Suffix;
    public static String OutputConfigurationCsvPath = "./run-" + Suffix;

    public static boolean OutputConfigurationToAlreadyExistingCSV = false;

    public static final int CORRECTION_FACTOR_CLOSURE = 2;
    public static final int CORRECTION_FACTOR_OPENING = 4;

    public static List<Integer> SelectedRelations = new ArrayList<>();
    protected static ThreadPoolExecutor executor;

    protected static AtomicInteger totalSumExploredRules = new AtomicInteger();
    protected static AtomicInteger totalSumExploredRulesAdjustedWithBidirectionality = new AtomicInteger();
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
            totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
            totalSumExploredRulesAdjustedWithBidirectionality.addAndGet
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
                    totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
                    totalSumExploredRulesAdjustedWithBidirectionality
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

    public static void Run() {

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

        Collection<MiniAmieRule> initRulesInstantiatedParameter = GetInitRulesWithInstantiatedParameter(MinSup) ;

        // Multicore execution
        System.out.println("Running mini-AMIE with " + NThreads + " threads.");
        if (NThreads == 1) {
                RunSearchTreeMonoCore(initRulesUninstantiated, finalRulesUninstantiated);
                RunSearchTreeMonoCore(initRulesInstantiatedParameter, finalRulesAcyclicInstantiatedVariables);
        } else {
            System.out.println("Exploring uninstantiated rules...");
            RunSearchTreeMultiCore(initRulesUninstantiated, finalRulesUninstantiated);
            System.out.println("Exploring acyclic instantiated rules...");
            RunSearchTreeMultiCore(initRulesInstantiatedParameter, finalRulesAcyclicInstantiatedVariables);
        }
        totalSumExploredRules.addAndGet(initRulesUninstantiated.size());
        totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(initRulesUninstantiated.size());
        totalSumExploredRules.addAndGet(initRulesInstantiatedParameter.size());
        totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(initRulesInstantiatedParameter.size());

        if (OutputRules) {
            System.out.println("");
            System.out.println("Mini-AMIE rules output: ");
            PrintOutputCSV(finalRulesUninstantiated) ;
            System.out.println("/!\\ Acyclic instantiated mini-AMIE rules output: ");
            PrintOutputCSV(finalRulesAcyclicInstantiatedVariables) ;
        }

        if (CompareToGroundTruth) {
            System.out.println("");
            System.out.println("Comparison to ground truth: ");
            PrintComparisonCSV(finalRulesUninstantiated, groundTruthRules) ;
        }

        PrintGlobalSearchResultCSV(
                startTime,
                totalSumExploredRules,
                totalSumExploredRulesAdjustedWithBidirectionality) ;

        System.out.println("Thank you for using mini-Amie. See you next time");

        if(NThreads > 1)
            executor.shutdown();
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
