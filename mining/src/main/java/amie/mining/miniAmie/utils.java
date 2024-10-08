package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;


import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    static int rangeSize(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    static int domainSize(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2subject2object.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    private static Lock overlapLock = new ReentrantLock();
    private static int overlapSize(int r1, int r2,
                                   Int2ObjectMap<Int2IntMap> overlapTable,
                                   Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1,
                                   Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2) {
        overlapLock.lock();
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

            Int2IntMap overlaps2 = new Int2IntOpenHashMap();
            overlaps2.put(r1, overlap);

            overlapTable.put(r1, overlaps1);
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
        overlapLock.unlock();
        return overlap;
    }

    static int objectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2object2subject;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    static int subjectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }


    static int subjectToSubjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    static List<int[]> sortPerfectPathBody(Rule rule) {
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
    static double bodyEstimate(Rule rule) {
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
            System.out.println(CompareToGT.ANSI_GREEN
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

    protected static double getSizeOfHead(Rule rule) {
        return miniAMIE.kb.relationSize(rule.getHead()[RELATION_POSITION]) ;
    }
    public static double ApproximateHeadCoverageOpenRule(Rule rule) {
        return ApproximateSupportOpenRule(rule) / getSizeOfHead(rule) ;
    }

    public static double ApproximateHeadCoverageClosedRule(Rule rule) {
        return ApproximateSupportClosedRule(rule) / getSizeOfHead(rule) ;
    }

    public static long RealSupport(Rule rule) {
        return miniAMIE.kb.countProjection(rule.getHead(), rule.getTriples());
    }

    // RealHeadCoverage computes head coverage for a rule. /!\ Relies on pre-existing support value in rule
    public static double RealHeadCoverage(Rule rule) {
        return rule.getSupport() / getSizeOfHead(rule);
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
