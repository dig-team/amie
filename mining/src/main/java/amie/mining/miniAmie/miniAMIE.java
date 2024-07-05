package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import amie.mining.miniAmie.utils;

import static amie.mining.miniAmie.utils.*;


public class miniAMIE {
    public static AbstractKB kb;

    public static int MaxRuleSize;
    public static int MinSup;
    private static final int CORRECTION_FACTOR_CLOSURE = 2;
    private static final int CORRECTION_FACTOR_OPENNING = 4;

    public static void Run() {
        // Init mining assistant (temp solution)
        miningAssistant = new MiningAssistant(kb) ;

        Collection<Rule> initRules = GetInitRules(MinSup);
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();
        for (Rule rule : initRules) {
            double supp = rule.getSupport();
            if (supp >= MinSup) {
                ExplorationResult exploreChildrenResult = InitExploreChildren(rule);
                totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                finalRules.addAll(exploreChildrenResult.finalRules);
            }
            totalSumExploredRules += 1;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " explored rules.");
        System.out.println("Approximate mining: ");
        for (Rule rule : finalRules) {
            System.out.println(rule.toString());
        }
        System.out.println("Thank you for using mini-Amie. See you next time");
    }

    private static ExplorationResult InitExploreChildren(Rule rule) {
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();
        AddOperationResult addClosureOperationResult = AddClosure(rule);

        Rule closedChild = addClosureOperationResult.chosenRule;
        int nbPossibleClosurePredicate = addClosureOperationResult.nbPossibleRules;

        if (ApproximateSupportClosedRule(closedChild) < MinSup) {
            totalSumExploredRules = nbPossibleClosurePredicate * CORRECTION_FACTOR_CLOSURE;
            // Note: Correcting factor due to 2 possible permutations for closing atom object/subject
            finalRules.add(closedChild);
        }

        AddOperationResult addOpenOperationResult = AddObjectSubjectDanglingAtom(rule);

        Rule openChild = addOpenOperationResult.chosenRule;
        int nbPossibleOpenPredicate = addOpenOperationResult.nbPossibleRules;

        if (ApproximateSupportOpenRule(openChild) < MinSup) {
            return new ExplorationResult(totalSumExploredRules, finalRules);
        }

        ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
        finalRules.addAll(exploreOpenChildResult.finalRules);
        totalSumExploredRules += nbPossibleOpenPredicate * exploreOpenChildResult.sumExploredRules
                * CORRECTION_FACTOR_OPENNING;
        // Note: Correcting factor due to 4 possible permutations for closing atom object/subject
        return new ExplorationResult(totalSumExploredRules, finalRules);
    }

    private static ExplorationResult ExploreChildren(Rule rule) {
        if (rule.getBodySize() + 1 > MaxRuleSize) {
            return new ExplorationResult(0, new ArrayList<Rule>());
        }
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();
        AddOperationResult addClosureOperationResult = AddClosure(rule);

        Rule closedChild = addClosureOperationResult.chosenRule;
        int nbPossibleClosurePredicate = addClosureOperationResult.nbPossibleRules;

        if (ApproximateSupportClosedRule(closedChild) < MinSup) {
            totalSumExploredRules = nbPossibleClosurePredicate * CORRECTION_FACTOR_CLOSURE;
            // Note: Correcting factor due to 2 possible permutations for closing atom object/subject
            finalRules.add(closedChild);
        }

        if (rule.getBodySize() + 2 > MaxRuleSize) {
            return new ExplorationResult(totalSumExploredRules, finalRules);
        }

        AddOperationResult addOpenOperationResult = AddObjectObjectDanglingAtom(rule);

        Rule openChild = addOpenOperationResult.chosenRule;
        int nbPossibleOpenPredicate = addOpenOperationResult.nbPossibleRules;

        if (ApproximateSupportOpenRule(openChild) < MinSup) {
            return new ExplorationResult(totalSumExploredRules, finalRules);
        }

        ExplorationResult exploreOpenChildResult = ExploreChildren(openChild);
        finalRules.addAll(exploreOpenChildResult.finalRules);
        totalSumExploredRules += nbPossibleOpenPredicate * exploreOpenChildResult.sumExploredRules
                * CORRECTION_FACTOR_OPENNING;
        // Note: Correcting factor due to 4 possible permutations for closing atom object/subject
        return new ExplorationResult(totalSumExploredRules, finalRules);
    }
}
