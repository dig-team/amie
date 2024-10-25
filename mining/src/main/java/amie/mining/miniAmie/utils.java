package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.mining.miniAmie.miniAMIE.NThreads;
import static amie.mining.miniAmie.miniAMIE.executor;

public class utils {

    public static MiningAssistant miningAssistant;

    public static final int ATOM_SIZE = 3;

    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1;
    protected static String commaSep = ",";

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
    public static ConcurrentHashMap<Integer, Boolean> bidirectionalityMap = new ConcurrentHashMap<>();

    public static final double BidirectionalityJaccardThreshold = 0.95;

    private static boolean isBidirectional(int relation) {
        if (bidirectionalityMap.containsKey(relation)) {
            return bidirectionalityMap.get(relation);
        }
        IntSet range = range(relation);
        IntSet domain = domain(relation);
        IntSet rangeDomainUnion = new IntOpenHashSet(domain);
        rangeDomainUnion.addAll(range);
//        rangeDomainUnion.addAll(domain(relation));
        double bidirectionalityJaccard = (double) subjectToObjectOverlapSize(relation, relation)
                / rangeDomainUnion.size();
        if (bidirectionalityJaccard >= BidirectionalityJaccardThreshold) {
            bidirectionalityMap.put(relation, true);
            return true;
        }
        bidirectionalityMap.put(relation, false);
        return false;
    }

    static final int DEFAULT_CORRECTION_CLOSED = 2;
    static final int REDUCED_CORRECTION_CLOSED = 1;

    public static int ClosedCorrectingFactor(int lastAddedRelation) {
        return isBidirectional(lastAddedRelation) ? DEFAULT_CORRECTION_CLOSED : REDUCED_CORRECTION_CLOSED;
    }


    static final int DEFAULT_CORRECTION_OPEN = 4;
    static final int REDUCED_CORRECTION_OPEN = 2;

    public static int OpenCorrectingFactor(int lastAddedRelation) {
        return isBidirectional(lastAddedRelation) ? DEFAULT_CORRECTION_OPEN : REDUCED_CORRECTION_OPEN;
    }

    private static ArrayList<Pair<Rule, Integer>> addClosure(Rule rule, int joinSubject, int joinObject) {
        ArrayList<Pair<Rule, Integer>> closedRules = new ArrayList<>();
        List<Integer> relations = PromisingRelations(rule);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            Rule closedRule = new Rule(rule, -1, miniAMIE.kb);

            int[] newAtom = new int[ATOM_SIZE];
            closedRule.getBody().add(newAtom);

            newAtom[SUBJECT_POSITION] = joinSubject;
            newAtom[RELATION_POSITION] = relation;
            newAtom[OBJECT_POSITION] = joinObject;

            int searchSpaceCorrectingFactor = ClosedCorrectingFactor(relation);
            closedRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));

        }
        return closedRules;
    }

    /**
     * AddClosureToEmptyBody adds a closure atom to an empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, Head object is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules.
     */
    public static ArrayList<Pair<Rule, Integer>> AddClosureToEmptyBody(Rule rule) {
        int[] headAtom = rule.getHead();
        int joinSubject = headAtom[SUBJECT_POSITION];
        int joinObject = headAtom[OBJECT_POSITION];

        return addClosure(rule, joinSubject, joinObject);
    }

    /**
     * AddClosure adds a closure atom to a non-empty body rule, with respect to perfect path pattern
     * (i.e. Head subject is closure subject, dangling's subject is closure object)
     *
     * @param rule to be closed
     * @return A set of possible closed rules paired with their correcting factor for search space size.
     */
    public static ArrayList<Pair<Rule, Integer>> AddClosureToNonEmptyBody(Rule rule) {
        int joinSubject = rule.getHead()[SUBJECT_POSITION];
        int joinObject = rule.getLastTriplePattern()[SUBJECT_POSITION];

        return addClosure(rule, joinSubject, joinObject);
    }

    private static ArrayList<Pair<Rule, Integer>> addDangling(Rule rule, int joinObject) {
        ArrayList<Pair<Rule, Integer>> openRules = new ArrayList<>();
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
            newAtom[OBJECT_POSITION] = joinObject;

            int searchSpaceCorrectingFactor = OpenCorrectingFactor(relation);
            openRules.add(new Pair<>(closedRule, searchSpaceCorrectingFactor));
        }

        return openRules;
    }

    /**
     * AddDanglingToEmptyBody adds a dangling atom to an empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Pair<Rule, Integer>> AddDanglingToEmptyBody(Rule rule) {
        int joinParameter = rule.getHead()[OBJECT_POSITION];
        return addDangling(rule, joinParameter);
    }

    /**
     * AddDangling adds a dangling atom to a non empty body rule, with respect to the perfect path pattern
     * (i.e. Only open variable is dangling's subject, Head object is dangling's object )
     *
     * @param rule parent
     * @return A set of possible open rules
     */
    public static ArrayList<Pair<Rule, Integer>> AddDanglingToNonEmptyBody(Rule rule) {
        int joinParameter = rule.getLastTriplePattern()[SUBJECT_POSITION];
        return addDangling(rule, joinParameter);
    }

    private static IntSet range(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> objectsToSubjects = kb.relation2object2subject.get(r);
        if (objectsToSubjects == null)
            return null;
        return objectsToSubjects.keySet();
    }

    private static IntSet domain(int r) {
        KB kb = (KB) miniAMIE.kb;
        Int2ObjectMap<IntSet> subjectsToObjects = kb.relation2subject2object.get(r);
        if (subjectsToObjects == null)
            return null;
        return subjectsToObjects.keySet();
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
            Int2IntMap overlaps1 = new Int2IntOpenHashMap();
            overlaps1.put(r2, overlap);

            Int2IntMap overlaps2 = new Int2IntOpenHashMap();
            overlaps2.put(r1, overlap);

            overlapTable.put(r1, overlaps1);
            overlapTable.put(r2, overlaps2);


        } else {


            overlap = factSet.getOrDefault(r2, NO_OVERLAP_VALUE);
            if (overlap == NO_OVERLAP_VALUE) {
                IntSet r1_set = triplesKeySet1.get(r1).keySet();
                IntSet r2_set = triplesKeySet2.get(r2).keySet();
                overlap = KB.computeOverlap(r1_set, r2_set);

                factSet.put(r1, overlap);
                factSet.put(r2, overlap);


            }
        }
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
        if (bodySize < 2)
            return rule.getBody();

        List<int[]> body = rule.getBody();
        List<int[]> sortedBody = new ArrayList<>();
        int[] head = rule.getHead();
        int var = head[SUBJECT_POSITION];
        for (int i = 0; i < bodySize + 1; i++) {
            for (int[] atom : body)
                if (atom[SUBJECT_POSITION] == var) {
                    sortedBody.add(atom);
                    var = atom[OBJECT_POSITION];
                    break;
                }
        }
        return sortedBody;
    }

    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     *
     * @param rule
     * @return total product operation iterating over the provided rule's body
     */
    protected static double bodyEstimate(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);


        int last_id = (int) rule.getBodySize() - 1;
        for (int id = 0; id < rule.getBodySize() - 1; id++) {
            int r_id = last_id - id;
            int r_next_id = last_id - id - 1;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rDom = domainSize(r);
            int r_nextRng = rangeSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

            double soOV = subjectToObjectOverlapSize(r, r_next);

            double factor = 0;
            double ifun_next = (double) r_nextRng / r_nextSize;
            double soSurv = soOV / rDom;
            if (ifun_next > 0) {
                factor = soSurv / ifun_next;
            }

            product *= factor;

            if (product == 0)
                break;
        }

        return product;
    }

    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1;
        if (idFirst < 0)
            System.err.println(rule);
        int idLast = 0;
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

        double inv_ifun_r1 = rFirstSize / rangeFirst;
        double inv_fun_rh = rHeadSize / domainHead;
        double ssSurv = (double) subjectToSubjectOverlap / domainLast;
        double factor1 = objectToObjectOverlap * inv_ifun_r1;
        double factor2 = ssSurv * inv_fun_rh;
        double product = factor1 * factor2;
        result = (long) (product * bodyEstimate);

        return result;
    }

    /**
     * Support approximation for an open rule
     *
     * @param rule Open rule
     * @return Support approximation
     */
    public static long ApproximateSupportOpenRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();

        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];


        double bodyEstimate = bodyEstimate(rule);
        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rangeFirst = rangeSize(rFirstBodyAtom);
        double result = 0;

        double inv_ifun_r1 = rFirstSize / rangeFirst;
        result = objectToObjectOverlap * inv_ifun_r1 * bodyEstimate;

        return (long) result;
    }


    protected static double getSizeOfHead(Rule rule) {
        return miniAMIE.kb.relationSize(rule.getHead()[RELATION_POSITION]);
    }

    public static double ApproximateHeadCoverageOpenRule(Rule rule) {
        return ApproximateSupportOpenRule(rule) / getSizeOfHead(rule);
    }

    public static double ApproximateHeadCoverageClosedRule(Rule rule) {
        return ApproximateSupportClosedRule(rule) / getSizeOfHead(rule);
    }


    public static long RealSupport(Rule rule) {
        return miniAMIE.kb.countProjection(rule.getHead(), rule.getTriples());
    }

    // RealHeadCoverage computes head coverage for a rule. /!\ Relies on pre-existing support value in rule
    public static double RealHeadCoverage(Rule rule) {
        return rule.getSupport() / getSizeOfHead(rule);
    }

    private static double altBodyEstimate(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = sortPerfectPathBody(rule);

        for (int r_id = 0; r_id < rule.getBodySize() - 1; r_id++) {

            int r_next_id = r_id + 1;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rRng = rangeSize(r);
            int r_nextDom = domainSize(r_next);
            int r_nextSize = miniAMIE.kb.relationSize(r_next);

            double ov = subjectToObjectOverlapSize(r_next, r);

            double factor = 0;
            double fun_next = (double) r_nextDom / r_nextSize;
            double soSurv = ov / rRng;
            if (fun_next > 0) {
                factor = soSurv / fun_next;
            }

            product *= factor;

            if (product == 0)
                break;
        }

        return product;
    }

    public static long AltApproximateSupportClosedRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();
        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = 0;
        if (idFirst < 0)
            System.err.println(rule);
        int idLast = bodySize - 1;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int subjectToSubjectOverlap = subjectToSubjectOverlapSize(rHead, rFirstBodyAtom);
        int objectToObjectOverlap = objectToObjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rHeadSize = miniAMIE.kb.relationSize(rHead);


        int rangeHead = rangeSize(rHead);
        int rangeLast = rangeSize(rLastBodyAtom);
        int domainFirst = domainSize(rFirstBodyAtom);

        double bodyEstimate = altBodyEstimate(rule);

        long result = 0;

        double inv_fun_r1 = rFirstSize / domainFirst;
        double inv_ifun_rh = rHeadSize / rangeHead;
        double ooSurv = (double) objectToObjectOverlap / rangeLast;
        double factor1 = subjectToSubjectOverlap * inv_fun_r1;
        double factor2 = ooSurv * inv_ifun_rh;
        double product = factor1 * factor2;
        result = (long) (product * bodyEstimate);

        return result;
    }

    public static long AltApproximateSupportOpenRule(Rule rule) {
        int rHead = rule.getHeadRelationBS();

        List<int[]> body = sortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];


        double bodyEstimate = bodyEstimate(rule);
        int objectToObjectOverlap = objectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int rFirstSize = miniAMIE.kb.relationSize(rFirstBodyAtom);
        double rangeFirst = rangeSize(rFirstBodyAtom);
        double result = 0;

        double inv_ifun_r1 = rFirstSize / rangeFirst;
        result = objectToObjectOverlap * inv_ifun_r1 * bodyEstimate;

        return (long) result;
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

    public static MiniAmieClosedRule computeClosedRuleMetrics(Rule rule) {
        MiniAmieClosedRule miniAmieClosedRule = new MiniAmieClosedRule(rule);
        long start = System.nanoTime();
        long support = RealSupport(rule);
        long time = System.nanoTime() - start;
        miniAmieClosedRule.setSupport(support);
        miniAmieClosedRule.setSupportNano(time);

        start = System.nanoTime();
        long appSupport = ApproximateSupportClosedRule(rule);
        time = System.nanoTime() - start;
        miniAmieClosedRule.setApproximateSupport(appSupport);
        miniAmieClosedRule.setAppSupportNano(time);

        miniAmieClosedRule.setHeadCoverage(RealHeadCoverage(rule));
        miniAmieClosedRule.setApproximateHC(ApproximateHeadCoverageClosedRule(rule));
        miniAmieClosedRule.setAlternativeApproximateSupport(AltApproximateSupportClosedRule(rule));

        miniAmieClosedRule.setFactorsOfApproximateSupport(new FactorsOfApproximateSupportClosedRule(rule));

        return miniAmieClosedRule;
    }

    public static List<MiniAmieClosedRule> ComputeRuleListMetrics(List<Rule> rules)
            throws InterruptedException, ExecutionException {

        List<MiniAmieClosedRule> miniAmieClosedRules = new ArrayList<>();

        if (NThreads == 1) {
            for (Rule rule : rules) {
                miniAmieClosedRules.add(computeClosedRuleMetrics(rule));
            }
        } else {
            List<Future<MiniAmieClosedRule>> miniAmieClosedRulesFutures = new ArrayList<>();
            System.out.println("Computing real support ...");
            CountDownLatch totalRulesLatch = new CountDownLatch(rules.size());
            for (Rule rule : rules) {
                miniAmieClosedRulesFutures.add(
                        executor.submit(() -> {
                            MiniAmieClosedRule miniAmieRule = computeClosedRuleMetrics(rule);
                            totalRulesLatch.countDown();
                            return miniAmieRule;
                        })
                );
            }
            totalRulesLatch.await();
            for (Future<MiniAmieClosedRule> future : miniAmieClosedRulesFutures) {
                miniAmieClosedRules.add(future.get()) ;
            }
        }

        return miniAmieClosedRules;
    }


}
