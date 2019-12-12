/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.mining.assistant.variableorder.FunctionalOrder;
import amie.mining.assistant.variableorder.VariableOrder;
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
public class DefaultMiningAssistantWithOrder extends DefaultMiningAssistant {

    protected VariableOrder order;

    public DefaultMiningAssistantWithOrder(KB dataSource) {
        super(dataSource);
        this.order = new FunctionalOrder();
    }

    public DefaultMiningAssistantWithOrder(KB dataSource, VariableOrder order) {
        super(dataSource);
        this.order = order;
    }

    public void setVariableOrder(VariableOrder order) {
        this.order = order;
    }

    @Override
    public double computeCardinality(Rule rule) {
        if (rule.isEmpty()) {
            rule.setSupport(0l);
            rule.setHeadCoverage(0.0);
            rule.setSupportRatio(0.0);
        } else {
            int[] head = rule.getHead();
            if (KB.numVariables(head) == 2) {
                rule.setSupport(this.kb.countDistinctPairs(
                        order.getFirstCountVariable(rule),
                        order.getSecondCountVariable(rule), rule.getTriples()));
            } else {
                rule.setSupport(this.kb.countDistinct(rule.getFunctionalVariable(), rule.getTriples()));
            }
            rule.setSupportRatio((double) rule.getSupport() / this.kb.size());
            Double relationSize = new Double(this.getHeadCardinality(rule));
            if (relationSize != null) {
                rule.setHeadCoverage(rule.getSupport() / relationSize.doubleValue());
            }
        }
        return rule.getSupport();
    }

    @Override
    public double computePCAConfidence(Rule rule) {
        if (rule.isEmpty()) {
            return rule.getPcaConfidence();
        }

        List<int[]> antecedent = rule.getAntecedentClone();
        int[] succedent = rule.getTriples().get(0);
        double pcaDenominator;
        int[] existentialTriple = succedent.clone();
        int freeVarPos;
        int noOfHeadVars = KB.numVariables(succedent);

        if (noOfHeadVars == 1) {
            freeVarPos = KB.firstVariablePos(succedent) == 0 ? 2 : 0;
        } else {
            if (existentialTriple[0] == rule.getFunctionalVariable()) {
                freeVarPos = 2;
            } else {
                freeVarPos = 0;
            }
        }

        existentialTriple[freeVarPos] = KB.map("?x9");
        if (!antecedent.isEmpty()) {

            try {
                if (noOfHeadVars == 1) {
                    antecedent.add(existentialTriple);
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) computePcaBodySize(
                            order.getFirstCountVariable(rule),
                            order.getSecondCountVariable(rule), rule, antecedent, existentialTriple, freeVarPos);
                }
                rule.setPcaBodySize(pcaDenominator);
            } catch (UnsupportedOperationException e) {

            }
        }

        return rule.getPcaConfidence();
    }

    @Override
    public double computeStandardConfidence(Rule candidate) {
        if (candidate.isEmpty()) {
            return candidate.getStdConfidence();
        }
        // TODO Auto-generated method stub
        List<int[]> antecedent = candidate.getAntecedentClone();
        double denominator = 0.0;
        int[] head = candidate.getHead();

        if (!antecedent.isEmpty()) {
           //Confidence
            try {
                if (KB.numVariables(head) == 2) {
                    int var1, var2;
                    var1 = order.getFirstCountVariable(candidate);
                    var2 = order.getSecondCountVariable(candidate);
                    denominator = (double) computeBodySize(var1, var2, candidate);
                } else {
                    denominator = (double) this.kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
                }
                candidate.setBodySize((long) denominator);
            } catch (UnsupportedOperationException e) {

            }
        }

        return candidate.getStdConfidence();
    }
}
