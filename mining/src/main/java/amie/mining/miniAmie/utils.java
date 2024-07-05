package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.KB;
import amie.data.Schema;
import amie.data.tuple.IntPair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class utils {

    public static MiningAssistant miningAssistant;

    // TODO define this in KB module
    private static final String X_QUERY = "?x";
    private static final String Y_QUERY = "?y";
    private static final String Z_QUERY = "?z";

    private static final int SUBJECT_POSITION = 0;
    private static final int RELATION_POSITION = 1;
    private static final int OBJECT_POSITION = 2;

    private static final int SUBJECT_SUBJECT_OVERLAP = 0 ;
    private static final int SUBJECT_OBJECT_OVERLAP = 2 ;
    private static final int OBJECT_OBJECT_OVERLAP = 4 ;



    /**
     * ExplorationResult is instantiated to return the result of an exploration.
     * - sumExploredRules is the approximate number of rules to be explored in the subtree
     * - finalRules is the list of rules to be kept as a result of the mining process
     */
    public static class ExplorationResult {
        int sumExploredRules = 0;
        List<Rule> finalRules;

        public ExplorationResult(int sumExploredRules, List<Rule> finalRules) {
            this.sumExploredRules = sumExploredRules;
            this.finalRules = finalRules;
        }
    }

    /**
     * AddOperationResult is instantiated upon adding a new atom to a rule.
     * - nbPossibleRules is the total amount of adding operations that could be performed on a given rule.
     * - chosenRule is the result of the operation after selecting an atom and adding it to a given rule.
     */
    public static class AddOperationResult {
        int nbPossibleRules = 0;
        Rule chosenRule;

        public AddOperationResult(int nbPossibleRules, Rule chosenRule) {
            this.nbPossibleRules = nbPossibleRules;
            this.chosenRule = chosenRule;
        }
    }
    /**
     * AddClosure selects a predicate and arguments so that at each variable in the rule are present at least
     * twice.
     *
     * @param rule open rule to extend and close.
     * @return Result of selective closure and the total amount of adding operation that could be performed on the
     * given rule.
     */
    public static AddOperationResult AddClosure(Rule rule) {
        Rule chosenClosedRule = null;
        int closedRuleCount = 0;

        IntPair[] varSetups = new IntPair[2];
        varSetups[0] = new IntPair(0, 2);
        varSetups[1] = new IntPair(2, 0);
        int[] newEdge = rule.fullyUnboundTriplePattern();
        int relationVariable = newEdge[1];

        for (IntPair varSetup : varSetups) {
            int joinPosition = varSetup.first;
            int closeCirclePosition = varSetup.second;
            int joinVariable = newEdge[joinPosition];
            int closeCircleVariable = newEdge[closeCirclePosition];

            for (int sourceVariable : rule.getOpenVariables()) {
                newEdge[joinPosition] = sourceVariable;

                for (int variable : rule.getOpenableVariables()) {
                    if (variable != sourceVariable) {
                        newEdge[closeCirclePosition] = variable;

                        rule.getTriples().add(newEdge);
                        Int2IntMap promisingRelations =
                                miniAMIE.kb.frequentBindingsOf(newEdge[1], rule.getFunctionalVariable(),
                                        rule.getTriples());
                        rule.getTriples().remove(rule.getTriples().size());

                        for (int relation : promisingRelations.keySet()) {

                            //Here we still have to make a redundancy check
                            int cardinality = promisingRelations.get(relation);
                            newEdge[1] = relation;
                            if (cardinality >= miniAMIE.MinSup) {
                                Rule candidate = rule.addAtom(newEdge, cardinality);
                                if (!candidate.isRedundantRecursive()) {
                                    candidate.addParent(rule);
                                    if (chosenClosedRule == null)
                                        chosenClosedRule = candidate;
                                    closedRuleCount++;
                                }
                            }
                        }
                    }
                    newEdge[1] = relationVariable;
                }
                newEdge[closeCirclePosition] = closeCircleVariable;
                newEdge[joinPosition] = joinVariable;
            }
        }

        return new AddOperationResult(closedRuleCount, chosenClosedRule);
    }

    /**
     * AddObjectSubjectDanglingAtom selects a predicate and arguments so that one the object variable is already present
     * in the rule as a subject and the subject variable is unique.
     *
     * @param rule open rule to extend
     * @return Result of selective open atom and the total amount of adding operation that could be performed on the
     * given rule.
     */
    public static AddOperationResult AddObjectSubjectDanglingAtom(Rule rule) {
        Rule chosenOpenRule = null;
        int openRuleCount = 0;

        int[] lastBodyAtom = rule.getHead() ;
        int[] dangling = new int[3];

        IntList joinVariables = null;

        int nPatterns = rule.getTriples().size();
        // Dangling object (first body atom) is last body atom subject
        dangling[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
        for (int joinVariable : joinVariables) {

            if (dangling[SUBJECT_POSITION] == 0)
                dangling[SUBJECT_POSITION] = joinVariable; // Luis: next variable in map?

            List<int[]> chosenOpenRuleTriples = rule.getTriples();
            chosenOpenRuleTriples.add(dangling);
            Int2IntMap promisingRelations = miniAMIE.kb.frequentBindingsOf(dangling[1],
                    rule.getFunctionalVariable(), rule.getTriples());
            rule.getTriples().remove(nPatterns);

            boolean boundHead = !KB.isVariable(rule.getTriples().get(0)[SUBJECT_POSITION]);
            for (int relation : promisingRelations.keySet()) {
                //Here we still have to make a redundancy check
                int cardinality = promisingRelations.get(relation);
                if (cardinality >= miniAMIE.MinSup) {
                    dangling[RELATION_POSITION] = relation;
                    Rule candidate = rule.addAtom(dangling, cardinality);
                    if (candidate.containsUnifiablePatterns()) {
                        //Verify whether dangling variable unifies to a single value (I do not like this hack)
                        if (boundHead &&
                                miniAMIE.kb.countDistinct(dangling[SUBJECT_POSITION], candidate.getTriples()) < 2)
                            continue;
                    }

                    if (chosenOpenRule == null) {
                        chosenOpenRule = candidate;

                    }
                    openRuleCount++;
                }
            }
        }
        return new AddOperationResult(openRuleCount, chosenOpenRule);
    }

    /**
     * AddObjectObjectDanglingAtom selects a predicate and arguments so that one the object variable is already present
     * in the rule as an object and the subject variable is unique.
     *
     * @param rule open rule to extend
     * @return Result of selective open atom and the total amount of adding operation that could be performed on the
     * given rule.
     */
    public static AddOperationResult AddObjectObjectDanglingAtom(Rule rule) {
        Rule chosenOpenRule = null;
        int openRuleCount = 0;

        int[] head = rule.getHead() ;
        int[] dangling = new int[3];

        IntList joinVariables = null;

        int nPatterns = rule.getTriples().size();
        dangling[OBJECT_POSITION] = head[OBJECT_POSITION]; // Dangling object (first body atom) is head object
        for (int joinVariable : joinVariables) {

            if (dangling[SUBJECT_POSITION] == 0)
                dangling[SUBJECT_POSITION] = joinVariable; // Luis: next variable in map?

            List<int[]> chosenOpenRuleTriples = rule.getTriples();
            chosenOpenRuleTriples.add(dangling);
            Int2IntMap promisingRelations = miniAMIE.kb.frequentBindingsOf(dangling[1],
                    rule.getFunctionalVariable(), rule.getTriples());
            rule.getTriples().remove(nPatterns);

            boolean boundHead = !KB.isVariable(rule.getTriples().get(0)[SUBJECT_POSITION]);
            for (int relation : promisingRelations.keySet()) {
                //Here we still have to make a redundancy check
                int cardinality = promisingRelations.get(relation);
                if (cardinality >= miniAMIE.MinSup) {
                    dangling[RELATION_POSITION] = relation;
                    Rule candidate = rule.addAtom(dangling, cardinality);
                    if (candidate.containsUnifiablePatterns()) {
                        //Verify whether dangling variable unifies to a single value (I do not like this hack)
                        if (boundHead &&
                                miniAMIE.kb.countDistinct(dangling[SUBJECT_POSITION], candidate.getTriples()) < 2)
                            continue;
                    }

                    if (chosenOpenRule == null) {
                        chosenOpenRule = candidate;

                    }
                    openRuleCount++;
                }
            }
        }
        return new AddOperationResult(openRuleCount, chosenOpenRule);
    }

    private static double survivalRate(int r1, int r2, int type) {
        int r1Range = miniAMIE.kb.schema.getRelationRange(miniAMIE.kb, r1) ;
        if (r1Range > 0) {
            return (double) miniAMIE.kb.overlap(r1, r2, type) / r1Range ;
        }
        return 0 ;
    }

    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     * @param rule
     * @return total product operation iterating over the provided rule's body
     */
    private static double bodyEstimate(Rule rule) {
        double product = 1 ;
        List<int[]> body = rule.getBody() ;
        for (int id = 2 ; id < rule.getBodySize() + 1 ; id ++) {
            int r = body.get(id)[RELATION_POSITION] ;
            int r_next = body.get(id+1)[RELATION_POSITION] ;
            double survivalRate = survivalRate(r, r_next , SUBJECT_POSITION) ;
            product *= survivalRate / miniAMIE.kb.inverseFunctionality(r_next)  ;
        }
        return product ;
    }

    /**
     * Support approximation for a closed rule
     * @param rule Closed rule
     * @return Support approximation
     */
    public static double ApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS() ;
        List<int[]> body = rule.getBody() ;
        int rFirstBodyAtom = body.get(0)[RELATION_POSITION] ;
        int rLastBodyAtom = body.get(body.size()-1)[RELATION_POSITION] ;

        int headSize =  miniAMIE.kb.relationSize(rHead) ;
        int overlap = miniAMIE.kb.overlap(rHead, rFirstBodyAtom, OBJECT_OBJECT_OVERLAP) ;
        double survivalRate = survivalRate(rHead, rLastBodyAtom, SUBJECT_SUBJECT_OVERLAP) ;
        int domainHead = miniAMIE.kb.schema.getRelationDomain(miniAMIE.kb, rHead) ;
        int rangeHead = miniAMIE.kb.schema.getRelationRange(miniAMIE.kb, rHead) ;
        double bodyEstimate = bodyEstimate(rule);
        return headSize*headSize*overlap*survivalRate*bodyEstimate/domainHead*rangeHead ;
    }

    /**
     * Support approximation for an open rule
     * @param rule Open rule
     * @return Support approximation
     */
    public static double ApproximateSupportOpenRule(Rule rule) {
        int rHead = rule.getHeadRelationBS() ;
        int rFirstBodyAtom = rule.getBody().get(0)[RELATION_POSITION] ;

        int overlap = miniAMIE.kb.overlap(rHead, rFirstBodyAtom, OBJECT_OBJECT_OVERLAP) ;
        double bodyEstimate = bodyEstimate(rule);
        double fun = miniAMIE.kb.functionality(rHead) ;
        return overlap*bodyEstimate/fun ;
    }

    /**
     * GetInitRules provides a sample of rules with empty bodies and head coverage above provided minimum support.
     * @param minSup
     * @return Collection of single atom rules
     */
    public static Collection<Rule> GetInitRules(int minSup) {
        return miningAssistant.getInitialAtoms(minSup);
    }
}
