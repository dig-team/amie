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
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleUnaryOperator;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
    public static boolean ShowRealSupport = false;
    public static boolean ShowExplorationLayers = false;
    public static boolean Verbose = false;
    public static double ErrorRateThreshold = 0.5;

    public static boolean CompareToGroundTruth = false;
    public static String RestrainedHead = "";
    public static String pathToGroundTruthRules = "";
    public static String outputComparisonCsvPath = "./comparison"+
            Instant.now().toString().replace(" ","_")+".csv";

    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>();

    public static void Run() {
        long startTime = System.currentTimeMillis();
        List<Rule> groundTruthRules = new ArrayList<>();
        if (CompareToGroundTruth) {
            // Generating comparison map
            groundTruthRules = LoadGroundTruthRules();
        }

        // Init mining assistant (temp solution)
        System.out.println("MaxRuleSize " + MaxRuleSize + " atoms.");
        System.out.println("MinSup " + MinSup + ".");
        System.out.println("ShowRealSupport " + ShowRealSupport + ".");
        System.out.println("ShowExplorationLayers " + ShowExplorationLayers + ".");
        System.out.println("Verbose " + Verbose + ".");
        System.out.println("ErrorRateThreshold " + ErrorRateThreshold + ".");
        System.out.println("CompareToGroundTruth " + CompareToGroundTruth + ".");
        System.out.println("RestrainedHead " + (RestrainedHead.isEmpty() ? "None" : RestrainedHead) + ".");
        System.out.println("pathToGroundTruthRules " + pathToGroundTruthRules + ".");
        System.out.println("Correction factor closure " + CORRECTION_FACTOR_CLOSURE + ".");
        System.out.println("Correction factor opening " + CORRECTION_FACTOR_OPENING + ".");


        miningAssistant = new DefaultMiningAssistant(kb);

        SelectedRelations = SelectRelations();

        Collection<Rule> initRules = GetInitRules(MinSup);
        int totalSumExploredRules = 0;
        int totalSumExploredRulesAdjustedWithBidirectionality = 0;
        List<Rule> finalRules = new ArrayList<>();

        for (Rule rule : initRules) {
            if (rule.toString().contains(RestrainedHead)) {
                ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                totalSumExploredRulesAdjustedWithBidirectionality +=
                        exploreChildrenResult.sumExploredRulesAdjustedWithBidirectionality;
                finalRules.addAll(exploreChildrenResult.finalRules);
            }
            totalSumExploredRules += 1;
            totalSumExploredRulesAdjustedWithBidirectionality += 1;
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
//            List<Rule> groundTruthRules = utils.LoadGroundTruthRules();
            HashMap<Rule, RuleStateComparison> comparisonMap = new HashMap<>();
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
                    System.out.println("Created CSV output: " +outputComparisonCsvPath);
                }

                FileWriter writer = new FileWriter(outputComparisonCsvPath);

                String csvColumnLine = String.format(
                        "rule" + sep // RULE
//                                + "example" + sep
                                + "headRelation" + sep
                                + "size" + sep
                                + "isFalse" + sep // FALSE
                                + "isCorrect" + sep // CORRECT
                                + "isMissingFailure" + sep // MISSING_FAILURE
                                + "isMissingOK" + sep // MISSING_OK
                                + "isPerfectPath" + sep
                                + "hasRedundancies" + sep
                                + "appSupport" + sep // APP SUPPORT
//                            + "altAppSupport" + sep
                                + "realSupport"+ sep
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
                writer.write(csvColumnLine) ;
                System.out.print(csvColumnLine);

                for (Rule rule : comparisonMap.keySet()) {
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
                    int NanoToMillisFactor = 1000000;
                    long startReal = System.nanoTime();
                    double real = RealSupport(rule);
                    long realNano = System.nanoTime() - startReal;
                    double app = -1;
//                double altApp = -1;
                    long appNano = -1;
                    FactorsOfApproximateSupportClosedRule factors = new FactorsOfApproximateSupportClosedRule();
                    if (comparisonMap.get(rule) == RuleStateComparison.CORRECT || ShouldHaveBeenFound(rule)) {
                        long startApp = System.nanoTime();
                        app = ApproximateSupportClosedRule(rule);
                        appNano = System.nanoTime() - startApp;
//                    altApp = ApproximateSupportClosedRule2(rule);
                        factors = GetFactorsOfApproximateSupportClosedRule(rule) ;
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
//                                + altApp + sep // ALT APP
                                    + real + sep
                                    + appNano + sep
                                    + realNano + sep
                                    + factors
                                    + "\n"
                    );
                    writer.write(csvLine);
                    // Printing comparison to console
                    System.out.print(comparisonCharacter + csvLine + ANSI_RESET);
//                csvText += csvLine ;
                }

            } catch (IOException e) {
                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
                e.printStackTrace();
            }




//            System.out.println(" --- ---- CSV BELLOW (copy/paste result to file)") ;
//            System.out.println(csvText) ;
//            System.out.println(" --- ---- ") ;

        }
//        for (Rule rule : finalRules) {
//            if (ShowRealSupport) {
//                double real = RealSupport(rule) ;
//                double app = ApproximateSupportClosedRule(rule) ;
//                double errorRate = utils.ErrorRate(real, app);
//                double errorContrastRatio = ErrorContrastRatio(real, app);
//                double errorLog = ErrorRateLog(real, app);
//                if (errorRate >= ErrorRateThreshold) {
//                    System.out.print(ANSI_YELLOW) ;
//                } else if (errorRate < -ErrorRateThreshold) {
//                    System.out.print(ANSI_CYAN) ;
//                } else {
//                    System.out.print(ANSI_WHITE) ;
//                }
//                utils.printRuleAsPerfectPath(rule) ;
//                System.out.println(" : s~ " + app +
//                        " | s " + real +
//                        " | err (rate, contrast, log) " + errorRate + " " + errorContrastRatio + " " + errorLog + ANSI_RESET);
//            }
//            else
//                System.out.println(rule.toString() + " : s~ " + ApproximateSupportClosedRule(rule) );
//        }

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
//            System.out.println("approximation: " + appSupp);
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
