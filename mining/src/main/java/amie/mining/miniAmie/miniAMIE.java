package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
    public static boolean ShowRealSupport = false ;
    public static boolean ShowExplorationLayers = false ;
    public static boolean Verbose = false ;
    public static double ErrorRateThreshold = 0.5 ;

    public static boolean CompareToGroundTruth = true ;
    public static String RestrainedHead = "" ;
    public static String pathToGroundTruthRules = "mining/src/test/resources/yago2s-rules" ;


    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENNING = 4;

    protected static List<Integer> SelectedRelations = new ArrayList<>() ;

    public static void Run() {
        // Init mining assistant (temp solution)
        System.out.println("MaxRuleSize " + MaxRuleSize + " atoms.");
        System.out.println("MinSup " + MinSup + ".");

        miningAssistant = new DefaultMiningAssistant(kb);

        SelectedRelations = SelectRelations() ;

        Collection<Rule> initRules = GetInitRules(MinSup);
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();

        for (Rule rule : initRules) {
                if (rule.toString().contains(RestrainedHead)) {
                    ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                    totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                    finalRules.addAll(exploreChildrenResult.finalRules);
                }
            totalSumExploredRules += 1;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");

        System.out.println("Approximate mining: ");
        if (CompareToGroundTruth) {

            // Generating comparison map
            List<Rule> groundTruthRules = utils.LoadGroundTruthRules() ;
            HashMap<Rule, utils.RuleStateComparison> comparisonMap = new HashMap<>();
            for(Rule rule : finalRules)
                comparisonMap.put(rule, RuleStateComparison.FALSE) ;
            for(Rule groundTruthRule : groundTruthRules) {
                boolean found = false;
                for(Rule rule : finalRules) {
                    if (utils.CompareRules(rule, groundTruthRule)) {
                        found = true;
                        comparisonMap.put(rule, RuleStateComparison.CORRECT) ;
                        break;
                    }
                }
                if (!found) {
                    if(utils.ShouldHaveBeenFound(groundTruthRule))
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_FAILURE) ;
                    else
                        comparisonMap.put(groundTruthRule, RuleStateComparison.MISSING_OK) ;
                }
            }

            // Displaying comparison map
            System.out.println(" Comparison to ground truth: ") ;
            for(Rule rule: comparisonMap.keySet()) {
                String comparisonCharacter ;
                if(comparisonMap.get(rule) == RuleStateComparison.FALSE) {
                    comparisonCharacter = ANSI_YELLOW+"F";
                } else if (comparisonMap.get(rule) == RuleStateComparison.CORRECT) {
                    comparisonCharacter = ANSI_GREEN+"C";
                } else if (comparisonMap.get(rule) == RuleStateComparison.MISSING_FAILURE){
                    comparisonCharacter = ANSI_RED+"M FA";
                } else if (comparisonMap.get(rule) == RuleStateComparison.MISSING_OK){
                    comparisonCharacter = ANSI_PURPLE+"M OK";

                } else {
                    throw new RuntimeException("Unknown comparison rule " + rule);
                }
                System.out.println(comparisonCharacter+  " " + rule + ANSI_RESET);
            }
        }
        for (Rule rule : finalRules) {
            if (ShowRealSupport) {
                double real = RealSupport(rule) ;
                double app = ApproximateSupportClosedRule(rule) ;
                double errorRate = utils.ErrorRate(real, app);
                double errorContrastRatio = ErrorContrastRatio(real, app);
                double errorLog = ErrorRateLog(real, app);
                if (errorRate >= ErrorRateThreshold) {
                    System.out.print(ANSI_YELLOW) ;
                } else if (errorRate < -ErrorRateThreshold) {
                    System.out.print(ANSI_CYAN) ;
                } else {
                    System.out.print(ANSI_WHITE) ;
                }
                utils.printRuleAsPerfectPath(rule) ;
                System.out.println(" : s~ " + app +
                        " | s " + real +
                        " | err (rate, contrast, log) " + errorRate + " " + errorContrastRatio + " " + errorLog + ANSI_RESET);
            }
            else
                System.out.println(rule.toString() + " : s~ " + ApproximateSupportClosedRule(rule) );
        }

        System.out.println("Thank you for using mini-Amie. See you next time");
    }


    private static ExplorationResult InitExploreChildren(final Rule rule) {

        ArrayList<Rule> finalRules = new ArrayList<>() ;
        if (ShowExplorationLayers)
            System.err.println("INIT Exploring rule: " + rule);
        int searchSpaceEstimatedSize = 0;
        ArrayList<Rule> closedChildren = AddClosureToEmptyBody(rule) ;

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE ;

            for (Rule closedChild : closedChildren) {
                long appSupp = ApproximateSupportClosedRule(closedChild);
//            System.out.println("approximation: " + appSupp);
                if (appSupp >= MinSup) {
                    finalRules.add(closedChild);
                }
            }
        }

        ArrayList<Rule> openChildren = AddDanglingToEmptyBody(rule);
        if (openChildren != null) {
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENNING ;

            for (Rule openChild : openChildren) {
                long appSupp = ApproximateSupportOpenRule(openChild);
                if (appSupp >= MinSup) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    finalRules.addAll(exploreOpenChildResult.finalRules);
                    searchSpaceEstimatedSize += exploreOpenChildResult.sumExploredRules;
                }
            }
        }

        return new ExplorationResult(searchSpaceEstimatedSize, finalRules);
    }

    private static ExplorationResult ExploreChildren(Rule rule) {

        if (rule.getBody().size() + 2 > miniAMIE.MaxRuleSize)
            return new ExplorationResult(0, Collections.emptyList());

        ArrayList<Rule> finalRules = new ArrayList<>() ;
        if (ShowExplorationLayers)
            System.err.println("Exploring rule: " + rule);
        int searchSpaceEstimatedSize = 0;
        ArrayList<Rule> closedChildren = AddClosure(rule) ;

        if (closedChildren != null) {
            searchSpaceEstimatedSize += closedChildren.size() * CORRECTION_FACTOR_CLOSURE ;

            for (Rule closedChild : closedChildren) {
                long appSupp = ApproximateSupportClosedRule(closedChild);
//            System.out.println("approximation: " + appSupp);
                if (appSupp >= MinSup) {
                    finalRules.add(closedChild);
                }
            }
        }

        if (rule.getBody().size() + 3 > miniAMIE.MaxRuleSize)
            return new ExplorationResult(searchSpaceEstimatedSize, finalRules);

        ArrayList<Rule> openChildren = AddDangling(rule);

        if (openChildren != null) {
            searchSpaceEstimatedSize += openChildren.size() * CORRECTION_FACTOR_OPENNING ;

            for (Rule openChild : openChildren) {
                long appSupp = ApproximateSupportOpenRule(openChild);
                if (appSupp >= MinSup) {
                    ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
                    finalRules.addAll(exploreOpenChildResult.finalRules);
                    searchSpaceEstimatedSize += exploreOpenChildResult.sumExploredRules;
                }
            }
        }

        return new ExplorationResult(searchSpaceEstimatedSize, finalRules);
    }
}
