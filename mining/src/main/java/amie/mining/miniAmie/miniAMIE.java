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
    public static boolean ShowRealSupport = true ;
    public static boolean ShowExplorationLayers = false ;
    public static boolean Verbose = false ;
    public static double ErrorRateThreshold = 0.5 ;


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
//                if (rule.toString().contains("isCitizenOf")) {
                    ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                    totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                    finalRules.addAll(exploreChildrenResult.finalRules);
//                }
            totalSumExploredRules += 1;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");


        System.out.println("Approximate mining: ");
        for (Rule rule : finalRules) {
            if (ShowRealSupport) {
                double real = RealSupport(rule) ;
                double app = ApproximateSupportClosedRule(rule) ;
                double err = utils.ErrorRate(real, app);
                if (err >= ErrorRateThreshold) {
                    System.out.print(ANSI_YELLOW) ;
                } else if (err < -ErrorRateThreshold) {
                    System.out.print(ANSI_CYAN) ;
                } else {
                    System.out.print(ANSI_WHITE) ;
                }
                utils.printRuleAsPerfectPath(rule) ;
                System.out.println(" : s~ " + app +
                        " | s " + real +
                        " | err " + err + ANSI_RESET);
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
