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
    public static String RestrainedHead;
    public static String pathToGroundTruthRules;
    static String suffix = Instant.now().toString().replace(" ", "_") + ".csv";
    public static String outputComparisonCsvPath = "./comparison-" + suffix;
    public static String outputConfigurationCsvPath = "./run-" + suffix;

    static final int CORRECTION_FACTOR_CLOSURE = 2;
    static final int CORRECTION_FACTOR_OPENING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>();
    protected static ThreadPoolExecutor executor;

    public static void Run() {



        //
        List<Rule> groundTruthRules = new ArrayList<>();
        if (CompareToGroundTruth) {
            // Generating comparison map
            groundTruthRules = CompareToGT.LoadGroundTruthRules();
        }
        long startTime = System.currentTimeMillis();

        // Init mining assistant (temp solution)
        miningAssistant = new DefaultMiningAssistant(kb);

        SelectedRelations = SelectRelations();

        Collection<Rule> initRules = GetInitRules(MinSup);
        AtomicInteger totalSumExploredRules = new AtomicInteger();
        AtomicInteger totalSumExploredRulesAdjustedWithBidirectionality = new AtomicInteger();

        List<Rule> finalRules = new ArrayList<>();

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
                Lock lock = new ReentrantLock();
                System.out.println("Exploring ...");
                CountDownLatch headLatch = new CountDownLatch(initRules.size());
                executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NThreads);
                Set<Rule> exploringRules = new HashSet<>() ;
                for (Rule rule : initRules) {
                    exploringRules.add(rule);
                    if (RestrainedHead == null || rule.toString().contains(RestrainedHead)) {
                        executor.submit(() -> {
                            try {
//                                System.out.println(rule + " Thread " + Thread.currentThread().getName());
//                                System.out.println(rule + " sub ");
                                ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
//                                System.out.println(rule + " exploreChildrenResult "+exploreChildrenResult);
                                totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
//                                System.out.println(rule + " totalSumExploredRules "+totalSumExploredRules);
                                totalSumExploredRulesAdjustedWithBidirectionality.addAndGet
                                        (exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
//                                System.out.println(rule + " totalSumExploredRulesAdjustedWithBidirectionality "
//                                        +totalSumExploredRulesAdjustedWithBidirectionality);

                                lock.lock();
                                finalRules.addAll(List.copyOf(exploreChildrenResult.finalRules));
//                                System.out.println(rule + " finalRules "+finalRules);
                                exploringRules.remove(rule);
                                lock.unlock();
                                headLatch.countDown();
                            } catch (Exception e) {
                                System.err.println("Exception while exploring " + rule + " subtree");
                                e.printStackTrace();
                                System.exit(1);
                            }
                        });
                    } else {
                        System.out.println("Skipping rule " + rule + ".");
                    }
                }
                if (!headLatch.await(5000, TimeUnit.MILLISECONDS)) {
                    System.err.println("FAILED !! "
                            + "\n latch value " + headLatch.getCount()
                            + "\n tasks left : " + Arrays.toString(exploringRules.toArray())
                            + "\n executor completed jobs : "
                            + executor.getCompletedTaskCount()
                            + "/" + initRules.size()
                            + "\n queue size " + executor.getQueue().size());
                    System.exit(1);
                }


            } catch (Exception e) {
                System.err.println("Mini-AMIE multicore error: " + e.getMessage());
                System.exit(1);
            }

        }
        totalSumExploredRules.addAndGet(initRules.size());
        totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(initRules.size());

        if (CompareToGroundTruth) {
            System.out.println("");
            System.out.println("Comparison to ground truth: ");
            PrintComparisonCSV(finalRules, groundTruthRules) ;
        }
        PrintGlobalSearchResultCSV(startTime,
                totalSumExploredRules,
                totalSumExploredRulesAdjustedWithBidirectionality) ;

        System.out.println("Thank you for using mini-Amie. See you next time");

        if(NThreads > 1)
            executor.shutdown();
    }

    // TODO replace that with IsPrunedClosedRule static method attribute to avoid repeated PruningMetric check
    private static boolean IsKeptClosedRule(Rule rule) {
        if(PM == PruningMetric.Support)
            return ApproximateSupportClosedRule(rule) >= MinSup ;
        else
            return ApproximateHeadCoverageClosedRule(rule) >= MinHC ;
    }

    // TODO replace that with IsPrunedOpenRule static method attribute to avoid repeated PruningMetric check
    private static boolean IsKeptOpenRule(Rule rule) {
        if(PM == PruningMetric.Support)
            return ApproximateSupportOpenRule(rule) >= MinSup ;
        else
            return ApproximateHeadCoverageOpenRule(rule) >= MinHC ;
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

                if (IsKeptClosedRule(closedChild)) {
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

                if (IsKeptOpenRule(openChild)) {
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


                if (IsKeptClosedRule(closedChild)) {
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

                if (IsKeptOpenRule(rule)) {
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
