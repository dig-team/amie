package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.tuple.IntPair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;
import javafx.beans.binding.ObjectExpression;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class utils {

    public static MiningAssistant miningAssistant;

    // TODO define this in KB module
    public static final int ATOM_SIZE = 3;

    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1 ;

    /**
     * ExplorationResult is instantiated to return the result of an exploration.
     * - sumExploredRules is the approximate number of rules to be explored in the subtree
     * - finalRules is the list of rules to be kept as a result of the mining process
     */
    public static class ExplorationResult {
        int sumExploredRules;
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
        int nbPossibleRules;
        Rule chosenRule;

        public AddOperationResult(int nbPossibleRules, Rule chosenRule) {
            this.nbPossibleRules = nbPossibleRules;
            this.chosenRule = chosenRule;
        }
    }

    /**
     * AddOperationResult is instantiated when seeking a new relation for a rule.
     * - nbPossibleRelations is the total amount of relations that could be used to create new atoms
     * - chosenRelation is the result of the operation after selecting a relation
     */
    public static class FindRelationResult {
        int nbPossibleRelations;
        int chosenRelation;

        public FindRelationResult(int nbPossibleRelations, int chosenRelation) {
            this.nbPossibleRelations = nbPossibleRelations;
            this.chosenRelation = chosenRelation;
        }
    }

    /**
     * ChooseRelation will select the best (highest support) available relation out of a set of promising relations.
     * @param promisingRelations
     * @return Selected relation and number of promising relations
     */
    private static FindRelationResult ChooseRelation(Int2IntMap promisingRelations) {
        if (promisingRelations.isEmpty()) {
//            System.out.println("[ChooseClosureRelation] no relations found ! \n");
            return null;
        }
        IntIterator iterator = promisingRelations.keySet().iterator();

        int nbPossibilities = promisingRelations.size() - 1 ; // Removing head duplicate
        int chosenRelation = 0;
        int maxSupport = 0 ;
        while (iterator.hasNext()) {
            int relation = iterator.nextInt() ;
            int support = promisingRelations.get(relation);
            if (support > maxSupport) {
                chosenRelation = relation ;
                maxSupport = support ;
            }
        }

        return new FindRelationResult(nbPossibilities, chosenRelation) ;
    }

    private static FindRelationResult ChooseRelationHeadExcluded(Int2IntMap promisingRelations, int headRelation) {
        if (promisingRelations.isEmpty()) {
//            System.out.println("[ChooseClosureRelation] no relations found ! \n");
            return null;
        }
        IntIterator iterator = promisingRelations.keySet().iterator();

        int nbPossibilities = promisingRelations.size() - 1 ; // Removing head duplicate
        int chosenRelation = 0;
        int maxSupport = 0 ;
        while (iterator.hasNext()) {
            int relation = iterator.nextInt() ;
            int support = promisingRelations.get(relation);
            if (support > maxSupport && relation != headRelation) {
                chosenRelation = relation ;
                maxSupport = support ;
            }
        }

        if (chosenRelation == 0)
            return null;

        return new FindRelationResult(nbPossibilities, chosenRelation) ;
    }

    /**
     * ChooseClosureRelation will try to find a relation to closes an open rule.
     * @param rule
     * @param openVariable
     * @param closureVariable
     * @return
     */
    private static FindRelationResult ChooseClosureRelation(final Rule rule, int openVariable, int closureVariable) {
        int[] unboundPattern = rule.fullyUnboundTriplePattern();
        unboundPattern[OBJECT_POSITION] = openVariable;
        unboundPattern[SUBJECT_POSITION] = closureVariable;
        int projectionVariable = unboundPattern[RELATION_POSITION];

        Rule child = new Rule(rule, rule.getSupport(), miniAMIE.kb) ;

        child.getBody().add(unboundPattern);
        int[] childHead = child.getHead();
        List<int[]> childBody = child.getBody() ;

        Int2IntMap promisingRelations = miniAMIE.kb.countProjectionBindings
                (childHead, childBody, projectionVariable);

        FindRelationResult result ;
        if (rule.getBody().isEmpty())
            result = ChooseRelationHeadExcluded(promisingRelations, childHead[RELATION_POSITION]);
        else
            result = ChooseRelation(promisingRelations) ;

        return result;

    }

    public static AddOperationResult AddClosure_EmptyBody(final Rule rule) {
        Rule chosenClosedRule;
        int closedRuleCount;
        int[] headAtom = rule.getHead();
        int openVariable;
        openVariable = headAtom[OBJECT_POSITION];
        FindRelationResult findRelationResult = ChooseClosureRelation
                (rule, openVariable, headAtom[SUBJECT_POSITION]);

        if (findRelationResult == null) {
            return null;
        }

        closedRuleCount = findRelationResult.nbPossibleRelations;
        int relation = findRelationResult.chosenRelation;
        chosenClosedRule = new Rule(rule, rule.getSupport(), miniAMIE.kb) ;
        int[] newAtom = headAtom.clone();
        newAtom[RELATION_POSITION] = relation;
        chosenClosedRule.getBody().add(newAtom);
        return new AddOperationResult(closedRuleCount, chosenClosedRule);

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
        Rule chosenClosedRule = null ;
        int closedRuleCount ;
        int[] headAtom = rule.getHead();
        int openVariable;

        // openVariables should be only of length 1
        IntList openVariables = rule.getOpenVariables();
        openVariable = openVariables.iterator().next();

        FindRelationResult findRelationResult = ChooseClosureRelation(rule, openVariable, headAtom[SUBJECT_POSITION]);

        if (findRelationResult == null) {
            return null;
        }
        closedRuleCount = findRelationResult.nbPossibleRelations;
        int relation = findRelationResult.chosenRelation;

        int[] newAtom = new int[ATOM_SIZE];
        newAtom[1] = relation;
        chosenClosedRule.getBody().add(newAtom);

        // TODO Subject or object ?
        newAtom[OBJECT_POSITION] = openVariable;
        newAtom[SUBJECT_POSITION] = headAtom[SUBJECT_POSITION];

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

        int[] lastBodyAtom = rule.getLastTriplePattern();
        int[] dangling = rule.fullyUnboundTriplePattern();

        // Dangling object (first body atom) is last body atom subject
        dangling[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
        Rule child = new Rule(rule, rule.getSupport(), miniAMIE.kb);
        List<int[]> chosenOpenRuleTriples = child.getTriples();
        chosenOpenRuleTriples.add(dangling);

        int[] childHead = child.getHead();
        List<int[]> childBody = child.getBody();
        int projectionVariable = dangling[RELATION_POSITION];
        Int2IntMap promisingRelations = miniAMIE.kb.countProjectionBindings
                (childHead, childBody, projectionVariable);
        FindRelationResult chooseRelationResult = ChooseRelation(promisingRelations);
        if (chooseRelationResult == null) {
            return null;
        }
        dangling[RELATION_POSITION] = chooseRelationResult.chosenRelation;
        child.getBody().add(dangling) ;
        return new AddOperationResult(promisingRelations.size(), child);
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
        int[] lastBodyAtom = rule.getHead();
        int[] dangling = rule.fullyUnboundTriplePattern();
        // Dangling object (first body atom) is last body atom subject
        dangling[OBJECT_POSITION] = lastBodyAtom[OBJECT_POSITION];
        Rule child = new Rule(rule, rule.getSupport(), miniAMIE.kb);
        List<int[]> chosenOpenRuleTriples = child.getTriples();
        chosenOpenRuleTriples.add(dangling);

        int[] childHead = child.getHead();
        List<int[]> childBody = child.getBody();
        int projectionVariable = dangling[RELATION_POSITION];
        Int2IntMap promisingRelations = miniAMIE.kb.countProjectionBindings
                (childHead, childBody, projectionVariable);
//        String childBodyStr = "";
//        for (int[] atom: childBody) {
//            childBodyStr += Arrays.toString(atom) + ", " ;
//        }
//        childBodyStr = "{"+childBodyStr+"}";
//        System.out.printf("[AddObjectObjectDanglingAtom] promisingRelations: %s <- childHead: %s, childBody: %s, projectionVariable: %d\n",
//                promisingRelations, Arrays.toString(childHead), childBodyStr, projectionVariable);
//        IntIterator iterator = promisingRelations.keySet().iterator();
//        int first = iterator.nextInt();
        FindRelationResult chooseRelationResult = ChooseRelation(promisingRelations);
        if (chooseRelationResult == null) {
            return null;
        }
        dangling[RELATION_POSITION] = chooseRelationResult.chosenRelation;
//        int nbPossibilities = promisingRelations.size() - 1 ; // Removing head duplicate

        child.getBody().add(dangling) ;
        return new AddOperationResult(chooseRelationResult.nbPossibleRelations, child);
    }

    private static int range(int r) {
        KB kb = (KB) miniAMIE.kb ;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int domain(int r) {
        KB kb = (KB) miniAMIE.kb ;
        Int2ObjectMap<IntSet> factSet = kb.relation2subject2object.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int overlap(int r1, int r2,
                               Int2ObjectMap<Int2IntMap> overlapTable, Int2ObjectMap<Int2ObjectMap<IntSet>> triples) {
        Int2IntMap factSet = overlapTable.get(r1);
        int overlap ;
        if (factSet == null) {
            IntSet r1_set = triples.get(r1).keySet();
            IntSet r2_set = triples.get(r2).keySet();
            overlap = KB.computeOverlap(r1_set, r2_set) ;

//            System.out.println("Adding " + r1 + " " + r2 + " overlap "+overlap);
            Int2IntMap overlaps1 = new Int2IntOpenHashMap();
            overlaps1.put(r2, overlap) ;
            overlapTable.put(r1, overlaps1);

            Int2IntMap overlaps2 = new Int2IntOpenHashMap();
            overlaps2.put(r1, overlap) ;
            overlapTable.put(r2, overlaps2);

        } else {
            overlap = factSet.getOrDefault(r1, NO_OVERLAP_VALUE);
            if (overlap == NO_OVERLAP_VALUE) {
                IntSet r1_set = triples.get(r1).keySet();
                IntSet r2_set = triples.get(r2).keySet();
                overlap = KB.computeOverlap(r1_set, r2_set) ;

//                System.out.println("Adding " + r1 + " " + r2 + " overlap "+ overlap);
                factSet.put(r1, overlap) ;
                factSet.put(r2, overlap) ;
            } else {
//                System.out.println("Found " + r1 + " " + r2 + " overlap "+ overlap);
            }
        }

        return overlap;
    }

    private static int objectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb ;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triples = kb.relation2object2subject;

        System.out.println("Size of overlap table "+ overlapTable.size() + " vs " + kb.relationSize.size());

        int overlap = overlap(r1, r2, overlapTable, triples);
        return overlap;
    }

    private static int subjectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb ;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triples = kb.relation2object2subject;

        System.out.println("Size of overlap table "+ overlapTable.size() + " vs " + kb.relationSize.size());

        int overlap = overlap(r1, r2, overlapTable, triples);
        return overlap;
    }


    private static int subjectToSubjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb ;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triples = kb.relation2object2subject;

        System.out.println("Size of overlap table "+ overlapTable.size() + " vs " + kb.relationSize.size());

        int overlap = overlap(r1, r2, overlapTable, triples);
        return overlap;
    }


    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     *
     * @param rule
     * @return total product operation iterating over the provided rule's body
     */
    private static double bodyEstimate(Rule rule) {
        double product = 1;
        List<int[]> body = rule.getBody();
        for (int id = 2; id < rule.getBodySize() + 1; id++) {
            int r = body.get(id)[RELATION_POSITION];
            int r_next = body.get(id + 1)[RELATION_POSITION];

            double survivalRate = 1;
            int rRange = range(r) ;
            if (rRange > 0)
                survivalRate = (double) subjectToObjectOverlap(r, r_next) / rRange;

            double inverseFunctionality = miniAMIE.kb.inverseFunctionality(r_next) ;

            if (inverseFunctionality > 0)
                product *= survivalRate / inverseFunctionality ;
        }
        return product;
    }

    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static double ApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = rule.getBody();
        int rFirstBodyAtom = body.get(0)[RELATION_POSITION];
        int rLastBodyAtom = body.get(body.size() - 1)[RELATION_POSITION];

        int headSize = miniAMIE.kb.relationSize(rHead);
        int objectToObjectOverlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        int domainHead = domain(rHead);

        int rangeHead = range(rHead);
        
        double survivalRate = 0;
        if (rangeHead > 0) {
            int subjectToSubjectOverlap = subjectToSubjectOverlap(rHead, rLastBodyAtom);
            survivalRate =  (double) subjectToSubjectOverlap / rangeHead;
        }
        double bodyEstimate = bodyEstimate(rule);

        double result = 0 ;
        if (domainHead > 0 && rangeHead > 0)
            result = (double) headSize * (double) headSize * (double) objectToObjectOverlap * (double) survivalRate * (double) bodyEstimate
                    / (double) domainHead * (double) rangeHead ;

        double real = rule.getSupport() ;
        System.out.println("ApproximateSupportClosedRule result " + result + " real " + real);
        if (result < 0) {
            System.out.println("neg " + result);
        }
        return result;
    }

    /**
     * Support approximation for an open rule
     *
     * @param rule Open rule
     * @return Support approximation
     */
    public static double ApproximateSupportOpenRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        int rFirstBodyAtom = rule.getBody().get(0)[RELATION_POSITION];

        int overlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        double bodyEstimate = bodyEstimate(rule);
        double fun = miniAMIE.kb.functionality(rHead);
        double result = 0 ;
        if (fun > 0)
            result = overlap * bodyEstimate / fun;
        double real = rule.getSupport() ;
        System.out.println("ApproximateSupportOpenRule result " + result + " real " + real);
        if (result < 0) {
            System.out.println("neg " + result);
        }
        return result ;
    }

    /**
     * GetInitRules provides a sample of rules with empty bodies and head coverage above provided minimum support.
     *
     * @param minSup
     * @return Collection of single atom rules
     */
    public static Collection<Rule> GetInitRules(double minSup) {
        return miningAssistant.getInitialAtoms(minSup);
    }
}
