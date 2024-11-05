package amie.mining.miniAmie;

import amie.mining.miniAmie.output.Attributes;
import amie.rules.Rule;

import java.util.HashSet;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;

public class MiniAmieClosedRule extends MiniAmieRule {

    static final int CLOSED_RULE_OPEN_VALUE = 0 ;

    private double approximateHC = -1 ;

    private double approximateSupport = -1;

    private double alternativeApproximateSupport = -1;

    private long supportNano ;

    private long appSupportNano ;

    private Attributes factorsOfApproximateSupport ;

    public MiniAmieClosedRule(Rule rule) {
        super(rule) ;
    }

    /** Appends closing atom to clone
     * @param rule
     * @param subject
     * @param relation
     * @param object
     * @return clone of the given rule with new closing atom
     */
    public MiniAmieClosedRule(MiniAmieRule rule, int subject, int relation, int object) {
        super(rule, rule.getSupport(), rule.kb);
        this.setLastOpenParameter(CLOSED_RULE_OPEN_VALUE) ;

        int[] newAtom = new int[ATOM_SIZE];
        this.getBody().add(newAtom);

        newAtom[SUBJECT_POSITION] = subject;
        newAtom[RELATION_POSITION] = relation;
        newAtom[OBJECT_POSITION] = object;

        setCorrectingFactor(ClosedCorrectingFactor(relation));
    }

    // todo test this
    public static boolean IsRealPerfectPath(Rule rule) {

        boolean found_x = false ;
        boolean found_y = false ;

        // Completes containsSinglePath method by checking that x is always subject and y is always object
        if (rule.containsSinglePath()) {
            for(int[] atom: rule.getBody()) {
                if(atom[utils.SUBJECT_POSITION] == rule.getHead()[utils.SUBJECT_POSITION])
                    found_x = true ;
                if(atom[utils.OBJECT_POSITION] == rule.getHead()[utils.OBJECT_POSITION])
                    found_y = true ;
            }
        }
        return found_x && found_y ;
    }

    public static boolean HasNoRedundancies(Rule rule) {
        HashSet<Integer> relations = new HashSet<>();
        relations.add(rule.getHead()[utils.RELATION_POSITION]);
        // Redundancy check
        for(int[] atom: rule.getBody()) {
            int relation = atom[utils.RELATION_POSITION];
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

    public double getAlternativeApproximateSupport() {
        return alternativeApproximateSupport;
    }

    public void setAlternativeApproximateSupport(double alternativeApproximateSupport) {
        this.alternativeApproximateSupport = alternativeApproximateSupport;
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

    public Attributes getFactorsOfApproximateSupport() {
        return factorsOfApproximateSupport;
    }

    public void setFactorsOfApproximateSupport(Attributes factorsOfApproximateSupport) {
        this.factorsOfApproximateSupport = factorsOfApproximateSupport;
    }

    // TODO replace that with IsPrunedClosedRule static method attribute to avoid repeated PruningMetric check
    @Override
    public boolean IsNotPruned() {
        switch(PM) {
            case ApproximateSupport -> {
                return ApproximateSupportClosedRule(this) >= MinSup ;
            }
            case ApproximateHeadCoverage -> {
                return ApproximateHeadCoverageClosedRule(this) >= MinHC ;
            }
            case AlternativeApproximateSupport -> {
                return AltApproximateSupportClosedRule(this) >= MinSup ;
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

}
