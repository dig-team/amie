package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.data.KB;
import amie.mining.miniAmie.output.Attributes;
import amie.rules.Rule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;

public class MiniAmieClosedRule extends MiniAmieRule {

    static final int CLOSED_RULE_OPEN_VALUE = 0;

    private double approximateHC = -1;

    private double approximateSupport = -1;

    private long supportNano;

    private long appSupportNano;

    private Attributes factorsOfApproximateSupport;

    private double jacquardBasedAppSupport = -1;
    private double avgBasedAppSupport = -1;
    private double survivalRateBasedAppSupport = -1;

    public MiniAmieClosedRule(Rule rule) {
        super(rule);
    }

    public MiniAmieClosedRule(int[] headAtom, AbstractKB kb) {

        super(new Rule(headAtom, -1, kb));
    }

    public MiniAmieClosedRule(int[] headAtom, List<int[]> body, AbstractKB kb) {
        super(new Rule(headAtom, body, -1, kb));
    }

    /**
     * Appends closing atom to clone
     *
     * @param rule
     * @param subject
     * @param relation
     * @param object
     * @return clone of the given rule with new closing atom
     */
    public MiniAmieClosedRule(MiniAmieRule rule, int subject, int relation, int object) {
        super(rule, rule.getSupport(), rule.kb);
        InheritAttributes(rule);
        this.setLastOpenParameter(CLOSED_RULE_OPEN_VALUE);

        int[] newAtom = new int[ATOM_SIZE];
        this.getBody().add(newAtom);

        newAtom[SUBJECT_POSITION] = subject;
        newAtom[RELATION_POSITION] = relation;
        newAtom[OBJECT_POSITION] = object;

        setCorrectingFactor(ClosedCorrectingFactor(relation));
    }


    private boolean isAcyclicInstantiatedPerfect() {
        Rule auxRule = new Rule(this, -1, this.kb);
        List<int[]> auxBody = auxRule.getBody();

        // Single constant in head, replace it with variable
        int newVariable = auxRule.newVariable();
        if (!(KB.isVariable(auxRule.getHead()[SUBJECT_POSITION]))) {
            System.out.println(auxRule.getHead()[SUBJECT_POSITION] + " is constant");
            auxRule.getHead()[SUBJECT_POSITION] = newVariable;
            System.out.println("Replaced by " + auxRule.getHead()[SUBJECT_POSITION]);
        }

        if (!(KB.isVariable(auxRule.getHead()[OBJECT_POSITION]))) {
            System.out.println(auxRule.getHead()[OBJECT_POSITION] + " is constant");
            auxRule.getHead()[OBJECT_POSITION] = newVariable;
            System.out.println("Replaced by " + auxRule.getHead()[OBJECT_POSITION]);
        }

        // Single other atom with constant in body, replace it with variable
        boolean found = false;
        for (int k = 0; k < auxBody.size(); k++) {
            int[] atom = auxBody.get(k);
            if (!KB.isVariable(atom[SUBJECT_POSITION])
                    ^ !KB.isVariable(atom[OBJECT_POSITION])) {
                if (found) {
                    return false;
                }
                System.out.println("Found constant in body atom " + Arrays.toString(atom));
                found = true;
            }

            if (!KB.isVariable(atom[SUBJECT_POSITION])) {
                atom[SUBJECT_POSITION] = newVariable;

            }

            if (!KB.isVariable(atom[OBJECT_POSITION])) {
                atom[OBJECT_POSITION] = newVariable;
            }
            System.out.println("Replaced with variable " + Arrays.toString(atom));

        }

        return auxRule.containsSinglePath();
    }

    public boolean IsMiniAmieStylePerfectPath() {
        return isAcyclicInstantiatedPerfect() || containsSinglePath();
    }

    public static boolean HasNoRedundancies(Rule rule) {
        HashSet<Integer> relations = new HashSet<>();
        relations.add(rule.getHead()[utils.RELATION_POSITION]);
        // Redundancy check
        for (int[] atom : rule.getBody()) {
            int relation = atom[utils.RELATION_POSITION];
            if (relations.contains(relation)) {
                return false;
            }
            relations.add(relation);
        }
        return true;
    }

    public boolean HasNoRedundancies() {
        return HasNoRedundancies(this);
    }

    /**
     * ShouldHaveBeenFound will seek for a perfect path
     *
     * @param rule
     * @return
     */
    public static boolean RespectsLanguageBias(Rule rule) {
        return HasNoRedundancies(rule) && rule.containsSinglePath();
    }

    public boolean RespectsLanguageBias() {
        return HasNoRedundancies(this) && this.containsSinglePath();
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

    public Attributes getFactorsOfApproximateSupport() {
        return factorsOfApproximateSupport;
    }

    public void setFactorsOfApproximateSupport(Attributes factorsOfApproximateSupport) {
        this.factorsOfApproximateSupport = factorsOfApproximateSupport;
    }


    public double ClosureFactor() {
        double closureFactor;
        int[] lastBodyAtom = this.GetLastSortedBodyAtom();
        if (this.isAcyclicInstantiated()) {
            // When instantiated, closure factor is last atom projection size over last relation size
            double lastAtomProjectionSize = Kb.countOneVariable(lastBodyAtom);
            double lastRelationSize = Kb.relationSize(lastBodyAtom[RELATION_POSITION]);
            closureFactor = lastAtomProjectionSize / lastRelationSize;
        } else {
            // When uninstantiated, closure factor is body to head selectivity
            int[] head = this.getHead();
            int headSubject = head[SUBJECT_POSITION];
            closureFactor = Selectivity.selectivity(
                    head,
                    lastBodyAtom,
                    headSubject
            );
        }
        return closureFactor;
    }

    @Override
    public double SupportApproximation() {
        return super.SupportApproximation() * this.ClosureFactor();
    }


    public void ComputeClosedRuleMetrics(boolean computeRealMetrics) {
        long support = -1;
        long time = -1;
        long start = -1;
        if (computeRealMetrics) {
            start = System.nanoTime();
            support = utils.RealSupport(this);
            time = System.nanoTime() - start;
            this.setSupport(support);
            this.setSupportNano(time);
        }


        start = System.nanoTime();
        double appSupport = this.SupportApproximation();
        time = System.nanoTime() - start;
        this.setApproximateSupport(appSupport);
        if (!computeRealMetrics) {
            this.setSupport(appSupport);
        }

        setSelectivity(new JacquardSelectivity());
        this.setJacquardBasedAppSupport(this.SupportApproximation());
        setSelectivity(new AvgSelectivity());
        this.setAvgBasedAppSupport(this.SupportApproximation());
        setSelectivity(new SurvivalRateSelectivity());
        this.setSurvivalRateBasedAppSupport(this.SupportApproximation());

        miniAMIE.ResetSelectivity();


        this.setAppSupportNano(time);

        this.setHeadCoverage(utils.RealHeadCoverage(this));
        this.setApproximateHC(this.HeadCoverageApproximation());

        this.setFactorsOfApproximateSupport(
                new Attributes(this)
        );
        // Now let us calculate PCA confidence
        if (computeRealMetrics) {
            utils.RealPCADenominator(this);
        }
    }

    public double getSurvivalRateBasedAppSupport() {
        return survivalRateBasedAppSupport;
    }

    public void setSurvivalRateBasedAppSupport(double survivalRateBasedAppSupport) {
        this.survivalRateBasedAppSupport = survivalRateBasedAppSupport;
    }

    public double getAvgBasedAppSupport() {
        return avgBasedAppSupport;
    }

    public void setAvgBasedAppSupport(double avgBasedAppSupport) {
        this.avgBasedAppSupport = avgBasedAppSupport;
    }

    public double getJacquardBasedAppSupport() {
        return jacquardBasedAppSupport;
    }

    public void setJacquardBasedAppSupport(double jacquardBasedAppSupport) {
        this.jacquardBasedAppSupport = jacquardBasedAppSupport;
    }

}
