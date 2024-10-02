package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.QueryEquivalenceChecker;
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
        int sumExploredRulesAdjustedWithBidirectionality;
        List<Rule> finalRules;

        public ExplorationResult(int sumExploredRules, int sumExploredRulesAdjustedWithBidirectionality,
                                 List<Rule> finalRules) {
            this.sumExploredRules = sumExploredRules;
            this.sumExploredRulesAdjustedWithBidirectionality = sumExploredRulesAdjustedWithBidirectionality;
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

    // bidirectionalityMap stores function result to avoid computing the same unions multiple times
    public static HashMap<Integer, Boolean> bidirectionalityMap = new HashMap<>();

    public static final double BidirectionalityJaccardThreshold = 0.95;
    private static boolean isBidirectional(int relation) {
        if (bidirectionalityMap.containsKey(relation)) {
            return bidirectionalityMap.get(relation);
        }
        IntSet range = range(relation) ;
        IntSet domain = domain(relation) ;
        IntSet rangeDomainUnion = new IntOpenHashSet(domain);
        rangeDomainUnion.addAll(range);
//        rangeDomainUnion.addAll(domain(relation));
        double bidirectionalityJaccard = (double) subjectToObjectOverlapSize(relation, relation)
                / rangeDomainUnion.size() ;
        if (bidirectionalityJaccard >= BidirectionalityJaccardThreshold) {
            bidirectionalityMap.put(relation, true);
            return true;
        }
        bidirectionalityMap.put(relation, false);
        return false;
    }

    static final int DEFAULT_CORRECTION_CLOSED = 2 ;
    static final int REDUCED_CORRECTION_CLOSED = 1 ;

    public static int ClosedCorrectingFactor(int lastAddedRelation) {
        return isBidirectional(lastAddedRelation) ? DEFAULT_CORRECTION_CLOSED : REDUCED_CORRECTION_CLOSED ;
    }


    static final int DEFAULT_CORRECTION_OPEN = 4 ;
    static final int REDUCED_CORRECTION_OPEN = 2 ;

    public static int OpenCorrectingFactor(int lastAddedRelation) {
        return isBidirectional(lastAddedRelation) ? DEFAULT_CORRECTION_OPEN : REDUCED_CORRECTION_OPEN ;
    }

    /**
     * AddClosureToEmptyBody adds a closure atom to an empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, Head object is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Pair<Rule, Integer>> AddClosureToEmptyBody(final Rule rule) {
        ArrayList<Pair<Rule, Integer>> closedRules = new ArrayList<>();
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

            int searchSpaceCorrectingFactor = ClosedCorrectingFactor(relation) ;
            closedRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));

        }

        return closedRules;
    }

    /**
     * AddClosure adds a closure atom to a non-empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, dangling's subject is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules paired with their correcting factor for search space size.
     */
    public static ArrayList<Pair<Rule, Integer>> AddClosure(final Rule rule) {
        ArrayList<Pair<Rule, Integer>> closedRules = new ArrayList<>();
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

            int searchSpaceCorrectingFactor = ClosedCorrectingFactor(relation) ;
            closedRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));
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
    public static ArrayList<Pair<Rule, Integer>> AddDanglingToEmptyBody(Rule rule) {
        ArrayList<Pair<Rule, Integer>> openRules = new ArrayList<>();
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

            int searchSpaceCorrectingFactor = OpenCorrectingFactor(relation) ;
            openRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));
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
    public static ArrayList<Pair<Rule, Integer>> AddDangling(Rule rule) {
        ArrayList<Pair<Rule, Integer>> openRules = new ArrayList<>();
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

            int searchSpaceCorrectingFactor = OpenCorrectingFactor(relation) ;
            openRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));
        }

        return openRules;
    }

    private static IntSet range(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> objectsToSubjects = kb.relation2object2subject.get(r);
        if (objectsToSubjects == null)
            return null;
        return objectsToSubjects.keySet() ;
    }

    private static IntSet domain(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> subjectsToObjects = kb.relation2subject2object.get(r);
        if (subjectsToObjects == null)
            return null;
        return subjectsToObjects.keySet() ;
    }

    private static int rangeSize(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int domainSize(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2subject2object.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static int overlapSize(int r1, int r2,
                                   Int2ObjectMap<Int2IntMap> overlapTable,
                                   Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1,
                                   Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2) {
        Int2IntMap factSet = overlapTable.get(r1);
        int overlap;
        if (factSet == null) {
            IntSet r1_set = triplesKeySet1.get(r1).keySet();
            IntSet r2_set = triplesKeySet2.get(r2).keySet();
            overlap = KB.computeOverlap(r1_set, r2_set);
//            List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
//            System.out.print("Overlap: ") ;
//            for (int value : overlapValues) {
//                System.out.print(" " + miniAMIE.kb.unmap(value));
//            }
//            System.out.println(" ") ;
//
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

//                List<Integer> overlapValues = KB.computeOverlapValues(r1_set, r2_set);
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
//        IntSet r1_set = triplesKeySet1.get(r1).keySet();
//        IntSet r2_set = triplesKeySet2.get(r2).keySet();
//        int real_overlap = KB.computeOverlap(r1_set, r2_set);
//        System.out.println(overlap + " real " + real_overlap);
        return overlap;
    }

    private static int objectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2object2subject;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static int subjectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }


    private static int subjectToSubjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    private static List<int[]> sortPerfectPathBody(Rule rule) {
        int bodySize = rule.getBody().size();
        rule.setBodySize(rule.getBody().size());
        if(bodySize < 2 )
            return rule.getBody() ;

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
        printBodyAsPerfectPath(body);


        int[] head = rule.getHead();
        System.out.print("=> ");
        System.out.print(
                miniAMIE.kb.unmap(head[SUBJECT_POSITION]) + " " +
                        miniAMIE.kb.unmap(head[RELATION_POSITION]) + " " +
                        miniAMIE.kb.unmap(head[OBJECT_POSITION])
        );
//        System.out.print(new Rule(head, body, -1, miniAMIE.kb));
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
        List<int[]> body = sortPerfectPathBody(rule);

        for (int id = 0; id < rule.getBodySize() - 1; id++) {

            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domainSize(r);
            int r_nextRng = rangeSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

//            if(r == miniAMIE.kb.map("<created>") && r_next == miniAMIE.kb.map("<hasGender>"))
//                System.err.println(rule+ ": " +miniAMIE.kb.unmap(r) + " -> " + miniAMIE.kb.unmap(r_next));
            double soOV = subjectToObjectOverlapSize(r, r_next);
//            double denom = rDom * r_nextRng;
//            double nom = soOV * r_nextSize;

            double factor = 0 ;
//            if (denom > 0) {
//                factor = nom/denom;
//            } else {
//                factor = 0;
//            }

            /////// ALTERNATIVE METHOD
            double ifun_next = (double) r_nextRng / r_nextSize;
            double soSurv = soOV / rDom ;
            if (ifun_next > 0) {
                factor = soSurv / ifun_next;
            }

            product *= factor;

//            if (miniAMIE.Verbose)
//                System.out.print(
//                        " { r["+(r_id)+"]: " + miniAMIE.kb.unmap(r) + " to " + " r["+(r_next_id)+"] (r_next)" + miniAMIE.kb.unmap(r_next)
//                                + " } "
//                                + " factor: " + factor
//                                + " product : " + product
//                                + " | soSurv " + soSurv
//                                + " | rDom " + rDom
//                                + " | soOV " + soOV
//                                + " | r_nextRng " + r_nextRng
//                                + " | r_nextSize " + r_nextSize);

            if (product == 0)
                break ;
        }
//        if (miniAMIE.Verbose)
//            System.out.print("\n");
        return product;
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
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        if(idFirst < 0)
            System.err.println(rule);
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = subjectToSubjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = domainSize(rHead);
        int domainLast = domainSize(rLastBodyAtom);
        int rangeFirst = rangeSize(rFirstBodyAtom);

        double bodyEstimate = bodyEstimate(rule);

        long result = 0;

        double nom = objectToObjectOverlap * subjectToSubjectOverlap * rFirstSize * rHeadSize;
        double denom = rangeFirst * domainHead * domainLast;

//        if (denom > 0) {
//            result = (long) (nom * bodyEstimate / denom);
//        }

        // ALTERNATIVE
        double inv_ifun_r1 =  rFirstSize / rangeFirst ;
        double inv_fun_rh = rHeadSize / domainHead ;
        double ssSurv = (double) subjectToSubjectOverlap / domainLast ;
        double factor1 = objectToObjectOverlap * inv_ifun_r1 ;
        double factor2 = ssSurv * inv_fun_rh ;
        double product = factor1 * factor2;
        result = (long) (product * bodyEstimate) ;

        if (miniAMIE.Verbose) {

            System.out.println(" s~ " + result
                    + " | bodyEstimate " + bodyEstimate
                    + " | rHead " + miniAMIE.kb.unmap(rHead)
                    + " | rLastBodyAtom " + miniAMIE.kb.unmap(rLastBodyAtom)
                    + " | objectToObjectOverlap " + objectToObjectOverlap
                    + " | subjectToSubjectOverlap " + subjectToSubjectOverlap
                    + " | rFirstSize " + rFirstSize
                    + " | rHeadSize " + rHeadSize
                    + " | rangeFirst " + rangeFirst
                    + " | domainHead " + domainHead
                    + " | domainLast " + domainLast
                    + " | inv_ifun_r1 " + inv_ifun_r1
                    + " | inv_fun_rh " + inv_fun_rh
                    + " | ssSurv " + ssSurv
                    + " | factor1 " + factor1
                    + " | factor2 " + factor2
                    + " | product " + product
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

        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];


        double bodyEstimate = bodyEstimate(rule);
        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rangeFirst = rangeSize(rFirstBodyAtom);
        double nom = objectToObjectOverlap * rFirstSize;
        double denom = rangeFirst ;
        double result = 0;
//        if (denom > 0)
//            result = (nom * bodyEstimate)/ denom;

        // ALTERNATIVE
        double inv_ifun_r1 =  rFirstSize / rangeFirst ;
        result = objectToObjectOverlap * inv_ifun_r1 * bodyEstimate ;

//        if (miniAMIE.Verbose) {
//            double realS = RealSupport(rule);
//            double errorRate = ErrorRate(realS, result);
//            double errorContrastRatio = ErrorContrastRatio(realS, result);
//            double errorLog = ErrorRateLog(realS, result);
//            System.out.println(ANSI_BLUE
//                    + "ApproximateSupportOpenRule " + ruleStr
//                    + " : s~ " + result
//                    + " | s " + realS
//                    + " | err (rate, contrast, log)  " + errorRate + " " + errorContrastRatio + " " + errorLog
//                    + " | ooOv " + objectToObjectOverlap
//                    + " | rangeHead " + rangeFirst
//                    + " | headSize " + rFirstSize
//                    + " | inv_ifun_r1 " + inv_ifun_r1
//                    + " | rHead " + miniAMIE.kb.unmap(rHead)
//                    + ANSI_RESET);
//        }

        return (long) result;
    }

    private static double bodyEstimate2(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);
        for (int id = 0; id < rule.getBodySize() - 1; id++) {

            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domainSize(r);
            int r_nextRng = rangeSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

            double soOV = subjectToObjectOverlapSize(r, r_next);
            double denom = rDom * r_nextRng;
            double nom = soOV * r_nextSize;

            double factor = 0 ;
            if (denom > 0) {
                factor = nom/denom;
            }
            product *= factor;

            if (product == 0)
                break ;
        }
        if (miniAMIE.Verbose)
            System.out.print("\n");
        return product;
    }
    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule2(Rule rule) {
        String ruleStr = rule.toString();
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = subjectToSubjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = domainSize(rHead);
        int domainLast = domainSize(rLastBodyAtom);
        int rangeFirst = rangeSize(rFirstBodyAtom);

        double bodyEstimate = bodyEstimate2(rule);

        long result = 0;

        double nom = objectToObjectOverlap * subjectToSubjectOverlap * rFirstSize * rHeadSize;
        double denom = rangeFirst * domainHead * domainLast;

        if (denom > 0) {
            result = (long) (nom * bodyEstimate / denom);
        }

        System.out.println("alt s~ " + result
                + " | bodyEstimate " + bodyEstimate
                + " | nom " + nom
                + " | denom " + denom
                + " | rHead " + miniAMIE.kb.unmap(rHead)
                + " | rLastBodyAtom " + miniAMIE.kb.unmap(rLastBodyAtom)
                + " | objectToObjectOverlap " + objectToObjectOverlap
                + " | subjectToSubjectOverlap " + subjectToSubjectOverlap
                + " | rFirstSize " + rFirstSize
                + " | rHeadSize " + rHeadSize
                + " | rangeFirst " + rangeFirst
                + " | domainHead " + domainHead
                + " | domainLast " + domainLast
        );

        return result;
    }

    public static long RealSupport(Rule rule) {
        return miniAMIE.kb.countProjection(rule.getHead(), rule.getTriples());
    }

    public static void InstantiateRule(Rule rule) {
        IntList variables = rule.getVariables() ;
        List<int[]> query = rule.getAntecedent() ;
        query.add(rule.getHead()) ;
        for(int variable: variables) {
            KB.Instantiator inst = new KB.Instantiator(query, variable) ;
            query = inst.instantiate(variable);
        }
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

    public enum RuleStateComparison {
        /** Rule has been found by mini-Amie and is in ground truth rule set */
        CORRECT,
        /** Rule has been found by mini-Amie but is not in ground truth rule set */
        FALSE,
        /** Rule has not been found by mini-Amie and is in ground truth rule set
        * BUT should not be found by mini-Amie (ex: rule is not a perfect path, rule has redundant relations) */
        MISSING_OK,
        /** Rule has not been found by mini_Amie and is in ground truth rule set
        * BUT should have been found by mini-Amie */
        MISSING_FAILURE
    }

    public static boolean IsRealPerfectPath(Rule rule) {
        boolean found_x = false ;
        boolean found_y = false ;

        if (rule.containsSinglePath()) {
            for(int[] atom: rule.getBody()) {
                if(atom[SUBJECT_POSITION] == rule.getHead()[SUBJECT_POSITION])
                    found_x = true ;
                if(atom[OBJECT_POSITION] == rule.getHead()[OBJECT_POSITION])
                    found_y = true ;
            }
        }
        return found_x && found_y ;
    }

    public static boolean HasNoRedundancies(Rule rule) {
        HashSet<Integer> relations = new HashSet<>();
        relations.add(rule.getHead()[RELATION_POSITION]);
        // Redundancy check
        for(int[] atom: rule.getBody()) {
            int relation = atom[RELATION_POSITION];
            if(relations.contains(relation)) {
                return false ;
            }
            relations.add(relation);
        }
        return true ;
    }

    /**
     * ShouldHaveBeenFound will seek for a perfect path
     * @param rule
     * @return
     */
    public static boolean ShouldHaveBeenFound(Rule rule) {
        return HasNoRedundancies(rule) && IsRealPerfectPath(rule);
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
        return QueryEquivalenceChecker.areEquivalent(groundTruthRule.getTriples(), rule.getTriples());
        //        // Comparing head relations
//        int[] groundTruthRuleHead = groundTruthRule.getHead();
//        int[] ruleHead = rule.getHead();
//        if (ruleHead[RELATION_POSITION] != groundTruthRuleHead[RELATION_POSITION])
//            return false;
//
//        // ---
//        List<int[]> groundTruthRuleBody = groundTruthRule.getBody();
//        List<int[]> ruleBody = rule.getBody();
//        int groundBodySize = groundTruthRuleBody.size();
//        int bodySize = ruleBody.size();
//
//        // Comparing body sizes
//        if (bodySize != groundBodySize)
//            return false;
//
//        // Absent body, equal body size
//        if (bodySize == 0)
//            return true;
//
//
//        Set<Integer> groundRelations = new HashSet<>();
//        groundRelations.add(groundTruthRuleHead[RELATION_POSITION]);
//        Set<Integer> relations = new HashSet<>();
//        relations.add(ruleHead[RELATION_POSITION]);
//
//        HashMap<Integer, List<HashSet<Integer>>> objectToRelationsGround = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> subjectToRelationsGround = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> objectToRelations = new HashMap<>();
//        HashMap<Integer, List<HashSet<Integer>>> subjectToRelations = new HashMap<>();
//
//        // Filling relation maps
//        for (int i = 0; i < bodySize; i++) {
//            int[] groundBodyAtom = groundTruthRuleBody.get(i);
//            int[] bodyAtom = ruleBody.get(i);
//
////            groundRelations.add(groundBodyAtom[RELATION_POSITION]);
////            relations.add(bodyAtom[RELATION_POSITION]);
//            if(groundRelations.contains(groundBodyAtom[RELATION_POSITION])) {
//                groundRelations.add(groundBodyAtom[RELATION_POSITION]);
//
//            } else {
//
//            }
//
//            relations.add(bodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> objectRelationGroundSet ;
//            if (objectToRelationsGround.containsKey(groundBodyAtom[OBJECT_POSITION])) {
//                objectRelationGroundSet = objectToRelationsGround.get(groundBodyAtom[OBJECT_POSITION]) ;
//            } else {
//                objectRelationGroundSet = new HashSet<>();
//                objectToRelationsGround.put(groundBodyAtom[OBJECT_POSITION], objectRelationGroundSet);
//            }
//            objectRelationGroundSet.add(groundBodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> objectRelationSet ;
//            if (objectToRelations.containsKey(bodyAtom[OBJECT_POSITION])) {
//                objectRelationSet = objectToRelations.get(bodyAtom[OBJECT_POSITION]) ;
//            } else {
//                objectRelationSet = new HashSet<>();
//                objectToRelations.put(bodyAtom[OBJECT_POSITION], objectRelationSet);
//            }
//            objectRelationSet.add(bodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> subjectRelationGroundSet ;
//            if (subjectToRelationsGround.containsKey(groundBodyAtom[SUBJECT_POSITION])) {
//                subjectRelationGroundSet = subjectToRelationsGround.get(groundBodyAtom[OBJECT_POSITION]) ;
//            } else {
//                subjectRelationGroundSet = new HashSet<>();
//                subjectToRelationsGround.put(groundBodyAtom[SUBJECT_POSITION], subjectRelationGroundSet);
//            }
//            subjectRelationGroundSet.add(groundBodyAtom[RELATION_POSITION]);
//
//            HashSet<Integer> subjectRelationSet ;
//            if (subjectToRelations.containsKey(bodyAtom[SUBJECT_POSITION])) {
//                subjectRelationSet = subjectToRelations.get(bodyAtom[OBJECT_POSITION]) ;
//            } else {
//                subjectRelationSet = new HashSet<>();
//                subjectToRelations.put(bodyAtom[SUBJECT_POSITION], subjectRelationSet);
//            }
//            subjectRelationSet.add(bodyAtom[RELATION_POSITION]);
//
//        }
//
//        // Comparing relations
//        if (!(groundRelations.containsAll(relations)) ||
//                !(relations.containsAll(groundRelations)))
//            return false;
//
//        // Filling the variable-relation hashmaps
//        for (int i = 0; i < bodySize; i++) {
//            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
//            int object = ruleBody.get(i)[OBJECT_POSITION];
//            List<Integer> groundRelationsWithObject = new ArrayList<>();
//            List<Integer> relationsWithObject = new ArrayList<>();
//            for (int k = 0; k < bodySize; k++) {
//                if (groundTruthRuleBody.get(i)[OBJECT_POSITION] == groundObject)
//                    groundRelationsWithObject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
//                if (ruleBody.get(i)[OBJECT_POSITION] == object)
//                    relationsWithObject.add(ruleBody.get(i)[RELATION_POSITION]);
//            }
//
//            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
//            int subject = ruleBody.get(i)[SUBJECT_POSITION];
//            List<Integer> groundRelationsWithSubject = new ArrayList<>();
//            List<Integer> relationsWithSubject = new ArrayList<>();
//            for (int k = 0; k < bodySize; k++) {
//                if (groundTruthRuleBody.get(i)[SUBJECT_POSITION] == groundSubject)
//                    groundRelationsWithSubject.add(groundTruthRuleBody.get(i)[RELATION_POSITION]);
//                if (ruleBody.get(i)[SUBJECT_POSITION] == subject)
//                    relationsWithSubject.add(ruleBody.get(i)[RELATION_POSITION]);
//            }
//        }
//
//        // Checking head variable object
//        int groundHeadObject = groundTruthRuleHead[OBJECT_POSITION];
//        int headObject = ruleHead[OBJECT_POSITION];
//        HashSet<Integer> headObjectRelationsGround = objectToRelationsGround.get(groundHeadObject);
//        HashSet<Integer> headObjectRelations = objectToRelations.get(headObject);
//
//        if ((headObjectRelations == null && headObjectRelationsGround != null)
//                || (headObjectRelationsGround == null && headObjectRelations != null))
//            return false;
//
//        if (headObjectRelations != null &&
//                (!headObjectRelations.containsAll(headObjectRelationsGround)
//                        || !headObjectRelationsGround.containsAll(headObjectRelations)))
//            return false;
//
//        // Checking head variable subject
//        int groundHeadSubject = groundTruthRuleHead[SUBJECT_POSITION];
//        int headSubject = ruleHead[SUBJECT_POSITION];
//        HashSet<Integer> headSubjectRelationsGround = subjectToRelationsGround.get(groundHeadSubject);
//        HashSet<Integer> headSubjectRelations = subjectToRelations.get(headSubject);
//
//        if ((headSubjectRelations == null && headSubjectRelationsGround != null)
//                || (headSubjectRelationsGround == null && headSubjectRelations != null))
//            return false;
//
//
//        if (headSubjectRelations != null &&
//                (!headSubjectRelations.containsAll(headSubjectRelationsGround)
//                        || !headSubjectRelations.containsAll(headSubjectRelations)))
//            return false;
//
//
//        // Comparing body
//        for (int i = 0; i < bodySize; i++) {
//            int groundObject = groundTruthRuleBody.get(i)[OBJECT_POSITION];
//            HashSet<Integer> objectRelationsGround = objectToRelationsGround.get(groundObject);
//
//            boolean objectFound = false;
//            // Comparing object variable relation set
//            for (int j = 0; j < bodySize; j++) {
//                HashSet<Integer> objectRelations = objectToRelations.get(groundObject);
//
//                if (
//                        (objectRelations == null && objectRelationsGround == null) ||
//                        (objectRelations != null &&
//                                (objectRelations.containsAll(objectRelationsGround) &&
//                        relations.containsAll(groundRelations)))
//                ) {
//                    objectFound = true;
//                    break;
//                }
//            }
//            if (!objectFound)
//                return false;
//
//            // Checking subject
//            int groundSubject = groundTruthRuleBody.get(i)[SUBJECT_POSITION];
//            HashSet<Integer> subjectRelationsGround = subjectToRelationsGround.get(groundSubject);
//
//            boolean subjectFound = false;
//            // Comparing subject variable relation set
//            for (int j = 0; j < bodySize; j++) {
//                HashSet<Integer> subjectRelations = subjectToRelations.get(groundSubject);
//
//                if ((subjectRelations == null && subjectRelationsGround == null) ||
//                (subjectRelations!=null &&
//                        (subjectRelations.containsAll(subjectRelationsGround) &&
//                                subjectRelationsGround.containsAll(subjectRelations)))
//                ) {
//                    subjectFound = true;
//                    break;
//                }
//            }
//            if (!subjectFound)
//                return false;
//        }
//        return true;
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
            String regexSpace = "[(\\ )|(\t)]+";
            String regexAtom = "(\\?[a-z]" + regexSpace + "<[^>]+>" + regexSpace + "\\?[a-z]" + regexSpace + ")";
            String regexBody = "(" + regexAtom + "+)";
            String regexRule = "(" + regexBody + "=> [(\\ )|(\t)]*" + regexAtom + ")";
            Pattern pat = Pattern.compile(regexRule);
            while (line != null) {
                List<int[]> bodyAtoms = new ArrayList<>();
                int[] headAtom;
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
                    String subjectString = headParts[SUBJECT_POSITION];
                    String relationString = headParts[RELATION_POSITION];
                    String objectString = headParts[OBJECT_POSITION];

                    if(miniAMIE.RestrainedHead != null &&
                            !miniAMIE.RestrainedHead.isEmpty() &&
                            !Objects.equals(relationString, miniAMIE.RestrainedHead))
                        break ;
                    int subject = miniAMIE.kb.map(subjectString);
                    int relation = miniAMIE.kb.map(relationString);
                    int object = miniAMIE.kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};
                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.kb);
                    groundTruthRules.add(groundTruthRule);
//                    line = reader.readLine();
                } else {
                    System.err.println("Could not find ground truth rule in "+line);
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
//        System.out.println("groundTruthRules "+ groundTruthRules);
        return groundTruthRules;
    }

    static class FactorsOfBody {
        String atomRelation = "atomRelation: ";
        String nextAtomRelation = "nextAtomRelation: " ;
        String atomRelationDomain = "atomRelationDomain: -1" ;
        String nextAtomRelationSize = "nextAtomRelationSize: -1" ;
        String nextAtomRelationIfun = "nextAtomRelationIfun: -1" ;
        String atomRelationSubjectToNextAtomRelationObjectOverlap =
                "atomRelationSubjectToNextAtomRelationObjectOverlap: -1" ;

        public FactorsOfBody(String atomRelation, String nextAtomRelation, double atomRelationDomain,
                             double nextAtomRelationSize, double nextAtomRelationIfun,
                             double atomRelationSubjectToNextAtomRelationObjectOverlap) {
            this.atomRelation = "atomRelation: "+atomRelation;
            this.nextAtomRelation = "nextAtomRelation: "+nextAtomRelation;
            this.atomRelationDomain = "atomRelationDomain: "+atomRelationDomain ;
            this.nextAtomRelationSize = "nextAtomRelationSize: "+nextAtomRelationSize;
            this.nextAtomRelationIfun = "nextAtomRelationIfun: "+nextAtomRelationIfun;
            this.atomRelationSubjectToNextAtomRelationObjectOverlap =
                    "atomRelationSubjectToNextAtomRelationObjectOverlap: "
                            +atomRelationSubjectToNextAtomRelationObjectOverlap;
        }
    }
    public static String sep = ",";
    public static String bodySep = ";" ;
    static class FactorsOfApproximateSupportClosedRule {
        String relationHeadAtom = "" ;
        String relationFirstBodyAtom = "";
        String relationLastBodyAtom = "";
        double headAtomObjectToFirstBodyAtomObjectOverlap = -1 ;
        double lastBodyAtomSubjectToHeadAtomSubjectOverlap = -1 ;
        double relationFirstBodyAtomSize = -1 ;
        double relationHeadSize = -1 ;
        double rangeFirstBodyAtom = -1 ;
        double domainHeadAtom = -1 ;
        double domainLastBodyAtom = -1 ;
        double ifunRelationFirstBodyAtom = -1 ;
        double funRelationHeadAtom = -1 ;
        double bodyEstimate = -1 ;
        List<FactorsOfBody> factorsOfBodies = new ArrayList<>();

        public FactorsOfApproximateSupportClosedRule() {
        }



        public FactorsOfApproximateSupportClosedRule(String relationHeadAtom, String relationFirstBodyAtom,
                                                     String relationLastBodyAtom,
                                                     double headAtomObjectToFirstBodyAtomObjectOverlap,
                                                     double lastBodyAtomSubjectToHeadAtomSubjectOverlap,
                                                     double relationFirstBodyAtomSize, double relationHeadSize,
                                                     double rangeFirstBodyAtom, double domainHeadAtom,
                                                     double domainLastBodyAtom, double ifunRelationFirstBodyAtom,
                                                     double funRelationHeadAtom, double bodyEstimate,
                                                     List<FactorsOfBody> factorsOfBodies) {
            this.relationHeadAtom = relationHeadAtom;
            this.relationFirstBodyAtom = relationFirstBodyAtom;
            this.relationLastBodyAtom = relationLastBodyAtom;
            this.headAtomObjectToFirstBodyAtomObjectOverlap = headAtomObjectToFirstBodyAtomObjectOverlap;
            this.lastBodyAtomSubjectToHeadAtomSubjectOverlap = lastBodyAtomSubjectToHeadAtomSubjectOverlap;
            this.relationFirstBodyAtomSize = relationFirstBodyAtomSize;
            this.relationHeadSize = relationHeadSize;
            this.rangeFirstBodyAtom = rangeFirstBodyAtom;
            this.domainHeadAtom = domainHeadAtom;
            this.domainLastBodyAtom = domainLastBodyAtom;
            this.ifunRelationFirstBodyAtom = ifunRelationFirstBodyAtom;
            this.funRelationHeadAtom = funRelationHeadAtom;
            this.bodyEstimate = bodyEstimate;
            this.factorsOfBodies = factorsOfBodies;
        }

        @Override
        public String toString() {
            String formatFactors =  "[" ;
            for(FactorsOfBody factors: factorsOfBodies) {
                formatFactors += "{"
                        + factors.atomRelationDomain + bodySep
                        + factors.nextAtomRelation + bodySep
                        + factors.atomRelationDomain + bodySep
                        + factors.nextAtomRelationSize + bodySep
                        + factors.atomRelationSubjectToNextAtomRelationObjectOverlap
                        + "} " ;
            }
            formatFactors += "]" ;
            return "" + relationHeadAtom + sep
                    + relationFirstBodyAtom + sep
                    + relationLastBodyAtom + sep
                    + headAtomObjectToFirstBodyAtomObjectOverlap + sep
                    + lastBodyAtomSubjectToHeadAtomSubjectOverlap + sep
                    + relationFirstBodyAtomSize + sep
                    + relationHeadSize + sep
                    + rangeFirstBodyAtom + sep
                    + domainHeadAtom + sep
                    + domainLastBodyAtom + sep
                    + ifunRelationFirstBodyAtom + sep
                    + funRelationHeadAtom + sep
                    + bodyEstimate + sep
                    + formatFactors ;
        }
    }


    public static List<FactorsOfBody> GetFactorsOfBody(Rule rule) {
        List<FactorsOfBody> factorsOfBodies = new ArrayList<>();
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);
        for (int id = 0; id < rule.getBodySize() - 1; id++) {
            int last_id = (int) rule.getBodySize() - 1 ;
            int r_id =  last_id - id ;
            int r_next_id = last_id - id - 1 ;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domainSize(r);
            int r_nextRng = rangeSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);
            double soOV = subjectToObjectOverlapSize(r, r_next);
            FactorsOfBody factors = new FactorsOfBody(
                    miniAMIE.kb.unmap(r),
                    miniAMIE.kb.unmap(r_next),
                    rDom,
                    r_nextSize,
                    r_nextRng,
                    soOV) ;
            factorsOfBodies.add(factors) ;
        }
        return factorsOfBodies;
    }

    public static FactorsOfApproximateSupportClosedRule GetFactorsOfApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        rule.setBodySize(body.size());
        int bodySize = body.size();
        int idFirst = bodySize - 1 ;
        if(idFirst < 0)
            System.err.println(rule);
        int idLast = 0 ;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int subjectToSubjectOverlap = subjectToSubjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int domainHead = domainSize(rHead);
        int domainLast = domainSize(rLastBodyAtom);
        int rangeFirst = rangeSize(rFirstBodyAtom);

        double bodyEstimate = bodyEstimate(rule);
        double ifun_r1 =  rangeFirst / rFirstSize  ;
        double fun_rh =  domainHead / rHeadSize;

        return new FactorsOfApproximateSupportClosedRule(
                miniAMIE.kb.unmap(rHead),
                miniAMIE.kb.unmap(rFirstBodyAtom),
                miniAMIE.kb.unmap(rLastBodyAtom),
                objectToObjectOverlap,
                subjectToSubjectOverlap,
                rFirstSize,
                rHeadSize,
                rangeFirst,
                domainHead,
                domainLast,
                ifun_r1,
                fun_rh,
                bodyEstimate,
                GetFactorsOfBody(rule)
        ) ;
    }


}
