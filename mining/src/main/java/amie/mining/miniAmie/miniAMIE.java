package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
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

    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>();
    protected static ThreadPoolExecutor executor;

    public static void Run() {

        List<Rule> groundTruthRules = new ArrayList<>();
        if (CompareToGroundTruth) {
            // Generating comparison map
            groundTruthRules = LoadGroundTruthRules();
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
                if (RestrainedHead == null || rule.toString().contains(RestrainedHead)) {
                    ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                    totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
                    totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
                    finalRules.addAll(exploreChildrenResult.finalRules);
                }
            }
        } else {
            try {
                CountDownLatch headLatch = new CountDownLatch(NThreads);
                executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(NThreads);
                for (Rule rule : initRules) {
                    if (RestrainedHead == null || rule.toString().contains(RestrainedHead)) {
                        executor.submit(() -> {
                            ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                            totalSumExploredRules.addAndGet(exploreChildrenResult.sumExploredRules);
                            totalSumExploredRulesAdjustedWithBidirectionality.addAndGet
                                    (exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality);
                            finalRules.addAll(exploreChildrenResult.finalRules);
                            headLatch.countDown();
                            return null;
                        });
                    }
                }
                headLatch.await();
            } catch (Exception e) {
                System.err.println("Mini-AMIE multicore error: " + e.getMessage());
                System.exit(1);
            }

        }
        for (Rule _rule : initRules) {
            totalSumExploredRules.addAndGet(1);
            totalSumExploredRulesAdjustedWithBidirectionality.addAndGet(1);
        }

        // Displaying result
        long duration = System.currentTimeMillis() - startTime;
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
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");
        System.out.println("Search space approximation (adjusted with bidirectionality): " +
                totalSumExploredRulesAdjustedWithBidirectionality + " possibilities.");

        System.out.println("Bidirectional relations (range-dom Jaccard >= " + BidirectionalityJaccardThreshold + "):");
        for (int relation : bidirectionalityMap.keySet())
            if (bidirectionalityMap.get(relation)) {
                System.out.print(kb.unmap(relation) + " ");
            }
        System.out.println("");
        System.out.println("Approximate mining: ");
        if (CompareToGroundTruth) {

            // Generating comparison map
            ConcurrentHashMap<Rule, RuleStateComparison> comparisonMap = new ConcurrentHashMap<>();
            for (Rule rule : finalRules)
                comparisonMap.put(rule, RuleStateComparison.FALSE);
            for (Rule groundTruthRule : groundTruthRules) {
                boolean found = false;
                for (Rule rule : finalRules) {
                    if (CompareRules(rule, groundTruthRule)) {
                        found = true;
                        comparisonMap.put(rule, RuleStateComparison.CORRECT);
                        break;
                    }
                }
                if (!found) {
                    if (ShouldHaveBeenFound(groundTruthRule))
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_FAILURE);
                    else
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_OK);
                }
            }

            // Displaying comparison map
            System.out.println(" Comparison to ground truth: ");


            try {
                File outputComparisonCsvFile = new File(outputComparisonCsvPath);
                if (outputComparisonCsvFile.createNewFile()) {
                    System.out.println("Created CSV comparison to ground rules output: " + outputComparisonCsvPath);
                } else {
                    System.err.println("Could not create CSV output: " + outputComparisonCsvPath +
                            ". Maybe name already exists?");
                }

                FileWriter outputComparisonCsvWriter = new FileWriter(outputComparisonCsvPath);

                String csvColumnLine = String.format(
                        "rule" + sep // RULE
                                + "headRelation" + sep
                                + "size" + sep
                                + "isFalse" + sep // FALSE
                                + "isCorrect" + sep // CORRECT
                                + "isMissingFailure" + sep // MISSING_FAILURE
                                + "isMissingOK" + sep // MISSING_OK
                                + "isPerfectPath" + sep
                                + "hasRedundancies" + sep
                                + "appSupport" + sep // APP SUPPORT
                                + "realSupport" + sep
                                + "appSupportNano" + sep
                                + "realSupportNano" + sep
                                + "relationHeadAtom" + sep
                                + "relationFirstBodyAtom" + sep
                                + "relationLastBodyAtom" + sep
                                + "headAtomObjectToFirstBodyAtomObjectOverlap" + sep
                                + "lastBodyAtomSubjectToHeadAtomSubjectOverlap" + sep
                                + "relationFirstBodyAtomSize" + sep
                                + "relationHeadSize" + sep
                                + "rangeFirstBodyAtom" + sep
                                + "domainHeadAtom" + sep
                                + "domainLastBodyAtom" + sep
                                + "ifunRelationFirstBodyAtom" + sep
                                + "funRelationHeadAtom" + sep
                                + "bodyEstimate" + sep
                                + "bodyProductElements"
                                + "\n"
                );
                outputComparisonCsvWriter.write(csvColumnLine);
                System.out.print(csvColumnLine);

                // Computing real support using available cores
                Set<Rule> totalRules = comparisonMap.keySet();
                if (NThreads == 1) {
                    for (Rule rule : totalRules)
                            rule.setSupport(RealSupport(rule));
                } else {
                    CountDownLatch totalRulesLatch = new CountDownLatch(totalRules.size());
                    for (Rule rule : totalRules) {
                        executor.submit(() -> {
                            rule.setSupport(RealSupport(rule));
                            totalRulesLatch.countDown();
                            return null;
                        });
                    }
                    totalRulesLatch.await();
                    executor.shutdown();
                }

                for (Rule rule : totalRules) {
                    String comparisonCharacter;
                    RuleStateComparison compRule = comparisonMap.get(rule);
                    switch (compRule) {
                        case FALSE -> comparisonCharacter = ANSI_YELLOW;
                        case CORRECT -> comparisonCharacter = ANSI_GREEN;
                        case MISSING_FAILURE -> comparisonCharacter = ANSI_RED;
                        case MISSING_OK -> comparisonCharacter = ANSI_PURPLE;
                        default -> throw new RuntimeException("Unknown comparison rule " + rule);
                    }

                    // Printing to csv file
                    long startReal = System.nanoTime();
                    double real = rule.getSupport();
                    long realNano = System.nanoTime() - startReal;
                    double app = -1;
                    long appNano = -1;
                    FactorsOfApproximateSupportClosedRule factors = new FactorsOfApproximateSupportClosedRule();
                    if (comparisonMap.get(rule) == RuleStateComparison.CORRECT || ShouldHaveBeenFound(rule)) {
                        long startApp = System.nanoTime();
                        app = ApproximateSupportClosedRule(rule);
                        appNano = System.nanoTime() - startApp;
                        factors = GetFactorsOfApproximateSupportClosedRule(rule);
                    }


                    String csvLine = String.format(
                            rule + sep + // RULE
                                    kb.unmap(rule.getHead()[RELATION_POSITION]) + sep // HEAD RELATION
                                    + (rule.getBody().size() + 1) + sep // RULE SIZE
                                    + (compRule == RuleStateComparison.FALSE ? 1 : 0) + sep // FALSE
                                    + (compRule == RuleStateComparison.CORRECT ? 1 : 0) + sep // CORRECT
                                    + (compRule == RuleStateComparison.MISSING_FAILURE ? 1 : 0) + sep // MISSING_FAILURE
                                    + (compRule == RuleStateComparison.MISSING_OK ? 1 : 0) + sep // MISSING_OK
                                    + (IsRealPerfectPath(rule) ? 1 : 0) + sep
                                    + (HasNoRedundancies(rule) ? 0 : 1) + sep
                                    + app + sep // APP SUPPORT
                                    + real + sep
                                    + appNano + sep
                                    + realNano + sep
                                    + factors
                                    + "\n"
                    );
                    outputComparisonCsvWriter.write(csvLine);
                    // Printing comparison to console
                    System.out.print(comparisonCharacter + csvLine + ANSI_RESET);
                }


            } catch (Exception e) {
//                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
                e.printStackTrace();
            }
        }

        // Outputing general information on run config
        try {
            File outputRunConfigCsvFile = new File(outputConfigurationCsvPath);
            if (outputRunConfigCsvFile.createNewFile()) {
                System.out.println("Created CSV run config output: " + outputConfigurationCsvPath);
            } else {
                System.err.println("Could not create CSV output: " + outputConfigurationCsvPath +
                        ". Maybe name already exists?");
            }

            FileWriter outputConfigurationCsvWriter = new FileWriter(outputConfigurationCsvPath);

            System.out.println("Run configuration :");
            String runConfigCsvHeader =
                    "MaxRuleSize" + sep
                            + "MinSup" + sep
                            + "NThreads" + sep
                            + "ShowRealSupport" + sep
                            + "ShowExplorationLayers" + sep
                            + "Verbose" + sep
                            + "ErrorRateThreshold" + sep
                            + "CompareToGroundTruth" + sep
                            + "RestrainedHead" + sep
                            + "PathToGroundTruthRules" + sep
                            + "CorrectionFactorClosure" + sep
                            + "CorrectionFactorOpening" + sep
                            + "SearchRuntime" + sep
                            + "MemoryPeak" + sep
                            + "SearchSpaceSizeEstimate" + sep
                            + "FixedSearchSpaceSizeEstimate"
                            + "\n";
            System.out.print(runConfigCsvHeader);
            outputConfigurationCsvWriter.write(runConfigCsvHeader);
            String runConfigCsvLine = "" +
                    MaxRuleSize + sep
                    + MinSup + sep
                    + NThreads + sep
                    + ShowRealSupport + sep
                    + ShowExplorationLayers + sep
                    + Verbose + sep
                    + ErrorRateThreshold + sep
                    + CompareToGroundTruth + sep
                    + (RestrainedHead == null ? "" : RestrainedHead) + sep
                    + (pathToGroundTruthRules == null ? "" : pathToGroundTruthRules) + sep
                    + CORRECTION_FACTOR_CLOSURE + sep
                    + CORRECTION_FACTOR_OPENING + sep
                    + duration + sep
                    + Runtime.getRuntime().totalMemory() / 1048576 + sep
                    + totalSumExploredRules + sep
                    + totalSumExploredRulesAdjustedWithBidirectionality + "\n";
            outputConfigurationCsvWriter.write(runConfigCsvLine);
            System.out.print(runConfigCsvLine);
            outputConfigurationCsvWriter.close();
        } catch (IOException e) {
            System.err.println("Couldn't create output file: " + outputComparisonCsvPath + ". Maybe file already exists.");
            e.printStackTrace();
        }

        System.out.println("Thank you for using mini-Amie. See you next time");

    }


    private static ExplorationResult InitExploreChildren(final Rule rule) {

        ArrayList<Rule> finalRules = new ArrayList<>();
        if (ShowExplorationLayers)
            System.err.println("INIT Exploring rule: " + rule);
        int searchSpaceEstimatedSize = 0;
        int searchSpaceEstimatedAdjustedWithBidirectionalitySize = 0;

        ArrayList<Pair<Rule, Integer>> closedChildren = AddClosureToEmptyBody(rule);

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE;

            for (Pair<Rule, Integer> closedChildCorrectionPair : closedChildren) {
                Rule closedChild = closedChildCorrectionPair.first;

                int correction = closedChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                long appSupp = ApproximateSupportClosedRule(closedChild);
                if (appSupp >= MinSup) {
                    finalRules.add(closedChild);
                }
            }
        }

        ArrayList<Pair<Rule, Integer>> openChildren = AddDanglingToEmptyBody(rule);
        if (openChildren != null) {
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENING;

            for (Pair<Rule, Integer> openChildCorrectionPair : openChildren) {
                Rule openChild = openChildCorrectionPair.first;

                int correction = openChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                long appSupp = ApproximateSupportOpenRule(openChild);
                if (appSupp >= MinSup) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    finalRules.addAll(exploreOpenChildResult.finalRules);
                    searchSpaceEstimatedSize += exploreOpenChildResult.sumExploredRules;
                    searchSpaceEstimatedAdjustedWithBidirectionalitySize +=
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality;
                }
            }
        }

        return new ExplorationResult(searchSpaceEstimatedSize,
                searchSpaceEstimatedAdjustedWithBidirectionalitySize, finalRules);
    }

    private static ExplorationResult ExploreChildren(Rule rule) {

        if (rule.getBody().size() + 2 > miniAMIE.MaxRuleSize)
            return new ExplorationResult(0, 0,
                    Collections.emptyList());

        ArrayList<Rule> finalRules = new ArrayList<>();
        if (ShowExplorationLayers)
            System.err.println("Exploring rule: " + rule);
        int searchSpaceEstimatedSize = 0;
        int searchSpaceEstimatedAdjustedWithBidirectionalitySize = 0;
        ArrayList<Pair<Rule, Integer>> closedChildren = AddClosure(rule);

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE;

            for (Pair<Rule, Integer> closedChildCorrectionPair : closedChildren) {
                Rule closedChild = closedChildCorrectionPair.first;
                int correction = closedChildCorrectionPair.second;
                searchSpaceEstimatedAdjustedWithBidirectionalitySize += correction;

                long appSupp = ApproximateSupportClosedRule(closedChild);
//            System.out.println("approximation: " + appSupp);
                if (appSupp >= MinSup) {
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

                long appSupp = ApproximateSupportOpenRule(openChild);
                if (appSupp >= MinSup) {
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
