package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;


public class MiniAmieRule extends Rule {


    private int lastOpenParameter;
    private int correctingFactor;


    boolean hasAcyclicInstantiatedParameterInHead = false;
    int instantiatedParameterPositionInHead;


    public MiniAmieRule(Rule rule) {
        super(rule, rule.getSupport(), rule.kb);
        this.setLastOpenParameter(rule.getHead()[OBJECT_POSITION]);
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
        this.setLastOpenParameter(newOpenParameter);

        int[] newAtom = new int[ATOM_SIZE];
        this.getBody().add(newAtom);

        newAtom[SUBJECT_POSITION] = subject;
        newAtom[RELATION_POSITION] = relation;
        newAtom[OBJECT_POSITION] = object;

        setCorrectingFactor(OpenCorrectingFactor(relation));
    }

    protected void transmitMiniAmieAttributes(MiniAmieRule rule) {
        lastOpenParameter = rule.lastOpenParameter;
        hasAcyclicInstantiatedParameterInHead = rule.hasAcyclicInstantiatedParameterInHead;
        instantiatedParameterPositionInHead = rule.instantiatedParameterPositionInHead;
    }

    public MiniAmieRule(MiniAmieRule rule, double support, AbstractKB kb) {
        super(rule, support, kb);
        transmitMiniAmieAttributes(rule);
    }

    public MiniAmieRule(int[] head) {
        super(head, -1, Kb);
    }

    public MiniAmieRule(int[] head, int instantiatedParameterPositionInHead) {
        super(head, -1, Kb);
        this.hasAcyclicInstantiatedParameterInHead = true;
        this.instantiatedParameterPositionInHead = instantiatedParameterPositionInHead;

//        switch (instantiatedParameterPositionInHead) {
//            case SUBJECT_POSITION -> setLastOpenParameter(head[SUBJECT_POSITION]);
//            case OBJECT_POSITION -> setLastOpenParameter(head[OBJECT_POSITION]);
//            default -> throw new IllegalArgumentException("Bad position for constant in MiniAmieRule constructor : "
//                    + instantiatedParameterPositionInHead + " for head " + Arrays.toString(head));
//        }
        setLastOpenParameter(newVariable());
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

    public ArrayList<MiniAmieRule> AddDanglingToAcyclicWithConstants() {
        ArrayList<MiniAmieRule> openRules = new ArrayList<>();
        List<Integer> relations = this.promisingRelations();
        int unboundParameter = this.newVariable();
        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            // Instantiated rule
            List<int[]> newAtomQuery = new ArrayList<>();
            int[] newAtom = new int[]{unboundParameter, relation, lastOpenParameter} ;
            newAtomQuery.add(newAtom);
            // TODO reuse previously instantiated heads if possible
            IntSet objectConstants = Kb.selectDistinct(lastOpenParameter, newAtomQuery);
            for (int constant : objectConstants) {
                int[] head = newAtom.clone();
                head[OBJECT_POSITION] = constant;
                MiniAmieRule openRuleWithConstant = new MiniAmieRule(this,
                        unboundParameter, relation, constant, unboundParameter);
                openRuleWithConstant.setAcyclicInstantiatedTrue();
                openRules.add(openRuleWithConstant);
            }

            // Reversing unbound parameter and join object
            List<int[]> newAtomQueryAlt = new ArrayList<>();
            int[] newAtomAlt = new int[]{lastOpenParameter, relation, unboundParameter} ;
            newAtomQueryAlt.add(newAtomAlt);
            IntSet objectConstantsAlt = Kb.selectDistinct(lastOpenParameter, newAtomQueryAlt);
            for (int constant : objectConstantsAlt) {
                int[] head = newAtom.clone();
                head[SUBJECT_POSITION] = constant;
                MiniAmieRule openRuleWithConstant = new MiniAmieRule(this,
                        constant, relation, unboundParameter, unboundParameter);
                openRuleWithConstant.setAcyclicInstantiatedTrue();
                openRules.add(openRuleWithConstant);
            }
        }

        return openRules ;
    }

    public ArrayList<MiniAmieRule> AddDangling() {
        if (hasAcyclicInstantiatedParameterInHead) {
            return AddDanglingToAcyclicWithConstants();
        }

        ArrayList<MiniAmieRule> openRules = new ArrayList<>();
        List<Integer> relations = this.promisingRelations();
        int unboundParameter = this.newVariable();

        if (relations.isEmpty()) {
            return null;
        }
            for (int relation : relations) {
                MiniAmieRule openRule = new MiniAmieRule(this,
                        unboundParameter, relation, lastOpenParameter, unboundParameter);
                openRules.add(openRule);

                // Reversing unbound parameter and join object
                MiniAmieRule openRuleAlt = new MiniAmieRule(this,
                        lastOpenParameter, relation, unboundParameter, unboundParameter);
                openRules.add(openRuleAlt);
            }
        return openRules;
    }

    /**
     * Note: only useful for rules of size two.
     * @return
     */
    public ArrayList<MiniAmieClosedRule> AddClosureToAcyclicWithConstants() {
        int[] headAtom = this.getHead();
        int unboundParameter ;

        switch(instantiatedParameterPositionInHead) {
            case SUBJECT_POSITION -> unboundParameter = headAtom[OBJECT_POSITION];
            case OBJECT_POSITION -> unboundParameter = headAtom[SUBJECT_POSITION];
            default -> throw new IllegalArgumentException("Bad instantiated parameter position in head "
                    + instantiatedParameterPositionInHead + " for rule "
                    + utils.RawBodyHeadToString(getBody(), headAtom));
        }

        ArrayList<MiniAmieClosedRule> closedRules = new ArrayList<>();
        List<Integer> relations = this.promisingRelations();

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            // Instantiated rule
            List<int[]> newAtomQuery = new ArrayList<>();
            int[] newAtom = new int[]{unboundParameter, relation, lastOpenParameter} ;
            newAtomQuery.add(newAtom);
            // TODO reuse previously instantiated heads if possible
            IntSet objectConstants = Kb.selectDistinct(lastOpenParameter, newAtomQuery);
            for (int constant : objectConstants) {
                MiniAmieClosedRule closedRule = new MiniAmieClosedRule(this,
                        unboundParameter, relation, constant);
                closedRules.add(closedRule);
            }

            // Reversing join subject and constant order
            List<int[]> newAtomQueryAlt = new ArrayList<>();
            int[] newAtomAlt = new int[]{lastOpenParameter, relation, unboundParameter} ;
            newAtomQueryAlt.add(newAtomAlt);
            // TODO reuse previously instantiated heads if possible
            IntSet objectConstantsAlt = Kb.selectDistinct(lastOpenParameter, newAtomQueryAlt);
            for (int constant : objectConstantsAlt) {
                MiniAmieClosedRule closedRule = new MiniAmieClosedRule(this,
                        constant, relation, unboundParameter);
                closedRules.add(closedRule);
            }
        }

        return closedRules;
    }


    public ArrayList<MiniAmieClosedRule> AddClosure() {
        if (hasAcyclicInstantiatedParameterInHead) {
            return AddClosureToAcyclicWithConstants();
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
            closedRules.add(closedRule);

            // Reversing joinSubject et joinObject
            MiniAmieClosedRule closedRuleAlt = new MiniAmieClosedRule(this,
                    joinObject, relation, joinSubject);
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

        if (hasAcyclicInstantiatedParameterInHead) {
            return false ;
//            switch (PM) {
//                case ApproximateSupport -> {
//                    return ApproximateSupportOpenRule(this) >= MinSup;
//                }
//                case ApproximateHeadCoverage -> {
//                    return ApproximateHeadCoverageOpenRule(this) >= MinHC;
//                }
//                case AlternativeApproximateSupport -> {
//                    return AltApproximateSupportOpenRule(this) >= MinSup;
//                }
//                case Support -> {
//                    return RealSupport(this) >= MinSup;
//                }
//                case HeadCoverage -> {
//                    return RealHeadCoverage(this) >= MinHC;
//                }
//                default -> {
//                    return false;
//                }
//            }
        } else {
            switch (PM) {
                case ApproximateSupport -> {
                    return ApproximateSupportOpenRule(this) >= MinSup;
                }
                case ApproximateHeadCoverage -> {
                    return ApproximateHeadCoverageOpenRule(this) >= MinHC;
                }
                case AlternativeApproximateSupport -> {
                    return AltApproximateSupportOpenRule(this) >= MinSup;
                }
                case Support -> {
                    return RealSupport(this) >= MinSup;
                }
                case HeadCoverage -> {
                    return RealHeadCoverage(this) >= MinHC;
                }
                default -> {
                    return false;
                }
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


}
