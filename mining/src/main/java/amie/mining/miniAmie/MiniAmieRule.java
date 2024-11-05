package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;


public class MiniAmieRule extends Rule {


    private int lastOpenParameter ;


    private int correctingFactor ;


    public MiniAmieRule(Rule rule) {
        super(rule, rule.getSupport(), rule.kb);
        this.setLastOpenParameter(rule.getHead()[OBJECT_POSITION]) ;
    }


    /** Appends new atom to clone
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
        this.setLastOpenParameter(newOpenParameter) ;

        int[] newAtom = new int[ATOM_SIZE];
        this.getBody().add(newAtom);

        newAtom[SUBJECT_POSITION] = subject;
        newAtom[RELATION_POSITION] = relation;
        newAtom[OBJECT_POSITION] = object;

        setCorrectingFactor(OpenCorrectingFactor(relation));
    }

    public MiniAmieRule(MiniAmieRule rule, double support, AbstractKB kb) {
        super(rule, support, kb);
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

    /**
     * @return List of new rules and their correcting factors for search space size estimate
     */
    public ArrayList<MiniAmieRule> AddDangling() {
        ArrayList<MiniAmieRule> openRules = new ArrayList<>();
        List<Integer> relations = this.promisingRelations() ;
        this.getOpenVariables() ;
        int unboundParameter = this.newVariable() ;

        if (relations.isEmpty()) {
            return null;
        }

        for (int relation : relations) {
            MiniAmieRule closedRule = new MiniAmieRule(this,
                    unboundParameter, relation, lastOpenParameter, unboundParameter);
            openRules.add(closedRule);

            // Reversing unbound parameter and join object
            MiniAmieRule closedRuleAlt = new MiniAmieRule(this,
                    lastOpenParameter, relation, unboundParameter, unboundParameter);
            openRules.add(closedRuleAlt);
        }
        return openRules;
    }

    public ArrayList<MiniAmieClosedRule> AddClosure() {
        int[] headAtom = this.getHead();

        int joinSubject = headAtom[SUBJECT_POSITION];
        int joinObject = this.lastOpenParameter ;

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
        return this.getBody().size() + 2 > miniAMIE.MaxRuleSize ;
    }

    public boolean ExceedsLengthWhenOpening() {
        return this.getBody().size() + 3 > miniAMIE.MaxRuleSize ;
    }

    // TODO replace that with IsPrunedOpenRule static method attribute to avoid repeated PruningMetric check
    public boolean IsNotPruned() {
        switch(PM) {
            case ApproximateSupport -> {
                return ApproximateSupportOpenRule(this) >= MinSup ;
            }
            case ApproximateHeadCoverage -> {
                return ApproximateHeadCoverageOpenRule(this) >= MinHC ;
            }
            case AlternativeApproximateSupport -> {
                return AltApproximateSupportOpenRule(this) >= MinSup ;
            }
            case Support -> {
                return RealSupport(this) >= MinSup ;
            }
            case HeadCoverage -> {
                return RealHeadCoverage(this) >= MinHC ;
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

    private List<Integer> promisingRelations() {
        List<Integer> relations = new ArrayList<>();

        for (int relation : SelectedRelations) {
            if (this.doesNotContainsRelation(relation))
                relations.add(relation);
        }
        return relations;
    }

}
