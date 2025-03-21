package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.KB;
import amie.rules.PruningMetric;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static amie.data.U.decreasingKeys;
import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;


public class MiniAmieRule extends Rule {


    private int lastOpenParameter;
    private int correctingFactor;
    private double approximateSupport = -1;
    private long supportNano;
    private long appSupportNano;



    boolean hasAcyclicInstantiatedParameterInHead = false;
    int instantiatedParameterPositionInHead = UNDEFINED_POSITION;

    private List<int[]> sortedBody ;
    private double approximateHC = -1;


    public MiniAmieRule(Rule rule) {
        super(rule, rule.getSupport(), rule.kb);
        this.setLastOpenParameter(rule.getHead()[OBJECT_POSITION]);

        sortedBody = utils.SortPerfectPathBody(this) ;
    }


    /**
     * Appends new atom to clone
     *
     * @param rule
     * @param subject
     * @param relation
     * @param object
     * @return clone of the given rule with a new atom
     */
    public MiniAmieRule(MiniAmieRule rule,
                        int subject,
                        int relation,
                        int object,
                        int newOpenParameter) {
        super(rule, rule.getSupport(), rule.kb);
        hasAcyclicInstantiatedParameterInHead = rule.hasAcyclicInstantiatedParameterInHead;
        instantiatedParameterPositionInHead = rule.instantiatedParameterPositionInHead;
        this.setLastOpenParameter(newOpenParameter);

        int[] newAtom = new int[ATOM_SIZE];
        this.getBody().add(newAtom);

        newAtom[SUBJECT_POSITION] = subject;
        newAtom[RELATION_POSITION] = relation;
        newAtom[OBJECT_POSITION] = object;

        setCorrectingFactor(OpenCorrectingFactor(relation));

        sortedBody = utils.SortPerfectPathBody(this) ;
    }

    public MiniAmieRule(int[] head) {
        super(head, -1, Kb);
    }

    public MiniAmieRule(int[] head, int instantiatedParameterPositionInHead) {
        super(head, -1, Kb);
        this.hasAcyclicInstantiatedParameterInHead = true;
        this.instantiatedParameterPositionInHead = instantiatedParameterPositionInHead;
        setLastOpenParameter(
                head[utils.NextPosition(
                        instantiatedParameterPositionInHead)]
                );
    }

    public MiniAmieRule(MiniAmieRule rule, double support, AbstractKB kb) {
        super(rule, support, kb);
    }

    protected void InheritAttributes(MiniAmieRule rule) {
        lastOpenParameter = rule.lastOpenParameter;
        hasAcyclicInstantiatedParameterInHead = rule.hasAcyclicInstantiatedParameterInHead;
        instantiatedParameterPositionInHead = rule.instantiatedParameterPositionInHead;
    }


    public int getLastOpenParameter() {
        return lastOpenParameter;
    }

    public void setLastOpenParameter(int lastOpenParameter) {
        this.lastOpenParameter = lastOpenParameter;
    }

    public int getCorrectingFactor() {
        return correctingFactor;
    }

    public void setCorrectingFactor(int correctingFactor) {
        this.correctingFactor = correctingFactor;
    }

    public ArrayList<MiniAmieRule> AddDangling() {
        ArrayList<MiniAmieRule> openRules = new ArrayList<>();
        int[] lastTriplePattern = this.getLastTriplePattern();
        int joinPositionInRule = lastTriplePattern[0] == lastOpenParameter? SUBJECT_POSITION : OBJECT_POSITION;
        List<Integer> relations = this.getRealLength() <= 1 ?  this.promisingRelations() :
                this.promisingRelationsFromOverlapTables(lastTriplePattern[0], joinPositionInRule, OBJECT_POSITION);
        int unboundParameter = this.newVariable();

        if (relations.isEmpty()) {
            return null;
        }
        for (int relation : relations) {
            MiniAmieRule openRule = new MiniAmieRule(this,
                    unboundParameter, relation, lastOpenParameter, unboundParameter);

            long start = System.nanoTime();
            openRule.setApproximateSupport(openRule.ComputeSupportApproximation());
            long time = System.nanoTime() - start;
            openRule.setAppSupportNano(time);

            if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                start = System.nanoTime();
                openRule.setSupport(RealSupport(this));
                openRule.setHeadCoverage(RealHeadCoverage(this));
                time = System.nanoTime() - start;
                openRule.setSupportNano(time);
            }

            if (openRule.IsNotPruned())
                openRules.add(openRule);
        }

        relations = this.getRealLength() <= 1 ?  this.promisingRelations() :
                    this.promisingRelationsFromOverlapTables(lastTriplePattern[0], joinPositionInRule, SUBJECT_POSITION);
        for (int relation : relations) {
                // Reversing unbound parameter and join object
            MiniAmieRule openRuleAlt = new MiniAmieRule(this,
                    lastOpenParameter, relation, unboundParameter, unboundParameter);
            long start = System.nanoTime();
            openRuleAlt.setApproximateSupport(openRuleAlt.ComputeSupportApproximation());
            long time = System.nanoTime() - start;
            openRuleAlt.setAppSupportNano(time);

            if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                start = System.nanoTime();
                openRuleAlt.setSupport(RealSupport(this));
                openRuleAlt.setHeadCoverage(RealHeadCoverage(this));
                time = System.nanoTime() - start;
                openRuleAlt.setSupportNano(time);
            }

            if (openRuleAlt.IsNotPruned())
                openRules.add(openRuleAlt);
        }
        return openRules;
    }

    private List<Integer> promisingRelationsFromOverlapTables(int predId, int position1, int position2) {
        List<Integer> promisingPredicates = new ArrayList<>();
        if (position1 == SUBJECT_POSITION) { // The subject is variable
            if (position2 == SUBJECT_POSITION)
                promisingPredicates.addAll(((KB)(this.kb)).subject2subjectOverlap.get(predId).keySet());
            else
                promisingPredicates.addAll(((KB)(this.kb)).subject2objectOverlap.get(predId).keySet());
        } else {
            if (position2 == SUBJECT_POSITION)
                // Here we have a problem
                return this.promisingRelations();
            else
                promisingPredicates.addAll(((KB)(this.kb)).object2objectOverlap.get(predId).keySet());
        }

        return promisingPredicates;
    }




    public double getApproximateHC() {
        return approximateHC;
    }

    public void setApproximateHC(double approximateHC) {
        this.approximateHC = approximateHC;
    }

    public double getApproximateSupport() {
        return approximateSupport;
    }

    public void setApproximateSupport(double approximateSupport) {
        this.approximateSupport = approximateSupport;
    }


    public long getSupportNano() {
        return supportNano;
    }

    public void setSupportNano(long supportNano) {
        this.supportNano = supportNano;
    }

    public long getAppSupportNano() {
        return appSupportNano;
    }

    public void setAppSupportNano(long appSupportNano) {
        this.appSupportNano = appSupportNano;
    }


    /**
     * Note: only useful for rules of size two.
     * @return
     */
    public ArrayList<MiniAmieClosedRule> AddClosureToAcyclicWithConstants(int maxConstants) {
        int unboundParameter = this.getLastOpenParameter();
        int[] lastTriplePattern = this.getLastTriplePattern();
        int joinPositionInRule = lastTriplePattern[0] == lastOpenParameter? SUBJECT_POSITION : OBJECT_POSITION;

        ArrayList<MiniAmieClosedRule> closedRules = new ArrayList<>();
        List<Integer> relations = this.getRealLength() <= 1 ?  this.promisingRelations() :
                this.promisingRelationsFromOverlapTables(lastTriplePattern[1], joinPositionInRule, OBJECT_POSITION);

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            // Instantiated rule
            List<int[]> newAtomQuery = new ArrayList<>();
            int[] newAtom = new int[]{unboundParameter, relation, lastOpenParameter};
            newAtomQuery.add(newAtom);
            // TODO reuse previously instantiated heads if possible
            IntList objectConstants = decreasingKeys(kb.countProjectionBindings(newAtom, Collections.EMPTY_LIST, lastOpenParameter));
            //IntSet objectConstants = Kb.selectDistinct(lastOpenParameter, newAtomQuery);
            int k = 0;
            for (int constant : objectConstants) {
                if (k >= maxConstants) break;
                k++;
                MiniAmieClosedRule closedRule = new MiniAmieClosedRule(this,
                        unboundParameter, relation, constant);
                long start = System.nanoTime();
                closedRule.setApproximateSupport(closedRule.ComputeSupportApproximation());
                long time = System.nanoTime() - start;
                closedRule.setAppSupportNano(time);

                if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                    start = System.nanoTime();
                    closedRule.setSupport(RealSupport(this));
                    closedRule.setHeadCoverage(RealHeadCoverage(this));
                    time = System.nanoTime() - start;
                    closedRule.setSupportNano(time);
                }

                if (closedRule.IsNotPruned())
                    closedRules.add(closedRule);
            }
        }
        relations = this.getRealLength() <= 1 ?  this.promisingRelations() :
                this.promisingRelationsFromOverlapTables(lastTriplePattern[1], joinPositionInRule, SUBJECT_POSITION);

        for (int relation: relations){
            // Reversing join subject and constant order
            List<int[]> newAtomQueryAlt = new ArrayList<>();
            int[] newAtomAlt = new int[]{lastOpenParameter, relation, unboundParameter} ;
            newAtomQueryAlt.add(newAtomAlt);
            // TODO reuse previously instantiated heads if possible
            IntList objectConstantsAlt = decreasingKeys(kb.countProjectionBindings(newAtomAlt, Collections.EMPTY_LIST, lastOpenParameter));
            //IntSet objectConstantsAlt = Kb.selectDistinct(lastOpenParameter, newAtomQueryAlt);
            for (int constant : objectConstantsAlt) {
                MiniAmieClosedRule closedRule = new MiniAmieClosedRule(this,
                        constant, relation, unboundParameter);
                long start = System.nanoTime();
                closedRule.setApproximateSupport(closedRule.ComputeSupportApproximation());
                long time = System.nanoTime() - start;
                closedRule.setAppSupportNano(time);

                if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                    start = System.nanoTime();
                    closedRule.setSupport(RealSupport(this));
                    closedRule.setHeadCoverage(RealHeadCoverage(this));
                    time = System.nanoTime() - start;
                    closedRule.setSupportNano(time);
                }

                if (closedRule.IsNotPruned())
                    closedRules.add(closedRule);
            }
        }

        return closedRules;
    }


    public ArrayList<MiniAmieClosedRule> AddClosure(int maxConstantsInExploration) {
        if (hasAcyclicInstantiatedParameterInHead) {
            return AddClosureToAcyclicWithConstants(maxConstantsInExploration);
        }
        int[] headAtom = this.getHead();

        int joinSubject = headAtom[SUBJECT_POSITION];
        int joinObject = this.lastOpenParameter;

        ArrayList<MiniAmieClosedRule> closedRules = new ArrayList<>();
        List<Integer> relations = this.promisingRelations();

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            MiniAmieClosedRule closedRule = new MiniAmieClosedRule(this,
                    joinSubject, relation, joinObject);
            long start = System.nanoTime();
            closedRule.setApproximateSupport(closedRule.ComputeSupportApproximation());
            long time = System.nanoTime() - start;
            closedRule.setAppSupportNano(time);

            if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                start = System.nanoTime();
                closedRule.setSupport(RealSupport(this));
                closedRule.setHeadCoverage(RealHeadCoverage(this));
                time = System.nanoTime() - start;
                closedRule.setSupportNano(time);
            }

            if (closedRule.IsNotPruned())
                closedRules.add(closedRule);

            // Reversing joinSubject et joinObject
            MiniAmieClosedRule closedRuleAlt = new MiniAmieClosedRule(this,
                    joinObject, relation, joinSubject);
            start = System.nanoTime();
            closedRuleAlt.setApproximateSupport(closedRuleAlt.ComputeSupportApproximation());
            time = System.nanoTime() - start;
            closedRuleAlt.setAppSupportNano(time);

            if (miniAMIE.PM == PruningMetric.Support || miniAMIE.PM == PruningMetric.HeadCoverage) {
                start = System.nanoTime();
                closedRuleAlt.setSupport(RealSupport(this));
                closedRuleAlt.setHeadCoverage(RealHeadCoverage(this));
                time = System.nanoTime() - start;
                closedRuleAlt.setSupportNano(time);
            }

            if (closedRuleAlt.IsNotPruned())
                closedRules.add(closedRuleAlt);
        }

        return closedRules;
    }


    public boolean ExceedsLengthWhenClosing() {
        return this.getBody().size() + 2 > miniAMIE.MaxRuleSize;
    }

    public boolean ExceedsLengthWhenOpening() {
        return this.getBody().size() + 3 > miniAMIE.MaxRuleSize;
    }

    // TODO replace that with IsPrunedOpenRule static method attribute to avoid repeated PruningMetric check
    public boolean IsNotPruned() {
            switch (PM) {
                case ApproximateSupport -> {
                    return this.approximateSupport >= MinSup;
                }
                case ApproximateHeadCoverage -> {
                    return this.HeadCoverageApproximation() >= MinHC;
                }
                case Support -> {
                    return this.getSupport() >= MinSup;
                }
                case HeadCoverage -> {
                    return this.getHeadCoverage() >= MinHC;
                }
                default -> {
                    return false;
                }
            }
    }

    private boolean doesNotContainsRelation(int relation) {

        if (relation == this.getHead()[RELATION_POSITION])
            return false;

        // Seeking unwanted duplicates
        for (int[] atom : this.getBody()) {
            if (relation == atom[RELATION_POSITION])
                return false;
        }
        return true;
    }

    protected List<Integer> promisingRelations() {
        List<Integer> relations = new ArrayList<>();

        for (int relation : SelectedRelations) {
            if (this.doesNotContainsRelation(relation))
                relations.add(relation);
        }
        return relations;
    }

    public boolean isAcyclicInstantiated() {
        return hasAcyclicInstantiatedParameterInHead;
    }
    public void setAcyclicInstantiatedTrue() {
        this.hasAcyclicInstantiatedParameterInHead = true ;
    }


    // TODO sortedBody attribute
    public int[] GetFirstSortedBodyAtom() {
        return GetSortedBodyAtom(0);
    }

    public int[] GetLastSortedBodyAtom() {
        return GetSortedBodyAtom(getBody().size() - 1);
    }

    public int[] GetSortedBodyAtom(int id) {
        List<int[]> body = utils.SortPerfectPathBody(this);
        return body.get(getBody().size() - 1 - id);
    }

    public double HeadSize() {
        double headSize ;
        if (this.isAcyclicInstantiated())
            headSize = Kb.countOneVariable(this.getHead()) ;
        else {
            headSize = Kb.count(this.getHead());
        }
        return headSize;
    }

    public int HeadToBodyJoinVariable() {
        return isAcyclicInstantiated() ? this.getHead()[
                utils.NextPosition(instantiatedParameterPositionInHead)
                ] : this.getHead()[OBJECT_POSITION] ;
    }

    static selectivityMethod Selectivity ;

    public static void setSelectivity(selectivityMethod selectivity) {
        Selectivity = selectivity;
    }

    public static selectivityMethod getSelectivity() {
        return Selectivity ;
    }

    public double HeadToBodySelectivity() {
        // Head to body
        try {
            return Selectivity.selectivity(
                    this.getHead(),
                    this.GetFirstSortedBodyAtom(),
                    HeadToBodyJoinVariable()
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MiniAmieRule "+ this + " raw " + utils.RawBodyHeadToString(
                    utils.SortPerfectPathBody(this), this.getHead()) + " instantiated pos in head "
                    + this.instantiatedParameterPositionInHead, e) ;
        }
    }

    public int InitJoinVariablePosition() {
        // Head to body
        return utils.NextPosition(
                utils.VariablePosition(
                        this.GetFirstSortedBodyAtom(),
                        HeadToBodyJoinVariable()
                )
        );
    }


    public double BodySelectivity() {
        int joinVariablePosition = this.InitJoinVariablePosition();
        double bodySelectivity = 1 ;
        int[] atomPrev = this.GetFirstSortedBodyAtom() ;

        for (int atomNextId = 1 ; atomNextId <= this.getBody().size() - 1 ; atomNextId++) {
            int[] atomNext = GetSortedBodyAtom(atomNextId);
            int joinVariable = atomPrev[joinVariablePosition] ;
            bodySelectivity *= Selectivity.selectivity(
                    atomNext,
                    atomPrev,
                    joinVariable
            ) ;

            joinVariablePosition =
                    utils.NextPosition(
                            utils.VariablePosition(
                                    atomNext,
                                    joinVariable
                            )
                    );
            atomPrev = atomNext;
        }
        return bodySelectivity;
    }

        /**
         *
         * @return
         */
    public double ComputeSupportApproximation() {

        if (this.getBody().isEmpty())
            return this.HeadSize() ;

        return this.HeadSize() * this.HeadToBodySelectivity() * this.BodySelectivity() ;
    }

    public double HeadCoverageApproximation() {
        return this.ComputeSupportApproximation() / this.HeadSize() ;
    }


}
