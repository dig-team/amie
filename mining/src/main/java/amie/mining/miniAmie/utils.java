package amie.mining.miniAmie;

import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.mining.assistant.MiningAssistant;
import amie.rules.PruningMetric;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.*;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static amie.data.U.decreasingKeys;
import static amie.mining.miniAmie.miniAMIE.*;

public abstract class utils {

    public static MiningAssistant miningAssistant;

    public static final int ATOM_SIZE = 3;

    public static final int UNDEFINED_POSITION = -1;
    public static final int SUBJECT_POSITION = 0;
    public static final int RELATION_POSITION = 1;
    public static final int OBJECT_POSITION = 2;

    public static final int NO_OVERLAP_VALUE = -1;
    public static final String commaSep = ",";
    public static String bodySep = ";";
    public static String atomSep = " ";
    public static final int MaxConstantsInExploration = 5;

    /**
     * It computes the PCA denominator of the provided rule
     * @param rule
     */
    public static void RealPCADenominator(MiniAmieClosedRule rule) {
        if (rule.isEmpty()) {
            return;
        }

        List<int[]> antecedent = new ArrayList<int[]>();
        antecedent.addAll(rule.getTriples().subList(1, rule.getTriples().size()));
        int[] succedent = rule.getTriples().get(0);
        double pcaDenominator = 0.0;
        int[] existentialTriple = succedent.clone();
        int freeVarPos = 0;
        int noOfHeadVars = KB.numVariables(succedent);

        if (noOfHeadVars == 1) {
            freeVarPos = KB.firstVariablePos(succedent) == 0 ? 2 : 0;
        } else {
            if (existentialTriple[0] == rule.getFunctionalVariable())
                freeVarPos = 2;
            else
                freeVarPos = 0;
        }

        existentialTriple[freeVarPos] = rule.kb.map("?x9");
        if (!antecedent.isEmpty()) {
            antecedent.add(existentialTriple);
            try {
                if (noOfHeadVars == 1) {
                    pcaDenominator = (double) rule.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) rule.kb.countDistinctPairs(succedent[0], succedent[2], antecedent);
                }
                rule.setPcaBodySize(pcaDenominator);
            } catch (UnsupportedOperationException e) {

            }
        }
    }

    /**
     * ExplorationResult is instantiated to return the result of an exploration.
     * - sumExploredRules is the approximate number of rules to be explored in the subtree
     * - finalRules is the list of rules to be kept as a result of the mining process
     */
    public static class ExplorationResult {
        int sumExploredRules;
        int sumExploredRulesAdjustedWithBidirectionality;
        List<MiniAmieClosedRule> finalRules;

        public ExplorationResult(int sumExploredRules, int sumExploredRulesAdjustedWithBidirectionality,
                                 List<MiniAmieClosedRule> finalRules) {
            this.sumExploredRules = sumExploredRules;
            this.sumExploredRulesAdjustedWithBidirectionality = sumExploredRulesAdjustedWithBidirectionality;
            this.finalRules = finalRules;
        }

    }

    public static List<Integer> SelectRelations() {
        List<Integer> relations = new ArrayList<>();

        for (int relation : Kb.getRelations()) {

            if (Kb.relationSize(relation) >= MinSup)
                relations.add(relation);
        }
        return relations;
    }


    /**
     * GetInitRules provides rules with empty bodies and head size above provided minimum support.
     *
     * @param minSup
     * @return Collection of single atom rules and collection of single atom rules with an instantiated parameter
     */
    public static Collection<MiniAmieRule> GetInitRules(double minSup) {
        Collection<Rule> initRules = miningAssistant.getInitialAtoms(minSup);
        Collection<MiniAmieRule> miniAmieInitRules = new ArrayList<>();
        for (Rule initRule : initRules) {
            miniAmieInitRules.add(new MiniAmieRule(initRule));
        }
        return miniAmieInitRules;
    }

    public static Collection<MiniAmieRule> miniAmieInitRules = new ArrayList<>();

    public static Collection<MiniAmieRule> GetInitRulesWithInstantiatedParameter(double minSup) {
        Collection<Rule> initRules = miningAssistant.getInitialAtoms(minSup);
        for (Rule initRule : initRules) {
            getInitRulesWithInstantiatedParameterFromSingleRule(initRule, OBJECT_POSITION, miniAmieInitRules);
            getInitRulesWithInstantiatedParameterFromSingleRule(initRule, SUBJECT_POSITION, miniAmieInitRules);
        }
        return miniAmieInitRules;
    }

    private static void getInitRulesWithInstantiatedParameterFromSingleRule
            (Rule initRule, int constantPosition, Collection<MiniAmieRule> instantiatedParameterInitRule) {

        int[] head = initRule.getHead();
        Int2IntMap variableInstantiations =
                Kb.countProjectionBindings(head, new ArrayList<>(), head[constantPosition]);
        IntList objectConstants = decreasingKeys(variableInstantiations);
        int nConstants = 0;
        for (int constant : objectConstants) {
            if (nConstants > MaxConstantsInExploration) break;
            nConstants++;
            if (variableInstantiations.get(constant) >= MinSup){
                int[] headClone = head.clone();
                headClone[constantPosition] = constant;
                instantiatedParameterInitRule.add(
                        new MiniAmieRule(headClone, constantPosition)
                );
            } else {
                break;
            }
        }
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


    public static int SubjectToSubjectOverlapSize(int r1, int r2) {
        KB kb = (KB) miniAMIE.Kb;
        Int2ObjectMap<Int2IntMap> overlapTable = kb.subject2subjectOverlap;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet1 = kb.relation2subject2object;
        Int2ObjectMap<Int2ObjectMap<IntSet>> triplesKeySet2 = kb.relation2subject2object;
        int overlap = overlapSize(r1, r2, overlapTable, triplesKeySet1, triplesKeySet2);
        return overlap;
    }

    public static int VariablePosition(int[] r, int variable) {
        for (int i = 0; i < r.length; i++) {
            if (r[i] == variable) {
                return i;
            }
        }
        throw new IllegalArgumentException("Variable " + variable + " not found in " + Arrays.toString(r));
    }


    public static int NextPosition(int position) {
        switch (position) {
            case SUBJECT_POSITION -> {
                return OBJECT_POSITION;
            }
            case OBJECT_POSITION -> {
                return SUBJECT_POSITION;
            }
            default -> throw new IllegalArgumentException("Invalid position " + position);
        }
    }

    /**
     * @param r1_atom
     * @param r2_atom
     * @param variablePosition_r1
     * @return Pair of overlap size and next variable position in r2 atom
     */
    public static Pair<Integer, Integer> VariableOverlapSize(int[] r1_atom, int[] r2_atom, int variablePosition_r1) {
        int overlapSize = -1;

        int variable = r1_atom[variablePosition_r1];
        int variablePosition_r2;
        try {
            variablePosition_r2 = VariablePosition(r2_atom, variable);
        } catch (Exception e) {
            throw new IllegalArgumentException("Variable position fail r1_atom " + Arrays.toString(r1_atom)
                    + " r2_atom " + Arrays.toString(r2_atom) + " variable position " + variablePosition_r1
                    , e);
        }
        int r1 = r1_atom[RELATION_POSITION];
        int r2 = r2_atom[RELATION_POSITION];

        if (variablePosition_r1 == SUBJECT_POSITION && variablePosition_r2 == SUBJECT_POSITION) {
            overlapSize = SubjectToSubjectOverlapSize(r1, r2);
        } else if (variablePosition_r1 == SUBJECT_POSITION && variablePosition_r2 == OBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r1, r2);
        } else if (variablePosition_r1 == OBJECT_POSITION && variablePosition_r2 == SUBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r2, r1);
        } else if (variablePosition_r1 == OBJECT_POSITION && variablePosition_r2 == OBJECT_POSITION) {
            overlapSize = SubjectToObjectOverlapSize(r1, r2);
        }

        if (overlapSize == -1) {
            throw new IllegalArgumentException("Couldn't compute overlap for " + variable +
                    " in " + Arrays.toString(r1_atom) + " and " + Arrays.toString(r2_atom));
        }

        return new Pair<Integer, Integer>(overlapSize, NextPosition(variablePosition_r2));
    }

    public static List<int[]> SortPerfectPathBody(MiniAmieRule rule) {
        int bodySize = rule.getBody().size();
        if (bodySize < 2) {
            return rule.getBody();
        }

        List<int[]> body = rule.getBody();
        List<int[]> mutableBody = new ArrayList<>(bodySize);
        for (int[] atom : body) {
            mutableBody.add(atom);
        }

        LinkedList<int[]> sortedBody = new LinkedList<>();
        int[] head = rule.getHead();
        int var = rule.HeadToBodyJoinVariable();

        for (int[] ignored : body) {
            for (int[] atom : mutableBody) {
                if (atom[SUBJECT_POSITION] == var) {
                    sortedBody.addFirst(atom);
                    mutableBody.remove(atom);
                    var = atom[OBJECT_POSITION];
                    break;
                } else if (atom[OBJECT_POSITION] == var) {
                    sortedBody.addFirst(atom);
                    var = atom[SUBJECT_POSITION];
                    mutableBody.remove(atom);
                    break;
                }
            }
        }
        if (body.size() != sortedBody.size()) {
            throw new IllegalArgumentException("Couldn't sort body " + RawBodyHeadToString(body, head));
        }
        return sortedBody;
    }

    public static String RawBodyHeadToString(List<int[]> body, int[] head) {
        String ruleStr = "";
        for (int[] atom : body) {
            ruleStr += Arrays.toString(atom) + " ";
        }
        return ruleStr + "=> " + Arrays.toString(head);
    }

    protected static double getSizeOfHead(Rule rule) {
        return Kb.relationSize(rule.getHead()[RELATION_POSITION]);
    }


    public static long RealSupport(Rule rule) {
        return Kb.countProjection(rule.getHead(), rule.getTriples());
    }

    // RealHeadCoverage computes head coverage for a rule. /!\ Relies on pre-existing support value in rule
    public static double RealHeadCoverage(Rule rule) {
        return rule.getSupport() / getSizeOfHead(rule);
    }


    public static List<MiniAmieClosedRule> ComputeRuleListMetrics(List<MiniAmieClosedRule> rules)
            throws InterruptedException //, ExecutionException
    {

        //List<MiniAmieClosedRule> miniAmieClosedRules = new ArrayList<>();
        if (miniAMIE.shouldComputeRealMetricsAfterMining()) {
            System.out.println("Computing real support and PCA confidence ...");
        }

        if (NThreads == 1) {
            for (MiniAmieClosedRule rule : rules) {
                rule.ComputeClosedRuleMetrics(miniAMIE.shouldComputeRealMetricsAfterMining()) ;
            }
        } else {
            //List<Future<MiniAmieClosedRule>> miniAmieClosedRulesFutures = new ArrayList<>();
            CountDownLatch totalRulesLatch = new CountDownLatch(rules.size());
            for (MiniAmieClosedRule rule: rules) {
                //miniAmieClosedRulesFutures.add(
                        executor.submit(() -> {
                            rule.ComputeClosedRuleMetrics(miniAMIE.shouldComputeRealMetricsAfterMining()) ;
                            totalRulesLatch.countDown();
                            return rule;
                        });
                //);
            }

            totalRulesLatch.await();
            //for (Future<MiniAmieClosedRule> future : miniAmieClosedRulesFutures) {
                //future.get();
                //miniAmieClosedRules.add(future.get());
            //}
        }

        //return miniAmieClosedRules;
        return rules;
    }

    public interface selectivityMethod {
        double selectivity(int[] atom1, int[] atom2, int joinVariable) ;
    }

    static final int SIZE_1_POSITION = 0 ;
    static final int SIZE_2_POSITION = 1 ;
    static final int OVERLAP_POSITION = 2 ;

    private static double[] selectivityElements(int[] atom1, int[] atom2, int joinVariable) {

        int r1 = atom1[RELATION_POSITION];
        int r2 = atom2[RELATION_POSITION];

        int position1 = VariablePosition(atom1, joinVariable);
        int position2 = VariablePosition(atom2, joinVariable);

        double overlap ;
        double size1 ;
        double size2 ;

        if (position1 == SUBJECT_POSITION && position2 == OBJECT_POSITION) {
            overlap = SubjectToObjectOverlapSize(r1, r2) ;
            size1 = DomainSize(r1) ;
            size2 = RangeSize(r2) ;
        } else if (position1 == OBJECT_POSITION && position2 == SUBJECT_POSITION) {
            overlap = SubjectToObjectOverlapSize(r2, r1) ;
            size1 = RangeSize(r1) ;
            size2 = DomainSize(r2) ;
        } else if (position1 == SUBJECT_POSITION && position2 == SUBJECT_POSITION) {
            overlap = SubjectToSubjectOverlapSize(r1, r2) ;
            size1 = DomainSize(r1) ;
            size2 = DomainSize(r2) ;
        } else if (position1 == OBJECT_POSITION && position2 == OBJECT_POSITION) {
            overlap = ObjectToObjectOverlapSize(r1, r2) ;
            size1 = RangeSize(r1) ;
            size2 = RangeSize(r2) ;
        } else {
            throw new IllegalArgumentException("Invalid positions in selectivity position1 " + position1
                    + " position2 " + position2) ;
        }
        return new double[]{size1, size2, overlap};
    }

    public static class AvgSelectivity implements selectivityMethod {

        @Override
        public double selectivity(int[] atom1, int[] atom2, int joinVariable) {
            double[] selectivityElements = selectivityElements(atom1, atom2, joinVariable);
            double size1 = selectivityElements[SIZE_1_POSITION];
            double size2 = selectivityElements[SIZE_2_POSITION];
            double overlap = selectivityElements[OVERLAP_POSITION];

            double denom = 2 * size1 * size2 ;
            if (denom <= 0) {
                return 0.0;
            }

            double nom = overlap * ( size1 + size2 ) ;
            return nom / denom ;
        }
    }

    public static class JacquardSelectivity implements selectivityMethod {

        @Override
        public double selectivity(int[] atom1, int[] atom2, int joinVariable) {
            double[] selectivityElements = selectivityElements(atom1, atom2, joinVariable);
            double size1 = selectivityElements[SIZE_1_POSITION];
            double size2 = selectivityElements[SIZE_2_POSITION];
            double overlap = selectivityElements[OVERLAP_POSITION];
            double denom = size1 + size2 - overlap;
            if (denom <= 0) {
                return 0.0;
            }
            return overlap / denom ;
        }
    }

    public static class SurvivalRateSelectivity implements selectivityMethod {
        @Override
        public double selectivity(int[] atom1, int[] atom2, int joinVariable) {
            int r1 = atom1[RELATION_POSITION];
            int r2 = atom2[RELATION_POSITION];

            int position1 = VariablePosition(atom1, joinVariable);
            int position2 = VariablePosition(atom2, joinVariable);

            double overlap ;
            double denom ;

            if (position1 == SUBJECT_POSITION && position2 == OBJECT_POSITION) {
                overlap = SubjectToObjectOverlapSize(r1, r2) ;
                denom = DomainSize(r1) ;
            } else if (position1 == OBJECT_POSITION && position2 == SUBJECT_POSITION) {
                overlap = SubjectToObjectOverlapSize(r2, r1) ;
                denom = RangeSize(r1) ;
            } else if (position1 == SUBJECT_POSITION && position2 == SUBJECT_POSITION) {
                overlap = SubjectToSubjectOverlapSize(r1, r2) ;
                denom = DomainSize(r1) ;
            } else if (position1 == OBJECT_POSITION && position2 == OBJECT_POSITION) {
                overlap = ObjectToObjectOverlapSize(r1, r2) ;
                denom = RangeSize(r1) ;
            } else {
                throw new IllegalArgumentException("Invalid positions in selectivity position1 " + position1
                        + " position2 " + position2) ;
            }

            if (denom <= 0) {
                return 0.0;
            }
            return overlap / denom ;
        }
    }



}
