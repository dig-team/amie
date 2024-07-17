package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;
import org.eclipse.rdf4j.query.algebra.Min;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENNING = 4;

    public static void Run() {
        // Init mining assistant (temp solution)
        miningAssistant = new DefaultMiningAssistant(kb);

        Collection<Rule> initRules = GetInitRules(MinSup);
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();
        for (Rule rule : initRules) {
            double supp = rule.getSupport();
            if (supp >= MinSup) {

                ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                finalRules.addAll(exploreChildrenResult.finalRules);
            }
            totalSumExploredRules += 1;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");
        System.out.println("Approximate mining: ");
        for (Rule rule : finalRules) {
            System.out.println(rule.toString());
        }
        System.out.println("Thank you for using mini-Amie. See you next time");
    }

    private static ExplorationResult InitExploreChildren(final Rule rule) {

        System.err.println("INIT Exploring rule: " + rule.toString());
        int totalSumExploredRules;
        List<Rule> finalRules = new ArrayList<>();
        AddOperationResult addClosureOperationResult = AddClosure_EmptyBody(rule);
        int nbPossibleClosurePredicate = 0;

        // Note: Correcting factor due to 2 possible permutations for closing atom object/subject
        if (addClosureOperationResult != null) {
            Rule closedChild = addClosureOperationResult.chosenRule;
            nbPossibleClosurePredicate = addClosureOperationResult.nbPossibleRules;
            System.out.println("Closure: " + closedChild
                    + " nbPossibleClosurePredicate: " + nbPossibleClosurePredicate);
            double appSupp = ApproximateSupportClosedRule(closedChild) ;
//            System.out.println("approximation: " + appSupp);
            if (appSupp < MinSup) {
                finalRules.add(closedChild);
            }
        }
        totalSumExploredRules = nbPossibleClosurePredicate * CORRECTION_FACTOR_CLOSURE;

        AddOperationResult addOpenOperationResult = AddObjectObjectDanglingAtom(rule);


        if (addOpenOperationResult != null) {
            Rule openChild = addOpenOperationResult.chosenRule;
            int nbPossibleOpenPredicate = addOpenOperationResult.nbPossibleRules;
            System.out.println("Opening: " + openChild
                    + " nbPossibleClosurePredicate: " + nbPossibleOpenPredicate);

            double appSupp = ApproximateSupportOpenRule(openChild) ;
            if (appSupp > MinSup) {
            ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
        finalRules.addAll(exploreOpenChildResult.finalRules);
        totalSumExploredRules += nbPossibleOpenPredicate * exploreOpenChildResult.sumExploredRules
                * CORRECTION_FACTOR_OPENNING;
                totalSumExploredRules += nbPossibleOpenPredicate * CORRECTION_FACTOR_OPENNING;
// Note: Correcting factor due to 4 possible permutations for closing atom object/subject
            } else {
                System.out.println("Reached insufficient support : "+appSupp+ " < " + MinSup);
            }
        }
        return new ExplorationResult(totalSumExploredRules, finalRules);
    }

    private static ExplorationResult ExploreChildren(Rule rule) {
        System.err.println("Exploring rule: " + rule.toString());
        if (rule.getBodySize() + 1 > MaxRuleSize) {
            return new ExplorationResult(0, new ArrayList<Rule>());
        }
        int totalSumExploredRules;
        List<Rule> finalRules = new ArrayList<>();
        AddOperationResult addClosureOperationResult = AddClosure(rule);
        int nbPossibleClosurePredicate = 0;

        // Note: Correcting factor due to 2 possible permutations for closing atom object/subject
        if (addClosureOperationResult != null) {
            Rule closedChild = addClosureOperationResult.chosenRule;
            nbPossibleClosurePredicate = addClosureOperationResult.nbPossibleRules;
            System.out.println("Closure: " + closedChild
                    + " nbPossibleClosurePredicate: " + nbPossibleClosurePredicate);
            double appSupp = ApproximateSupportClosedRule(closedChild) ;
//            System.out.println("approximation: " + appSupp);
            if (appSupp < MinSup) {
                finalRules.add(closedChild);
            }
        }
        totalSumExploredRules = nbPossibleClosurePredicate * CORRECTION_FACTOR_CLOSURE;

        if (rule.getBodySize() + 2 > MaxRuleSize) {
            return new ExplorationResult(totalSumExploredRules, finalRules);
        }

        AddOperationResult addOpenOperationResult = AddObjectSubjectDanglingAtom(rule);


        if (addOpenOperationResult != null) {
            Rule openChild = addOpenOperationResult.chosenRule;
            int nbPossibleOpenPredicate = addOpenOperationResult.nbPossibleRules;
            System.out.println("Opening: " + openChild
                    + " nbPossibleClosurePredicate: " + nbPossibleOpenPredicate);

            double appSupp = ApproximateSupportOpenRule(openChild) ;
            if (appSupp > MinSup) {
            ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
        finalRules.addAll(exploreOpenChildResult.finalRules);
        totalSumExploredRules += nbPossibleOpenPredicate * exploreOpenChildResult.sumExploredRules
                * CORRECTION_FACTOR_OPENNING;
                totalSumExploredRules += nbPossibleOpenPredicate * CORRECTION_FACTOR_OPENNING;
// Note: Correcting factor due to 4 possible permutations for closing atom object/subject
            }
        }
        // Note: Correcting factor due to 4 possible permutations for closing atom object/subject
        return new ExplorationResult(totalSumExploredRules, finalRules);
    }
}