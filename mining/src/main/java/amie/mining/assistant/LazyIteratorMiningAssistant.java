/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.mining.assistant.variableorder.VariableOrder;
import amie.data.KB;
import amie.rules.Rule;
import java.util.List;

/**
 *
 * @author jlajus
 */
public class LazyIteratorMiningAssistant extends LazyMiningAssistant {

    public LazyIteratorMiningAssistant(KB dataSource) {
        super(dataSource);
    }

    public LazyIteratorMiningAssistant(KB dataSource, VariableOrder order) {
        super(dataSource, order);
    }

    /**
     * It computes the standard and the PCA confidence of a given rule. It
     * assumes the rule's cardinality (absolute support) is known.
     *
     * @param candidate
     */
    public void calculateConfidenceMetrics(Rule candidate) {
        if (this.minPcaConfidence == 0) {
            if (this.ommitStdConfidence) {
                candidate.setBodySize((long) candidate.getSupport() * 2);
                computePCAConfidence(candidate);
            } else {
                computeStandardConfidence(candidate);
                if (candidate.getStdConfidence() >= this.minStdConfidence) {
                    computePCAConfidence(candidate);
                }
            }
        } else {
            computePCAConfidence(candidate);
            if (candidate.getPcaConfidence() >= this.minPcaConfidence) {
                if (this.ommitStdConfidence) {
                    candidate.setBodySize((long) candidate.getSupport() * 2);
                } else {
                    computeStandardConfidence(candidate);
                }
            }
        }
    }

    /**
     * Returns the denominator of the PCA confidence expression for the
     * antecedent of a rule.
     *
     * @param var1
     * @param var2
     * @param query
     * @param antecedent
     * @param existentialTriple
     * @param nonExistentialPosition
     * @return
     */
    @Override
    protected double computePcaBodySize(int var1, int var2, Rule query, List<int[]> antecedent, int[] existentialTriple, int nonExistentialPosition) {
        antecedent.add(existentialTriple);
        long t1 = System.currentTimeMillis();
        long result;
        if (this.minPcaConfidence > 0.0) {
            result = this.kb.countDistinctPairsUpToWithIterator((long) Math.ceil(query.getSupport() / this.minPcaConfidence) + 1, var1, var2, antecedent);
        } else {
            result = this.kb.countDistinctPairs(var1, var2, antecedent);
        }
        long t2 = System.currentTimeMillis();
        query.setPcaConfidenceRunningTime(t2 - t1);
        if ((t2 - t1) > 20000 && this.verbose) {
            System.err.println("countPairs vars " + KB.unmap(var1) + ", " + KB.unmap(var2) + " in " + KB.toString(antecedent) + " has taken " + (t2 - t1) + " ms");
        }
        return result;
    }

    /**
     * Returns the number of distinct bindings of the given variables in the
     * body of the rule.
     *
     * @param var1
     * @param var2
     * @param query
     * @return
     */
    @Override
    protected long computeBodySize(int var1, int var2, Rule query) {
        long t1 = System.currentTimeMillis();
        long result;
        if (this.minStdConfidence > 0.0) {
            result = this.kb.countDistinctPairsUpToWithIterator((long) Math.ceil(query.getSupport() / this.minStdConfidence) + 1, var1, var2, query.getAntecedent());
        } else {
            result = this.kb.countDistinctPairs(var1, var2, query.getAntecedent());
        }
        long t2 = System.currentTimeMillis();
        query.setConfidenceRunningTime(t2 - t1);
        if ((t2 - t1) > 20000 && this.verbose) {
            System.err.println("countPairs vars " + KB.unmap(var1) + ", " + KB.unmap(var2) + " in " + KB.toString(query.getAntecedent()) + " has taken " + (t2 - t1) + " ms");
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "Lazy mining assistant that stops counting "
                + "when the denominator gets too high (iterator version)";
    }
}
