package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.MiningAssistant;
import amie.mining.miniAmie.output.Attributes;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.mining.miniAmie.miniAMIE.*;

public abstract class utils {

    public static MiningAssistant miningAssistant;

    public static final int ATOM_SIZE = 3;

    public static final int UNDEFINED_POSITION = -1 ;
    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1;
    public static final String commaSep = ",";
    public static String bodySep = ";" ;
    public static String atomSep = " " ;

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

        for (int relation : Kb.getRelations()) {

            if (Kb.relationSize(relation) >= MinSup)
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
        double bidirectionalityJaccard = (double) SubjectToObjectOverlapSize(relation, relation)
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

    private static IntSet range(int r) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<IntSet> objectsToSubjects = kb.relation2object2subject.get(r);
        if (objectsToSubjects == null)
            return null;
        return objectsToSubjects.keySet();
    }

    private static IntSet domain(int r) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<IntSet> subjectsToObjects = kb.relation2subject2object.get(r);
        if (subjectsToObjects == null)
            return null;
        return subjectsToObjects.keySet();
    }

    public static int RangeSize(int r) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<IntSet> factSet = kb.relation2object2subject.get(r);
        if (factSet == null)
            return 0;
        return factSet.size();
    }

    public static int DomainSize(int r) {
        KB kb = (KB) miniAMIE.Kb;
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

    public static int ObjectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.object2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2object2subject;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    public static int SubjectToObjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2objectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2object2subject;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }


    static int subjectToSubjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    public static int VariablePosition(int[] r, int variable) {
        int pos = UNDEFINED_POSITION ;
        for(int i = 0 ; i < r.length ; i++) {
            if (r[i] == variable) {
                pos = i;
                break;
            }
        }
        if (pos == UNDEFINED_POSITION) {
            throw new IllegalArgumentException("Variable " + variable + " not found in " + Arrays.toString(r));
        }
        return pos;
    }


    public static int nextPosition(int position) {
        switch (position) {
            case SUBJECT_POSITION -> {
                return OBJECT_POSITION ;
            }
            case OBJECT_POSITION -> {
                return SUBJECT_POSITION ;
            }
            default -> throw new IllegalArgumentException("Invalid position " + position);
        }
    }

    public static int VariableSetSize(int position, int relation) {
        switch (position) {
            case SUBJECT_POSITION -> {
                return DomainSize(relation) ;
            }
            case OBJECT_POSITION -> {
                return RangeSize(relation) ;
            }
            default -> throw new IllegalArgumentException("Invalid position " + position + " in relation " + relation);
        }
    }

    /**
     *
     * @param r1_atom
     * @param r2_atom
     * @param variablePosition_r1
     * @return Pair of overlap size and next variable position in r2 atom
     */
    public static Pair<Integer, Integer> VariableOverlapSize(int[] r1_atom, int[] r2_atom, int variablePosition_r1) {
        int overlapSize = -1 ;

        int variable = r1_atom[variablePosition_r1] ;
        int variablePosition_r2 ;
        try {
            variablePosition_r2 = VariablePosition(r2_atom, variable);
        } catch (Exception e) {
            throw new IllegalArgumentException("Variable position fail r1_atom "+ Arrays.toString(r1_atom)
                    + " r2_atom " + Arrays.toString(r2_atom) + " variable position " + variablePosition_r1
                    , e) ;
        }
        int r1 = r1_atom[RELATION_POSITION] ;
        int r2 = r2_atom[RELATION_POSITION] ;

        if (variablePosition_r1 == SUBJECT_POSITION && variablePosition_r2 == SUBJECT_POSITION) {
            overlapSize = subjectToSubjectOverlapSize(r1, r2);
        }
        else if (variablePosition_r1 == SUBJECT_POSITION && variablePosition_r2 == OBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r1, r2);
        }
        else if (variablePosition_r1 == OBJECT_POSITION && variablePosition_r2 == SUBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r2, r1) ;
        }
        else if (variablePosition_r1 == OBJECT_POSITION && variablePosition_r2 == OBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r1, r2) ;
        }

        if (overlapSize == -1) {
            throw new IllegalArgumentException("Couldn't compute overlap for " + variable +
                    " in " + Arrays.toString(r1_atom) + " and " + Arrays.toString(r2_atom));
        }

        return new Pair<Integer, Integer>(overlapSize, nextPosition(variablePosition_r2));
    }

    public static List<int[]> SortPerfectPathBody(Rule rule) {
        int bodySize = rule.getBody().size();
        if (bodySize < 2) {
            return rule.getBody();
        }

        List<int[]> body = rule.getBody();
        List<int[]> mutableBody = new ArrayList<>(bodySize) ;
        for (int[] atom: body){
            mutableBody.add(atom) ;
        }

        LinkedList<int[]> sortedBody = new LinkedList<>();
        int[] head = rule.getHead();
        int var = head[OBJECT_POSITION];

        for (int[] ignored : body) {
            for (int[] atom : mutableBody) {
                if (atom[SUBJECT_POSITION] == var) {
                    sortedBody.addFirst(atom);
                    mutableBody.remove(atom) ;
                    var = atom[OBJECT_POSITION];
                    break ;
                } else if (atom[OBJECT_POSITION] == var) {
                    sortedBody.addFirst(atom);
                    var = atom[SUBJECT_POSITION];
                    mutableBody.remove(atom) ;
                    break ;
                }
            }
        }
        if (body.size() != sortedBody.size()) {
            throw new IllegalArgumentException("Couldn't sort body " + RawBodyHeadToString(body, head));
        }
        return sortedBody;
    }

    public static double AverageParameterRatio(int relation, int destinationSetPosition) {
        switch (destinationSetPosition) {
            case OBJECT_POSITION -> {
                // functionality
                double domain = DomainSize(relation) ;
                if (domain > 0)
                    return Kb.relationSize(relation) / domain ;
                else
                    return 0 ;
            }
            case SUBJECT_POSITION -> {
                // inverse functionality
                double range = RangeSize(relation) ;
                if (range > 0)
                    return Kb.relationSize(relation) / range ;
                else
                    return 0 ;
            }
            default -> throw new IllegalArgumentException("Invalid position " + destinationSetPosition +
                    " for relation " + relation);
        }
    }

    public static int GetInitPositionInBody(Rule rule) {
        List<int[]> body = SortPerfectPathBody(rule);
        int last_id = body.size() - 1;
        int[] lastAtom = body.get(last_id) ;
        int headObject = rule.getHead()[OBJECT_POSITION];
        int headObjectPositionInLastAtom = VariablePosition(lastAtom, headObject) ;
        return nextPosition(headObjectPositionInLastAtom);
    }

    /**
     * bodyEstimate computes the total product operation for estimating support of a rule
     *
     * @param rule rule to compute body approximation
     * @param initVariablePosition indicates the position of the starting variable in the first atom.
     * @return total product operation iterating over the provided rule's body
     */
    // todo replace Rule rule param by sorted body
    public static double BodyEstimate(Rule rule, int initVariablePosition) {
        double product = 1;
        List<int[]> body = SortPerfectPathBody(rule);
        int last_id = body.size() - 1;
        int[] lastAtom = body.get(last_id) ;
        int headObject = rule.getHead()[OBJECT_POSITION];
        int headObjectPositionInLastAtom = VariablePosition(lastAtom, headObject) ;
        int variablePosition = nextPosition(headObjectPositionInLastAtom);
        for (int id = 0; id < body.size() - 1; id++) {
            int atom_id = body.size() - id - 1;
            int atom_next_id = atom_id - 1 ;
            int[] atom = body.get(atom_id);
            int[] atom_next = body.get(atom_next_id);

            Pair<Integer, Integer> variableOverlapSizeResult = null;
            try {
                variableOverlapSizeResult =
                        VariableOverlapSize(atom, atom_next, variablePosition);
            } catch (Exception e) {
                throw new IllegalArgumentException("body estimate fail id " + id + " rule "
                            + RawBodyHeadToString(body, rule.getHead())
                            + " lastAtom " + Arrays.toString(lastAtom)
                        + " headObject " + headObject
                        + " headObjectPositionInLastAtom " + headObjectPositionInLastAtom
                        + " variablePosition " + variablePosition
                        + " unsorted rule " + RawBodyHeadToString(rule.getBody(), rule.getHead()) +
                            " body size " + body.size() + " rule body size "+ rule.getBodySize(), e) ;
            }
            int variableOverlap = variableOverlapSizeResult.first ;
            variablePosition = variableOverlapSizeResult.second ;

            int r = atom[RELATION_POSITION];
            int r_next = atom_next[RELATION_POSITION];
            int variableSetSize = VariableSetSize(variablePosition, r);

            double survRate = variableOverlap / variableSetSize;
            double nAvg = AverageParameterRatio(r_next, variablePosition) ;
            double factor = survRate * nAvg ;

            product *= factor;

            if (product == 0)
                break;
        }

        return product;
    }

    public static String RawBodyHeadToString(List<int[]> body, int[] head) {
        String ruleStr = "";
        for (int[] atom : body) {
            ruleStr += Arrays.toString(atom)+" " ;
        }
        return ruleStr + "=> "+Arrays.toString(head);
    }

    /**
     * Support approximation for an open rule
     *
     * @param rule Open rule
     * @return Support approximation
     */
    public static long ApproximateSupportOpenRule(Rule rule) {
        if (rule.getBody().isEmpty())
            return 0;

        int[] headAtom = rule.getHead() ;
        int headObject = headAtom[OBJECT_POSITION] ;
        List<int[]> body = SortPerfectPathBody(rule);
        int bodySize = body.size();

        int headRelation = headAtom[RELATION_POSITION] ;

        //// Opening
        int[] firstBodyAtom = body.get(bodySize - 1);
        int subjectPositionInFirstBodyAtom ;

        try {
            subjectPositionInFirstBodyAtom = VariablePosition(firstBodyAtom, headObject);
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to find init variable " +
                    headObject + " position in " + RawBodyHeadToString(body, headAtom) +
                    " firstBodyAtom " + Arrays.toString(firstBodyAtom), e) ;
        }

        int firstBodyRelation = firstBodyAtom[RELATION_POSITION];
        int openingOverlap;
        switch (subjectPositionInFirstBodyAtom) {
            case SUBJECT_POSITION ->
                    openingOverlap = ObjectToObjectOverlapSize(headRelation, firstBodyRelation) ;
            case OBJECT_POSITION ->
                    openingOverlap = SubjectToObjectOverlapSize(firstBodyRelation, headRelation) ;
            default -> throw new IllegalArgumentException("Invalid position " + subjectPositionInFirstBodyAtom
                    + " in first body atom " + Arrays.toString(firstBodyAtom) + " of rule " + rule);
        }
        double nAvgFirst = AverageParameterRatio(firstBodyRelation, nextPosition(subjectPositionInFirstBodyAtom)) ;

        // -------------------------------------
        // Body
        double bodyEstimate = BodyEstimate(rule, nextPosition(subjectPositionInFirstBodyAtom));

        return (long) (openingOverlap * nAvgFirst * bodyEstimate);
    }

    /**
     * Support approximation for a closed rule
     *
     * @param rule Closed rule
     * @return Support approximation
     */
    public static long ApproximateSupportClosedRule(Rule rule) {
        long approximateSupportOpen = ApproximateSupportOpenRule(rule) ;
        if (approximateSupportOpen == 0)
            return 0;

        int[] headAtom = rule.getHead() ;
        int headSubject = headAtom[SUBJECT_POSITION] ;
        List<int[]> body = SortPerfectPathBody(rule);
        // Closed rule factor (closing)

        ///// Closing
        int headRelation = headAtom[RELATION_POSITION] ;
        int idLast = 0;
        int[] lastBodyAtom =  body.get(idLast) ;
        int lastBodyRelation = lastBodyAtom[RELATION_POSITION] ;
        double closingSurvivalRate ;

        int lastVariablePosition ;
        try {
            lastVariablePosition = VariablePosition(lastBodyAtom, headSubject) ;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to find last variable "+
                    headSubject + " position in " + RawBodyHeadToString(body, rule.getHead())
                    + " last " + Arrays.toString(body.get(idLast)), e) ;
        }
        switch (lastVariablePosition) {
            case SUBJECT_POSITION -> {
                double lastBodyRelationDomainSize = DomainSize(lastBodyRelation);
                if (lastBodyRelationDomainSize > 0)
                    closingSurvivalRate = SubjectToObjectOverlapSize(lastBodyRelation, headRelation)
                            / lastBodyRelationDomainSize;
                else
                    closingSurvivalRate = 0;
            }
            case OBJECT_POSITION -> {
                double lastBodyRelationRangeSize = RangeSize(lastBodyRelation);
                if (lastBodyRelationRangeSize > 0)
                    closingSurvivalRate = SubjectToObjectOverlapSize(headRelation, lastBodyRelation)
                            / lastBodyRelationRangeSize;
                else
                    closingSurvivalRate = 0;
            }
            default -> throw new IllegalArgumentException("Invalid position " + lastVariablePosition
                    + " in last body atom " + Arrays.toString(lastBodyAtom) + " of rule " + rule);
        }
        double nAvgHead = AverageParameterRatio(headRelation, SUBJECT_POSITION) ;

        return (long) (approximateSupportOpen * closingSurvivalRate * nAvgHead);
    }




    protected static double getSizeOfHead(Rule rule) {
        return Kb.relationSize(rule.getHead()[RELATION_POSITION]);
    }

    public static double ApproximateHeadCoverageOpenRule(Rule rule) {
        return ApproximateSupportOpenRule(rule) / getSizeOfHead(rule);
    }

    public static double ApproximateHeadCoverageClosedRule(Rule rule) {
        return ApproximateSupportClosedRule(rule) / getSizeOfHead(rule);
    }


    public static long RealSupport(Rule rule) {
        return Kb.countProjection(rule.getHead(), rule.getTriples());
    }

    // RealHeadCoverage computes head coverage for a rule. /!\ Relies on pre-existing support value in rule
    public static double RealHeadCoverage(Rule rule) {
        return rule.getSupport() / getSizeOfHead(rule);
    }

    private static double altBodyEstimate(Rule rule) {
        double product = 1;
        rule.setBodySize(rule.getBody().size());
        List<int[]> body = SortPerfectPathBody(rule);

        for (int r_id = 0; r_id < body.size() - 1; r_id++) {

            int r_next_id = r_id + 1;
            int r = body.get(r_id)[RELATION_POSITION];
            int r_next = body.get(r_next_id)[RELATION_POSITION];
            // Computing SO Survival rate
            int rRng = RangeSize(r);
            int r_nextDom = DomainSize(r_next);
            int r_nextSize = Kb.relationSize(r_next);

            double ov = SubjectToObjectOverlapSize(r_next, r);

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
        List<int[]> body = SortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = 0;
        if (idFirst < 0)
            System.err.println(rule);
        int idLast = bodySize - 1;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];
        int rLastBodyAtom = body.get(idLast)[RELATION_POSITION];


        int subjectToSubjectOverlap = subjectToSubjectOverlapSize(rHead, rFirstBodyAtom);
        int objectToObjectOverlap = ObjectToObjectOverlapSize(rLastBodyAtom, rHead);
        double rFirstSize = Kb.relationSize(rFirstBodyAtom);
        double rHeadSize = Kb.relationSize(rHead);


        int rangeHead = RangeSize(rHead);
        int rangeLast = RangeSize(rLastBodyAtom);
        int domainFirst = DomainSize(rFirstBodyAtom);

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

        List<int[]> body = SortPerfectPathBody(rule);
        int bodySize = body.size();
        int idFirst = bodySize - 1;
        int rFirstBodyAtom = body.get(idFirst)[RELATION_POSITION];


        double bodyEstimate = BodyEstimate(rule, SUBJECT_POSITION);
        int objectToObjectOverlap = ObjectToObjectOverlapSize(rHead, rFirstBodyAtom);
        int rFirstSize = Kb.relationSize(rFirstBodyAtom);
        double rangeFirst = RangeSize(rFirstBodyAtom);
        double result = 0;

        double inv_ifun_r1 = rFirstSize / rangeFirst;
        result = objectToObjectOverlap * inv_ifun_r1 * bodyEstimate;

        return (long) result;
    }

    /**
     * GetInitRules provides a sample of rules with empty
     * bodies and head coverage above provided minimum support.
     * @param minSup
     * @return Collection of single atom rules
     */
    public static Collection<MiniAmieRule> GetInitRules(double minSup) {
        Collection<Rule> initRules = miningAssistant.getInitialAtoms(minSup) ;
        Collection<MiniAmieRule> miniAmieInitRules = new ArrayList<>() ;
        for (Rule initRule: initRules)
            miniAmieInitRules.add(new MiniAmieRule(initRule)) ;
        return miniAmieInitRules;
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

        miniAmieClosedRule.setFactorsOfApproximateSupport(
                new Attributes(rule)
        );

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
