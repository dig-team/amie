/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.rules.ConfidenceMetric;
import amie.rules.Metric;
import amie.rules.Rule;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

/**
 * GPro class to compute GPro measures:
 *  ``Graph pattern entity ranking model for knowledge graph completion''
 *  From Ebisu, Takuma and Ichise, Ryutaro
 *
 * Which is basically PCA confidence (both ways) using injective mappings
 * @author jlajus
 */
public class GPro extends InjectiveMappingsAssistant {

	public GPro(KB dataSource) {
		super(dataSource);
	}

	@Override
	public String getDescription() {
       	return "Computes GPro measures instead of PCA Confidence";
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
                computeConfHead(candidate);
                computeConfTail(candidate);
            } else {
                computeStandardConfidence(candidate);
                if (candidate.getStdConfidence() >= this.minStdConfidence) {
                    computeConfHead(candidate);
                    computeConfTail(candidate);
                }
            }
        } else {
            computeConfTail(candidate);
            if ((computeConfHead(candidate) >= this.minPcaConfidence)
                    || (candidate.getMeasure("GPro_conf_tail") >= this.minPcaConfidence)) {
                if (this.ommitStdConfidence) {
                    candidate.setBodySize((long) candidate.getSupport() * 2);
                } else {
                    computeStandardConfidence(candidate);
                }
            }
        }
    }

    public boolean testConfidenceThresholds(Rule candidate) {
        boolean addIt = true;
	//if(candidate.containsLevel2RedundantSubgraphs()) {
        //    return false;
	//}

	if(candidate.getStdConfidence() >= minStdConfidence
		&& (candidate.getMeasure("GPro_conf_tail") >= minPcaConfidence
                 || candidate.getMeasure("GPro_conf_head") >= minPcaConfidence)){
			//Now check the confidence with respect to its ancestors
                        if (!useSkylinePruning) {
                            return true;
                        }
			Set<Rule> ancestors = candidate.getAncestors();
			for(Rule ancestor : ancestors){
				double ancestorConfidence = 0.0;
				double ruleConfidence = 0.0;
				if (this.confidenceMetric == ConfidenceMetric.PCAConfidence) {
                                    if (shouldBeOutput(ancestor)
                                            && candidate.getMeasure("GPro_conf_tail") <= ancestor.getMeasure("GPro_conf_tail")
                                            && candidate.getMeasure("GPro_conf_head") <= ancestor.getMeasure("GPro_conf_head"))
                                        addIt = false;
					break;
				} else {
					ancestorConfidence = ancestor.getStdConfidence();
					ruleConfidence = candidate.getStdConfidence();
				}
				// Skyline technique on PCA confidence
				if (shouldBeOutput(ancestor) &&
						ruleConfidence <= ancestorConfidence){
					addIt = false;
					break;
				}
			}
		}else{
			return false;
		}

		return addIt;
	}

    public double computeConfTail(Rule rule) {
        /*
         * e.g Injective PCA.
        */
        if (rule.isEmpty()) {
            return 0;
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
            freeVarPos = 2;
        }

        existentialTriple[freeVarPos] = KB.map("?x9");
        if (!antecedent.isEmpty()) {

            try {
                if (noOfHeadVars == 1) {
                    antecedent = rule.injectiveBody();
                    antecedent.add(existentialTriple);
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) computePcaBodySize(
                            succedent[0],
                            succedent[2], rule, rule.injectiveBody(), existentialTriple, freeVarPos);
                }
                rule.setMeasure("GPro_conf_tail_body", pcaDenominator);
                rule.setMeasure("GPro_conf_tail", (1.0*rule.getSupport()) / pcaDenominator);
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return rule.getMeasure("GPro_conf_tail");
    }

    public double computeConfHead(Rule rule) {
        /*
         * e.g Injective PCA.
        */
        if (rule.isEmpty()) {
            return 0;
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
            freeVarPos = 0;
        }

        existentialTriple[freeVarPos] = KB.map("?x9");
        if (!antecedent.isEmpty()) {

            try {
                if (noOfHeadVars == 1) {
                    antecedent = rule.injectiveBody();
                    antecedent.add(existentialTriple);
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) computePcaBodySize(
                            succedent[2],
                            succedent[0], rule, rule.injectiveBody(), existentialTriple, freeVarPos);
                }
                rule.setMeasure("GPro_conf_head_body", pcaDenominator);
                rule.setMeasure("GPro_conf_head", (1.0*rule.getSupport()) / pcaDenominator);
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return rule.getMeasure("GPro_conf_head");
    }

    /**
     * It returns a string representation of the rule depending on the assistant configurations
     * @param rule
     * @return
     */
	public String formatRule(Rule rule) {
                DecimalFormat df = new DecimalFormat("#.#########");
                DecimalFormat df1 = new DecimalFormat("#.##");
		StringBuilder result = new StringBuilder();
		Metric[] metrics2Ommit = new Metric[]{Metric.PCAConfidence, Metric.PCABodySize};
		if (this.ommitStdConfidence) {
			metrics2Ommit = new Metric[]{Metric.PCAConfidence, Metric.PCABodySize, Metric.StandardConfidence, Metric.BodySize};
		}

		if (this.datalogNotation) {
                    if (isVerbose()) {
    			result.append(rule.getDatalogFullRuleString(metrics2Ommit));
                    } else {
    			result.append(rule.getDatalogBasicRuleString(metrics2Ommit));
                    }
                } else {
                    if (isVerbose()) {
    			result.append(rule.getFullRuleString(metrics2Ommit));
                    } else {
    			result.append(rule.getBasicRuleString(metrics2Ommit));
                    }
                }
                double lazyval = rule.getMeasure("GPro_conf_tail");
                if (lazyval < this.minPcaConfidence) {
                    result.append("\t< " + df.format(this.minPcaConfidence));
                } else {
                    result.append("\t" + df.format(lazyval));
                }
                lazyval = rule.getMeasure("GPro_conf_head");
                if (lazyval < this.minPcaConfidence) {
                    result.append("\t< " + df.format(this.minPcaConfidence));
                } else {
                    result.append("\t" + df.format(lazyval));
                }

		return result.toString();
	}
}
