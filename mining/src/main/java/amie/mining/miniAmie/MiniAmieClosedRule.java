package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.rules.Rule;

import java.util.HashSet;
import java.util.List;

public class MiniAmieClosedRule extends Rule {


    private double approximateHC = -1 ;

    private double approximateSupport = -1;

    private double alternativeApproximateSupport = -1;

    private long supportNano ;

    private long appSupportNano ;

    private FactorsOfApproximateSupportClosedRule factorsOfApproximateSupport ;


    public MiniAmieClosedRule(AbstractKB kb) {
        super(kb);
    }

    public MiniAmieClosedRule(int[] headAtom, double cardinality, AbstractKB kb) {
        super(headAtom, cardinality, kb);
    }

    public MiniAmieClosedRule(Rule rule) {
        super(rule, -1, rule.kb) ;
    }

    public MiniAmieClosedRule(Rule otherQuery, double support, AbstractKB kb) {
        super(otherQuery, support, kb);
    }

    public MiniAmieClosedRule(int[] head, List<int[]> body, double cardinality, AbstractKB kb) {
        super(head, body, cardinality, kb);
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

    protected static boolean HasNoRedundancies(Rule rule) {
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

    public FactorsOfApproximateSupportClosedRule getFactorsOfApproximateSupport() {
        return factorsOfApproximateSupport;
    }

    public void setFactorsOfApproximateSupport(FactorsOfApproximateSupportClosedRule factorsOfApproximateSupport) {
        this.factorsOfApproximateSupport = factorsOfApproximateSupport;
    }

}
