package amie.mining.miniAmie;

import amie.data.KB;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class utils {

    public static MiningAssistant miningAssistant;

    public static final int ATOM_SIZE = 3;

    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1;

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

    protected static List<Integer> SelectRelations() {
        List<Integer> relations = new ArrayList<>();

        for (int relation : miniAMIE.kb.getRelations()) {

            if (miniAMIE.kb.relationSize(relation) >= miniAMIE.MinSup)
                relations.add(relation);
        }
        return relations;
    }

    private static boolean RuleDoesNotContainsRelation(Rule rule, int relation) {

        if (relation == rule.getHead()[RELATION_POSITION])
            return false;

        // Seeking unwanted duplicates
        for (int[] atom : rule.getBody()) {
            if (relation == atom[RELATION_POSITION])
                return false;
        }
        return true;
    }

    private static List<Integer> PromisingRelations(Rule rule) {
        List<Integer> relations = new ArrayList<>();

        for (int relation : miniAMIE.SelectedRelations) {
            if (RuleDoesNotContainsRelation(rule, relation))
                relations.add(relation);
        }
        return relations;
    }

    /**
     * AddClosureToEmptyBody adds a closure atom to an empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, Head object is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Rule> AddClosureToEmptyBody(final Rule rule) {
        ArrayList<Rule> closedRules = new ArrayList<>();
        int[] headAtom = rule.getHead();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = headAtom[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = headAtom[OBJECT_POSITION];
            closedRules.add(closedRule);
        }

        return closedRules;
    }

    /**
     * AddClosure adds a closure atom to a non-empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, dangling's subject is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Rule> AddClosure(final Rule rule) {
        ArrayList<Rule> closedRules = new ArrayList<>();
        int[] headAtom = rule.getHead();
        int[] lastBodyAtom = rule.getLastTriplePattern();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = headAtom[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
            closedRules.add(closedRule);
        }

        return closedRules;
    }


    /**
     * AddDanglingToEmptyBody adds a dangling atom to an empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Rule> AddDanglingToEmptyBody(Rule rule) {
        ArrayList<Rule> openRules = new ArrayList<>();
        int[] headAtom = rule.getHead();

        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = closedRule.fullyUnboundTriplePattern()[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = headAtom[OBJECT_POSITION];
            openRules.add(closedRule);
        }

        return openRules;
    }

    /**
     * AddDangling adds a dangling atom to an empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Rule> AddDangling(Rule rule) {
        ArrayList<Rule> openRules = new ArrayList<>();
        int[] lastBodyAtom = rule.getLastTriplePattern();

        List<Integer> closureRelations = PromisingRelations(rule);

        if (closureRelations.isEmpty()) {
            return null;
        }

        for (int relation : closureRelations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = closedRule.fullyUnboundTriplePattern()[SUBJECT_POSITION];
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = lastBodyAtom[SUBJECT_POSITION];
            openRules.add(closedRule);
        }

        return openRules;
    }


    private static int range(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int domain(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2subject2object.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int overlap(int r1, int r2,
                               Int2ObjectMap<Int2IntMap> overlapTable,
                               Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1,
                               Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2) {
        Int2IntMap factSet = overlapTable.get(r1);
        int overlap;
        if (factSet == null) {
            IntSet r1_set = triplesKeySet1.get(r1).keySet();
            IntSet r2_set = triplesKeySet2.get(r2).keySet();
            overlap = KB.computeOverlap(r1_set, r2_set);
            List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
//            System.out.print("Overlap: ") ;
//            for (int value : overlapValues) {
//                System.out.print(" " + miniAMIE.kb.unmap(value));
//            }
//            System.out.println(" ") ;

//            System.out.println("Adding " + miniAMIE.kb.unmap(r1) + " " + miniAMIE.kb.unmap(r2) + " overlap "+overlap);
            Int2IntMap overlaps1 = new Int2IntOpenHashMap();
            overlaps1.put(r2, overlap);
            overlapTable.put(r1, overlaps1);

            Int2IntMap overlaps2 = new Int2IntOpenHashMap();
            overlaps2.put(r1, overlap);
            overlapTable.put(r2, overlaps2);

        } else {
            overlap = factSet.getOrDefault(r2, NO_OVERLAP_VALUE);
//            System.out.println("factSet " + factSet + " r1 " + r1 + " overlap " + overlap) ;
            if (overlap == NO_OVERLAP_VALUE) {
                IntSet r1_set = triplesKeySet1.get(r1).keySet();
                IntSet r2_set = triplesKeySet2.get(r2).keySet();
//                System.out.println(" r1_sets " + r1_set + " r2_sets " + r2_set);
                overlap = KB.computeOverlap(r1_set, r2_set);

                List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
//                System.out.print("Overlap: ") ;
//                for (int value : overlapValues) {
//                    System.out.print(" " + miniAMIE.kb.unmap(value));
//                }
//                System.out.println(" ") ;

//                System.out.println("Adding "+ r1 + " = "+ miniAMIE.kb.unmap(r1) +
//                        " " + r2 + " = "+ miniAMIE.kb.unmap(r2) + " overlap "+ overlap);
                factSet.put(r1, overlap);
                factSet.put(r2, overlap);
//            } else {
//                System.out.println("Found " + r1 + " = "+ miniAMIE.kb.unmap(r1) +
//                        " " + r2 + " = "+ miniAMIE.kb.unmap(r2) + " overlap "+ overlap);
            }
        }
        IntSet r1_set = triplesKeySet1.get(r1).keySet();
        IntSet r2_set = triplesKeySet2.get(r2).keySet();
        int real_overlap = KB.computeOverlap(r1_set, r2_set);
//        System.out.println(overlap + " real " + real_overlap);
        return overlap;
    }

    private static int objectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2object2subject;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static int subjectToObjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }


    private static int subjectToSubjectOverlap(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlap(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static List<int[]> sortPerfectPathBody(Rule rule) {
        int bodySize = rule.getBody().size();
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = rule.getBody();
        List<int[]> sortedBody = new ArrayList<>();
        int[] head = rule.getHead();
        int var = head[SUBJECT_POSITION];
        for (int i = 0; i < bodySize + 1; i++) {
//            System.err.println("Looking for atom with object " + miniAMIE.kb.unmap(var)) ;
            for (int[] atom : body)
                if (atom[SUBJECT_POSITION] == var) {
//                    System.err.println("Found " + miniAMIE.kb.unmap(atom[RELATION_POSITION])) ;
                    sortedBody.add(atom);
                    var = atom[OBJECT_POSITION];
                    break;
                }
        }
        return sortedBody;
    }

    private static void printBodyAsPerfectPath(List<int[]> body) {
        for (int[] triple : body) {
            System.out.print(
                    miniAMIE.kb.unmap(triple[SUBJECT_POSITION]) + " " +
                            miniAMIE.kb.unmap(triple[RELATION_POSITION]) + " " +
                            miniAMIE.kb.unmap(triple[OBJECT_POSITION]) + " "
            );
        }
    }

    protected static void printRuleAsPerfectPath(Rule rule) {
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);
//        printBodyAsPerfectPath(body);


        int[] head = rule.getHead();
//        System.out.print("=> ");
//        System.out.print(
//                miniAMIE.kb.unmap(head[SUBJECT_POSITION]) + " " +
//                        miniAMIE.kb.unmap(head[RELATION_POSITION]) + " " +
//                        miniAMIE.kb.unmap(head[OBJECT_POSITION])
//        );
        System.out.print(new Rule(head, body, -1, miniAMIE.kb));
    }

    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     *
     * @param rule
     * @return total product operation iterating over the provided rule's body
     */
    private static double bodyEstimate(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
//        List<int[]> body = rule.getBody();
        List<int[]> body = sortPerfectPathBody(rule);
        if (miniAMIE.Verbose) {
            System.out.print("Body: ");
            for (int[] triple : body) {
                System.out.print(
                        miniAMIE.kb.unmap(triple[SUBJECT_POSITION]) + " " +
                                miniAMIE.kb.unmap(triple[RELATION_POSITION]) + " " +
                                miniAMIE.kb.unmap(triple[OBJECT_POSITION])
                );
            }

            System.out.print("\nBody estimate: (size: " + rule.getBodySize() + ") ");
        }
        for (int id = 0; id < rule.getBodySize() - 1; id++) {
            int r = body.get(id)[RELATION_POSITION];
            int r_next = body.get(id + 1)[RELATION_POSITION];
            // Computing SO Survival rate
            double survivalRate = 1;
            int dom = domain(r);
            double soOV = subjectToObjectOverlap(r_next, r);
            if (dom > 0) {
                survivalRate = soOV / dom;
            }

            double inverseFunctionality = miniAMIE.kb.inverseFunctionality(r);

            if (inverseFunctionality > 0) {
                product *= survivalRate / inverseFunctionality;
            }
            if (miniAMIE.Verbose)
                System.out.print(
                        " { r: " + miniAMIE.kb.unmap(r) + " to " + " r_next: " + miniAMIE.kb.unmap(r_next)
                                + " } survival " + survivalRate
                                + " | r_next domain " + dom
                                + " | soOV " + soOV
                                + " | inverseFunctionality " + inverseFunctionality
                                + " | product " + product);
        }
        if (miniAMIE.Verbose)
            System.out.print("\n");
        return product;
    }

    public static long RealSupport(Rule rule) {
        return miniAMIE.kb.countProjection(rule.getHead(), rule.getTriples());
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    protected static double ErrorContrastRatio(double real, double estimate) {
        double delta = estimate - real;
        double total = estimate + real;
        return delta / total;
    }

    protected static double ErrorRate(double real, double estimate) {
        double delta = estimate - real;
        double total = real + 1;
        return delta / total;
    }

    protected static double ErrorRateLog(double real, double estimate) {
        double logReal = Math.log10(real + 1);
        double logEstimate = Math.log10(estimate + 1);
        return logReal - logEstimate;
    }

    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule(Rule rule) {
        String ruleStr = rule.toString();
        if (miniAMIE.Verbose)
            System.out.println(ANSI_GREEN
                    + "ApproximateSupportClosedRule " + ruleStr);
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = rule.getBody();
        int rFirstBodyAtom = body.get(0)[RELATION_POSITION];
        int rLastBodyAtom = body.get(body.size() - 1)[RELATION_POSITION];


        int headSize = miniAMIE.kb.relationSize(rHead);
//        System.out.println("Object to Object : ");
        int objectToObjectOverlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        int domainHead = domain(rHead);
        int domainLast = domain(rLastBodyAtom);

        int rangeHead = range(rHead);
        int rangeLast = range(rLastBodyAtom);


        double survivalRate = 0;
        int subjectToSubjectOverlap = 0;
        if (rangeHead > 0) {
//            System.out.print("Subject to Subject : ");
            subjectToSubjectOverlap = subjectToSubjectOverlap(rLastBodyAtom, rHead);
//            survivalRate = (double) subjectToSubjectOverlap / rangeHead;
            survivalRate = (double) subjectToSubjectOverlap / domainLast;
        }
        double bodyEstimate = bodyEstimate(rule);
        long result = 0;

        double headSizeMulOv = (double) headSize * (double) headSize * (double) objectToObjectOverlap;
        double survMulBod = survivalRate * bodyEstimate;
        double nom = headSizeMulOv * survMulBod;
//        double denom = (double) domainHead * (double) rangeHead * (double) rangeLast;
        double denom = (double) domainHead * (double) rangeHead;


        if (domainHead > 0 && rangeHead > 0) {
            result = (long) (nom / denom);
        }
        if (miniAMIE.Verbose) {
            double realS = RealSupport(rule);
            double errorRate = ErrorRate(realS, result);
            double errorContrastRatio = ErrorContrastRatio(realS, result);
            double errorLog = ErrorRateLog(realS, result);

            if (errorRate > 0.5)
                System.out.print(ANSI_YELLOW);
            else if (errorRate < -0.5)
                System.out.print(ANSI_CYAN);

            System.out.println(" s~ " + result
                    + " | s " + RealSupport(rule)
                    + " | err (rate, contrast, log)  " + errorRate + " " + errorContrastRatio + " " + errorLog
                    + " | head -> last ooOv " + objectToObjectOverlap
                    + " | first -> head ssOv " + subjectToSubjectOverlap
                    + " | survival " + survivalRate
                    + " | body " + bodyEstimate
                    + " | headSize " + headSize
                    + " | domainHead " + domainHead
                    + " | rangeHead " + rangeHead
                    + " | survMulBod " + survMulBod
                    + " | headSizeMulOv " + headSizeMulOv
                    + " | nom " + nom
                    + " | denom " + denom
                    + ANSI_RESET
            );
        }
        return result;
    }

    /**
     * Support approximation for an open rule
     *
     * @param rule Open rule
     * @return Support approximation
     */
    public static long ApproximateSupportOpenRule(Rule rule) {
        String ruleStr = rule.toString();
        int rHead = rule.getHeadRelationBS();
        int rFirstBodyAtom = rule.getBody().get(0)[RELATION_POSITION];

        int overlap = objectToObjectOverlap(rHead, rFirstBodyAtom);
        double bodyEstimate = bodyEstimate(rule);
//        double fun = miniAMIE.kb.inverseFunctionality(rHead);
        int headSize = miniAMIE.kb.relationSize(rHead);
        double rangeHead = range(rHead);
        double denom = rangeHead * rangeHead;
        double nom = overlap * headSize;
        double result = 0;
        if (denom > 0)
            result = nom / denom;

        if (miniAMIE.Verbose) {
            double realS = RealSupport(rule);
            double errorRate = ErrorRate(realS, result);
            double errorContrastRatio = ErrorContrastRatio(realS, result);
            double errorLog = ErrorRateLog(realS, result);
            System.out.println(ANSI_BLUE
                    + "ApproximateSupportOpenRule " + ruleStr
                    + " : s~ " + result
                    + " | s " + realS
                    + " | err (rate, contrast, log)  " + errorRate + " " + errorContrastRatio + " " + errorLog
                    + " | ooOv " + overlap
                    + " | rangeHead " + rangeHead
                    + " | headSize " + headSize
                    + " | nom " + nom
                    + " | denom " + denom
                    + ANSI_RESET);
        }

        return (long) result;
    }

    public enum RuleStateComparison {
        CORRECT,
        FALSE,
        MISSING
    }

    /**
     * CompareRules will return true if two rules are equivalent (considering atom positions and variable naming)
     * (ex: ?a  < worksAt >  ?c ?c  < isLocatedIn >  ?b => ?a  < isCitizenOf >  ?b
     *  and ?d  < isLocatedIn >  ?b ?a  < worksAt >  ?d => ?a  < isCitizenOf >  ?b are equivalent)
     * @param groundTruthRule
     * @param rule
     * @return
     */
    public static boolean CompareRules(Rule groundTruthRule, Rule rule) {
        int[] groundTruthRuleHead = groundTruthRule.getHead();
        int[] ruleHead = rule.getHead();

        if (ruleHead[RELATION_POSITION] != groundTruthRuleHead[RELATION_POSITION])
            return false;
        List<int[]> groundTruthRuleBody = groundTruthRule.getBody();
        List<int[]> ruleBody = rule.getBody();
        int groundBodySize = groundTruthRuleBody.size();
        int bodySize = ruleBody.size();
        if (bodySize != groundBodySize)
            return false;

        HashSet<Integer> groundBodyRelations = new HashSet<>();
        HashSet<Integer> bodyRelations = new HashSet<>();
        HashMap<Integer, HashSet<Integer>> objectToRelationsGround = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> subjectToRelationsGround = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> objectToRelations = new HashMap<>();
        HashMap<Integer, HashSet<Integer>> subjectToRelations = new HashMap<>();

        for (int i = 0; i < bodySize; i++) {
            groundBodyRelations.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
            bodyRelations.add(ruleBody.get(i)[RELATION_POSITION]);
        }

        // Comparing relation lists
        if (!(groundBodyRelations.containsAll(bodyRelations)) ||
                !(bodyRelations.containsAll(groundBodyRelations)))
            return false;

        // Filling the variable-relation hashmaps
        for (int i = 0; i < bodySize; i++) {
            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
            int object = ruleBody.get(i)[OBJECT_POSITION];
            List<Integer> groundRelationsWithObject = new ArrayList<>();
            List<Integer> relationsWithObject = new ArrayList<>();
            for (int k = 0; k < bodySize; k++) {
                if (groundTruthRuleBody.get(i)[OBJECT_POSITION] == groundObject)
                    groundRelationsWithObject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
                if (ruleBody.get(i)[OBJECT_POSITION] == object)
                    relationsWithObject.add(ruleBody.get(i)[RELATION_POSITION]);
            }

            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
            int subject = ruleBody.get(i)[SUBJECT_POSITION];
            List<Integer> groundRelationsWithSubject = new ArrayList<>();
            List<Integer> relationsWithSubject = new ArrayList<>();
            for (int k = 0; k < bodySize; k++) {
                if (groundTruthRuleBody.get(i)[SUBJECT_POSITION] == groundSubject)
                    groundRelationsWithSubject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
                if (ruleBody.get(i)[SUBJECT_POSITION] == subject)
                    relationsWithSubject.add(ruleBody.get(i)[RELATION_POSITION]);
            }
        }

        // Checking head variable object
        int groundHeadObject = groundTruthRuleHead[OBJECT_POSITION];
        int headObject = ruleHead[OBJECT_POSITION];
        HashSet<Integer> headObjectRelationsGround = objectToRelationsGround.get(groundHeadObject);
        HashSet<Integer> headObjectRelations = objectToRelations.get(headObject);

        if ((headObjectRelations == null && headObjectRelationsGround != null)
                || (headObjectRelationsGround == null && headObjectRelations != null))
            return false;

        if (headObjectRelations != null &&
                (!headObjectRelations.containsAll(headObjectRelationsGround)
                        || !headObjectRelationsGround.containsAll(headObjectRelations)))
            return false;

        // Checking head variable subject
        int groundHeadSubject = groundTruthRuleHead[SUBJECT_POSITION];
        int headSubject = ruleHead[SUBJECT_POSITION];
        HashSet<Integer> headSubjectRelationsGround = subjectToRelationsGround.get(groundHeadSubject);
        HashSet<Integer> headSubjectRelations = subjectToRelations.get(headSubject);

        if ((headSubjectRelations == null && headSubjectRelationsGround != null)
                || (headSubjectRelationsGround == null && headSubjectRelations != null))
            return false;


        if (headSubjectRelations != null &&
                (!headSubjectRelations.containsAll(headSubjectRelationsGround)
                        || !headSubjectRelations.containsAll(headSubjectRelations)))
            return false;


        // Comparing body
        for (int i = 0; i < bodySize; i++) {
            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
            HashSet<Integer> objectRelationsGround = objectToRelationsGround.get(groundObject);

            boolean objectFound = false;
            // Comparing object variable relation set
            for (int j = 0; j < bodySize; j++) {
                HashSet<Integer> objectRelations = objectToRelations.get(groundObject);

                if ( (objectRelations == null && objectRelationsGround == null) ||
                        (objectRelations.containsAll(objectRelationsGround) &&
                        bodyRelations.containsAll(groundBodyRelations))) {
                    objectFound = true;
                    break;
                }
            }
            if (!objectFound)
                return false;

            // Checking subject
            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
            HashSet<Integer> subjectRelationsGround = subjectToRelationsGround.get(groundSubject);

            boolean subjectFound = false;
            // Comparing subject variable relation set
            for (int j = 0; j < bodySize; j++) {
                HashSet<Integer> subjectRelations = subjectToRelations.get(groundSubject);

                if ((subjectRelations == null && subjectRelationsGround == null) ||
                        (subjectRelations.containsAll(subjectRelationsGround) &&
                        bodyRelations.containsAll(groundBodyRelations))) {
                    subjectFound = true;
                    break;
                }
            }
            if (!subjectFound)
                return false;
        }
        return true;
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


    public static List<Rule> LoadGroundTruthRules() {
        List<Rule> groundTruthRules = new ArrayList<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(miniAMIE.pathToGroundTruthRules));
            String line = reader.readLine();

            while (line != null) {
                List<int[]> bodyAtoms = new ArrayList<>();
                int[] headAtom;
//                System.out.println(line);

                String regexSpace = "[(\\ )|(\t)]+";
                String regexAtom = "(\\?[a-z]" + regexSpace + "<[a-zA-Z]+>" + regexSpace + "\\?[a-z]" + regexSpace + ")";
                String regexBody = "(" + regexAtom + "+)";
                String regexRule = "(" + regexBody + "=> [(\\ )|(\t)]*" + regexAtom + ")";
                Pattern pat = Pattern.compile(regexRule);
                Matcher matcher = pat.matcher(line);
                if (matcher.find()) {
                    String bodyString = matcher.group(2);
                    String[] bodyParts = bodyString.split(regexSpace);
                    for (int i = 0; i < bodyParts.length; i += 3) {
                        String subjectString = bodyParts[i];
                        String relationString = bodyParts[i + 1];
                        String objectString = bodyParts[i + 2];

                        int subject = miniAMIE.kb.map(subjectString);
                        int relation = miniAMIE.kb.map(relationString);
                        int object = miniAMIE.kb.map(objectString);

                        bodyAtoms.add(new int[]{subject, relation, object});
                    }
                    String headString = matcher.group(4);
                    String[] headParts = headString.split(regexSpace);
                    String subjectString = headParts[0];
                    String relationString = headParts[1];
                    String objectString = headParts[2];

                    int subject = miniAMIE.kb.map(subjectString);
                    int relation = miniAMIE.kb.map(relationString);
                    int object = miniAMIE.kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};

                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.kb);
                    groundTruthRules.add(groundTruthRule);
                    line = reader.readLine();
                }

            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return groundTruthRules;
    }
}
