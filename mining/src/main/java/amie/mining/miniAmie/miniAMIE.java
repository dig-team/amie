package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
    public static boolean ShowRealSupport = false;
    public static boolean ShowExplorationLayers = false;
    public static boolean Verbose = false;
    public static double ErrorRateThreshold = 0.5;

    public static boolean CompareToGroundTruth = true;
    public static String RestrainedHead = "";
    public static String pathToGroundTruthRules = "mining/src/test/resources/yago2s-rules2";

    public static boolean printOutputToCsv = true;

    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENNING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>();

    public static void Run() {
        // Init mining assistant (temp solution)
        System.out.println("MaxRuleSize " + MaxRuleSize + " atoms.");
        System.out.println("MinSup " + MinSup + ".");

        miningAssistant = new DefaultMiningAssistant(kb);

        SelectedRelations = SelectRelations();

        Collection<Rule> initRules = GetInitRules(MinSup);
        int totalSumExploredRules = 0;
        int totalSumExploredRulesAdjustedWithBidirectionality = 0 ;
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
            totalSumExploredRulesAdjustedWithBidirectionality += 1 ;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");
        System.out.println("Search space approximation (adjusted with bidirectionality): " +
                totalSumExploredRulesAdjustedWithBidirectionality + " possibilities.");

        System.out.println("Bidirectional relations (range-dom jaccard >= "+ BidirectionalityJaccardThreshold+"):");
        for(int relation : bidirectionalityMap.keySet())
            if(bidirectionalityMap.get(relation))
                System.out.print(kb.unmap(relation)+" ");
        System.out.println("");

        System.out.println("Approximate mining: ");
        if (CompareToGroundTruth) {

            // Generating comparison map
            List<Rule> groundTruthRules = utils.LoadGroundTruthRules();
            HashMap<Rule, utils.RuleStateComparison> comparisonMap = new HashMap<>();
            for (Rule rule : finalRules)
                comparisonMap.put(rule, RuleStateComparison.FALSE);
            for (Rule groundTruthRule : groundTruthRules) {
                boolean found = false;
                for (Rule rule : finalRules) {
                    if (utils.CompareRules(rule, groundTruthRule)) {
                        found = true;
                        comparisonMap.put(rule, RuleStateComparison.CORRECT);
                        break;
                    }
                }
                if (!found) {
                    if (utils.ShouldHaveBeenFound(groundTruthRule))
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_FAILURE);
                    else
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_OK);
                }
            }

            // Displaying comparison map
            System.out.println(" Comparison to ground truth: ");
            String sep = ",";
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
//                            + "altAppSupport" + sep
                            + "realSupport"
                            + "\n"
            );
//            String csvText = csvColumnLine ;
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
                double real = RealSupport(rule);
                double app = -1;
//                double altApp = -1;
                if (utils.ShouldHaveBeenFound(rule)) {
                    app = ApproximateSupportClosedRule(rule);
//                    altApp = ApproximateSupportClosedRule2(rule);
                }

                String csvLine = String.format(
                        rule + sep + // RULE
                                kb.unmap(rule.getHead()[RELATION_POSITION]) + sep // HEAD RELATION
                                + (rule.getBody().size() + 1) + sep // RULE SIZE
                                + (compRule == RuleStateComparison.FALSE ? 1 : 0) + sep // FALSE
                                + (compRule == RuleStateComparison.CORRECT ? 1 : 0) + sep // CORRECT
                                + (compRule == RuleStateComparison.MISSING_FAILURE ? 1 : 0) + sep // MISSING_FAILURE
                                + (compRule == RuleStateComparison.MISSING_OK ? 1 : 0) + sep // MISSING_OK
                                + (utils.IsRealPerfectPath(rule) ? 1 : 0) + sep
                                + (utils.HasNoRedundancies(rule) ? 0 : 1) + sep
                                + app + sep // APP SUPPORT
//                                + altApp + sep // ALT APP
                                + real
                                + "\n"
                );
                // Printing comparison to console
                System.out.print(comparisonCharacter + csvLine + ANSI_RESET);
//                csvText += csvLine ;
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
                Rule closedChild = closedChildCorrectionPair.first ;

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
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENNING;

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
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality ;
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
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENNING;

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
                            exploreOpenChildResult.sumExploredRulesAdjustedWithBidirectionality ;
                }
            }

        }

        return new ExplorationResult(searchSpaceEstimatedSize, searchSpaceEstimatedAdjustedWithBidirectionalitySize,
                finalRules);
    }
}
