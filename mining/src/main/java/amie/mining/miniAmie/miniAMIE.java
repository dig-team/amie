package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.PruningMetric;
import amie.rules.Rule;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.mining.miniAmie.CompareToGT.PrintComparisonCSV;
import static amie.mining.miniAmie.GlobalSearchResult.PrintGlobalSearchResultCSV;
import static amie.mining.miniAmie.OutputRules.PrintOutputCSV;
import static amie.mining.miniAmie.utils.*;
import static amie.mining.miniAmie.utils.ApproximateSupportClosedRule;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static PruningMetric PM;
    public static int MinSup;
    public static double MinHC;
    public static int NThreads = 1;
    public static boolean ShowRealSupport = false;
    public static boolean ShowExplorationLayers = false;
    public static boolean Verbose = false;
    public static double ErrorRateThreshold = 0.5;
    public static boolean CompareToGroundTruth = false;
    public static boolean OutputRules = true ;
    public static String RestrainedHead;
    public static String pathToGroundTruthRules;
    static String timestamp = Instant.now().toString().replace(" ", "_") ;
    static int  runId = 0 ;
    static String suffix =   timestamp + ".csv";

    public static String OutputRulesCsvPath = "./rules-" + suffix;
    public static String OutputComparisonCsvPath = "./comparison-" + suffix;
    public static String OutputConfigurationCsvPath = "./run-" + suffix;

    public static boolean OutputConfigurationToAlreadyExistingCSV = false;

    static final int CORRECTION_FACTOR_CLOSURE = 2;
    static final int CORRECTION_FACTOR_OPENING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>();
    protected static ThreadPoolExecutor executor;

    protected static AtomicInteger totalSumExploredRules = new AtomicInteger();
    protected static AtomicInteger totalSumExploredRulesAdjustedWithBidirectionality = new AtomicInteger();
    protected static Lock lock = new ReentrantLock();
    protected static CountDownLatch headLatch ;
//    protected static Set<Rule> exploringRules = new HashSet<>() ;
    protected static List<Rule> finalRules = new ArrayList<>();

    // SubtreeExploration explores a subtree of rules from a head start
    protected static class SubtreeExploration implements Callable<Void> {
        Rule initRule ;
        public SubtreeExploration(Rule initRule) {
            this.initRule = initRule;
        }
        @Override
        public Void call() throws Exception {
            Thread.sleep(10);
            ExplorationResult exploreChildrenResult = InitExploreChildren(initRule);
            totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
            totalSumExploredRulesAdjustedWithBidirectionality.addAndGet
                    (exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
            lock.lock();
            finalRules.addAll(List.copyOf(exploreChildrenResult.finalRules));
//            exploringRules.remove(initRule);
            lock.unlock();
            headLatch.countDown();
            return null ;
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
        miningAssistant = new DefaultMiningAssistant(kb);

        SelectedRelations = SelectRelations();

        System.out.println("Using " + PM + " as pruning metric with minimum threshold " +
                (PM == PruningMetric.ApproximateSupport || PM == PruningMetric.Support ? MinSup : MinHC));

        Collection<Rule> initRules = GetInitRules(MinSup);

        // Multicore execution
        System.out.println("Running mini-AMIE with " + NThreads + " threads.");
        if (NThreads == 1) {
            for (Rule rule : initRules) {
                try {
                    if (RestrainedHead == null || rule.toString().contains(RestrainedHead)) {
                        ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                        totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
                        totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
                        finalRules.addAll(exploreChildrenResult.finalRules);
                    }
                } catch (Exception e) {
                    System.err.println("Exception while exploring " + rule + " subtree");
                    e.printStackTrace();
                    System.exit(1);
                }

            }
        } else {
            try {

                System.out.println("Exploring ...");
                headLatch = new CountDownLatch(initRules.size());
                executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NThreads);

                for (Rule initRule : initRules) {
                    if (RestrainedHead == null || initRule.toString().contains(RestrainedHead))
                        executor
                                .submit(new SubtreeExploration(initRule))
                                .get(); // Exception can be raised from here
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
                System.err.println("Mini-AMIE multicore error: \n" + e.getMessage());
                System.exit(1);
            }

        }
        totalSumExploredRules.addAndGet(initRules.size());
        totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(initRules.size());

        if (OutputRules) {
            System.out.println("");
            System.out.println("Mini-AMIE rules output: ");
            PrintOutputCSV(finalRules) ;
        }

        if (CompareToGroundTruth) {
            System.out.println("");
            System.out.println("Comparison to ground truth: ");
            PrintComparisonCSV(finalRules, groundTruthRules) ;
        }

        PrintGlobalSearchResultCSV(
                startTime,
                totalSumExploredRules,
                totalSumExploredRulesAdjustedWithBidirectionality) ;

        System.out.println("Thank you for using mini-Amie. See you next time");

        if(NThreads > 1)
            executor.shutdown();
    }

    // TODO replace that with IsPrunedClosedRule static method attribute to avoid repeated PruningMetric check
    private static boolean ClosedRuleIsKept(Rule rule) {
        switch(PM) {
            case ApproximateSupport -> {
                return ApproximateSupportClosedRule(rule) >= MinSup ;
            }
            case ApproximateHeadCoverage -> {
                return ApproximateHeadCoverageClosedRule(rule) >= MinHC ;
            }
            case AlternativeApproximateSupport -> {
                return AltApproximateSupportClosedRule(rule) >= MinSup ;
            }
            case Support -> {
                return RealSupport(rule) >= MinSup ;
            }
            case HeadCoverage -> {
                return RealHeadCoverage(rule) >= MinHC ;
            }
            default -> {
                return false;
            }
        }
    }

    // TODO replace that with IsPrunedOpenRule static method attribute to avoid repeated PruningMetric check
    private static boolean OpenRuleIsKept(Rule rule) {
        switch(PM) {
            case ApproximateSupport -> {
                return ApproximateSupportOpenRule(rule) >= MinSup ;
            }
            case ApproximateHeadCoverage -> {
                return ApproximateHeadCoverageOpenRule(rule) >= MinHC ;
            }
            case AlternativeApproximateSupport -> {
                return AltApproximateSupportOpenRule(rule) >= MinSup ;
            }
            case Support -> {
                return RealSupport(rule) >= MinSup ;
            }
            case HeadCoverage -> {
                return RealHeadCoverage(rule) >= MinHC ;
            }
            default -> {
                return false;
            }
        }
    }



    private static ExplorationResult InitExploreChildren(Rule rule) {

//        System.out.println(rule+" : Init Exploring  subtrees");
        ArrayList<Rule> finalRules = new ArrayList<>();
        int searchSpaceEstimatedSize = 0;
        int searchSpaceEstimatedAdjustedWithBidirectionalitySize = 0;

        ArrayList<Pair<Rule, Integer>> closedChildren = AddClosureToEmptyBody(rule);

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE;

            for (Pair<Rule, Integer> closedChildCorrectionPair : closedChildren) {
                Rule closedChild = closedChildCorrectionPair.first;

                int correction = closedChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                if (ClosedRuleIsKept(closedChild)) {
                    finalRules.add(closedChild);
                }
            }
        }
//        System.out.println(rule+" : closedChildren Exploring  subtrees");

        ArrayList<Pair<Rule, Integer>> openChildren = AddDanglingToEmptyBody(rule);
        if (openChildren != null) {
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENING;

            for (Pair<Rule, Integer> openChildCorrectionPair : openChildren) {
                Rule openChild = openChildCorrectionPair.first;

                int correction = openChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                if (OpenRuleIsKept(openChild)) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    finalRules.addAll(exploreOpenChildResult.finalRules);
                    searchSpaceEstimatedSize += exploreOpenChildResult.sumExploredRules;
                    searchSpaceEstimatedAdjustedWithBidirectionalitySize +=
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality;
                }
            }
        }
//        System.out.println(rule+" : End Init Exploring  subtrees");

        return new ExplorationResult(searchSpaceEstimatedSize,
                searchSpaceEstimatedAdjustedWithBidirectionalitySize, finalRules);
    }

    private static ExplorationResult ExploreChildren(Rule rule) {

//        System.out.println(rule+" : Exploring  subtrees");
        if (rule.getBody().size() + 2 > miniAMIE.MaxRuleSize)
            return new ExplorationResult(0, 0,
                    Collections.emptyList());

        ArrayList<Rule> finalRules = new ArrayList<>();
        int searchSpaceEstimatedSize = 0;
        int searchSpaceEstimatedAdjustedWithBidirectionalitySize = 0;
        ArrayList<Pair<Rule, Integer>> closedChildren = AddClosure(rule);

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE;

            for (Pair<Rule, Integer> closedChildCorrectionPair : closedChildren) {
                Rule closedChild = closedChildCorrectionPair.first;
                int correction = closedChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;


                if (ClosedRuleIsKept(closedChild)) {
                    finalRules.add(closedChild);
                }
            }
        }

        if (rule.getBody().size() + 3 > miniAMIE.MaxRuleSize)
            return new ExplorationResult(searchSpaceEstimatedSize, searchSpaceEstimatedAdjustedWithBidirectionalitySize,
                    finalRules);

        ArrayList<Pair<Rule, Integer>> openChildren = AddDangling(rule);

        if (openChildren != null) {
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENING;

            for (Pair<Rule, Integer> openChildCorrectionPair : openChildren) {
                Rule openChild = openChildCorrectionPair.first;
                int correction = openChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                if (OpenRuleIsKept(rule)) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    finalRules.addAll(exploreOpenChildResult.finalRules);
                    searchSpaceEstimatedSize += exploreOpenChildResult.sumExploredRules;
                    searchSpaceEstimatedAdjustedWithBidirectionalitySize +=
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality;
                }
            }

        }

        return new ExplorationResult(searchSpaceEstimatedSize, searchSpaceEstimatedAdjustedWithBidirectionalitySize,
                finalRules);
    }
}
