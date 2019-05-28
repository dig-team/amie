/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;
import java.util.ArrayList;
import java.util.List;
import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class LazyMiningAssistant extends DefaultMiningAssistant {

    public LazyMiningAssistant(KB dataSource) {
        super(dataSource);
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
    protected double computePcaBodySize(ByteString var1, ByteString var2, Rule query, List<ByteString[]> antecedent, ByteString[] existentialTriple, int nonExistentialPosition) {
        antecedent.add(existentialTriple);
        long t1 = System.currentTimeMillis();
        long result;
        if (this.minPcaConfidence > 0.0) {
            result = this.kb.countDistinctPairsUpTo((long) Math.ceil(query.getSupport() / this.minPcaConfidence), var1, var2, antecedent);
        } else {
            result = this.kb.countDistinctPairs(var1, var2, antecedent);
        }
        long t2 = System.currentTimeMillis();
        query.setConfidenceRunningTime(t2 - t1);
        if ((t2 - t1) > 20000 && this.verbose) {
            System.out.println("countPairs vars " + var1 + ", " + var2 + " in " + KB.toString(antecedent) + " has taken " + (t2 - t1) + " ms");
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
    protected long computeBodySize(ByteString var1, ByteString var2, Rule query) {
        long t1 = System.currentTimeMillis();
        long result;
        if (this.minStdConfidence > 0.0) {
            result = this.kb.countDistinctPairsUpTo((long) Math.ceil(query.getSupport() / this.minStdConfidence), var1, var2, query.getAntecedent());
        } else {
            result = this.kb.countDistinctPairs(var1, var2, query.getAntecedent());
        }
        long t2 = System.currentTimeMillis();
        query.setPcaConfidenceRunningTime(t2 - t1);
        if ((t2 - t1) > 20000 && this.verbose) {
            System.out.println("countPairs vars " + var1 + ", " + var2 + " in " + KB.toString(query.getAntecedent()) + " has taken " + (t2 - t1) + " ms");
        }
        return result;
    }

    @Override
    public double computePCAConfidence(Rule rule) {
        if (rule.isEmpty()) {
            return rule.getPcaConfidence();
        }

        List<ByteString[]> antecedent = new ArrayList<ByteString[]>();
        antecedent.addAll(rule.getTriples().subList(1, rule.getTriples().size()));
        ByteString[] succedent = rule.getTriples().get(0);
        double pcaDenominator = 0.0;
        ByteString[] existentialTriple = succedent.clone();
        int freeVarPos = 0;
        int noOfHeadVars = KB.numVariables(succedent);

        if (noOfHeadVars == 1) {
            freeVarPos = KB.firstVariablePos(succedent) == 0 ? 2 : 0;
        } else if (existentialTriple[0].equals(rule.getFunctionalVariable())) {
            freeVarPos = 2;
        } else {
            freeVarPos = 0;
        }

        existentialTriple[freeVarPos] = ByteString.of("?xw");
        if (!antecedent.isEmpty()) {
            antecedent.add(existentialTriple);
            try {
                if (noOfHeadVars == 1) {
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else if (this.minPcaConfidence > 0.0) {
                    pcaDenominator = (double) this.kb.countDistinctPairsUpTo((long) Math.ceil(rule.getSupport() / this.minPcaConfidence), succedent[0], succedent[2], antecedent);
                } else {
                    pcaDenominator = (double) this.kb.countDistinctPairs(succedent[0], succedent[2], antecedent);
                }
                rule.setPcaBodySize(pcaDenominator);
            } catch (UnsupportedOperationException e) {

            }
        }
        return rule.getPcaConfidence();
    }

}
