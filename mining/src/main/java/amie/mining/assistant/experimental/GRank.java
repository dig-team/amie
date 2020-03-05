/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.data.KB.Instantiator;
import amie.data.SetU;
import amie.data.U;
import static amie.data.U.decreasingKeys;
import amie.data.tuple.IntPair;
import amie.rules.ConfidenceMetric;
import amie.rules.Metric;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GRank assistant that computes quality of a rule as defined in:
 * ``Graph pattern entity ranking model for knowledge graph completion''
 * From Ebisu, Takuma and Ichise, Ryutaro
 * @author jlajus
 */
public class GRank extends InjectiveMappingsAssistant {
    
    public GRank(KB dataSource) {
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
    @Override
    public void calculateConfidenceMetrics(Rule candidate) {
        if (this.minPcaConfidence == 0) {
            if (this.ommitStdConfidence) {
                candidate.setBodySize((long) candidate.getSupport() * 2);
                computeDMaps(candidate);
            } else {
                computeStandardConfidence(candidate);
                if (candidate.getStdConfidence() >= this.minStdConfidence) {
                    computeDMaps(candidate);
                }
            }
        } else {
            computeDMaps(candidate);
            if ((candidate.getMeasure("GRank_map_tail") >= this.minPcaConfidence)
                    || (candidate.getMeasure("GRank_map_head") >= this.minPcaConfidence)) {
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
	if(candidate.containsLevel2RedundantSubgraphs()) {
            return false;
	}	
		
	if(candidate.getStdConfidence() >= minStdConfidence 
		&& (candidate.getMeasure("GRank_map_tail") >= minPcaConfidence
                 || candidate.getMeasure("GRank_map_head") >= minPcaConfidence)){
			//Now check the confidence with respect to its ancestors
			List<Rule> ancestors = candidate.getAncestors();			
			for(int i = 0; i < ancestors.size(); ++i){
				double ancestorConfidence = 0.0;
				double ruleConfidence = 0.0;
				if (this.confidenceMetric == ConfidenceMetric.PCAConfidence) {
                                    if (shouldBeOutput(ancestors.get(i)) 
                                            && candidate.getMeasure("GRank_map_tail") <= ancestors.get(i).getMeasure("GRank_map_tail")
                                            && candidate.getMeasure("GRank_map_head") <= ancestors.get(i).getMeasure("GRank_map_head"))
                                        addIt = false;
					break;   
				} else {
					ancestorConfidence = ancestors.get(i).getStdConfidence();
					ruleConfidence = candidate.getStdConfidence();
				}
				// Skyline technique on PCA confidence					
				if (shouldBeOutput(ancestors.get(i)) && 
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
    
    /**
     * Given the scores of the predictions for X, returns for any prediction y
     * the index and the size of the block $N_d$ it belongs to.
     * 
     * For what I understood, given the entities ordered according to their rank
     * and U_d being the matrix of size $d \times d$ filled with ones, then the
     * matrix DRank is a block diagonal matrix filled with blocks of the forms:
     *  $N_d = \frac{U_d}{d}$ for $d$ between 1 and $n$
     * 
     * The rank is determined by the number of injective mappings and d entities
     * with the same rank form a $N_d$ block.
     * 
     * The size of the matrix is the number of predictions y for a given x for
     * a rule B => r(x, y) (or r(y, x) for the head version), e.g the number of 
     * mappings of the body for a given x.
     * 
     * Warning: indexes starts at 0.
     * @param scores: A map from the predictions y to their score.
     * @return 
     */
    public Int2ObjectMap<IntPair> computeDRankOfX(Int2LongMap scores) {
        Int2ObjectMap<IntPair> result = new Int2ObjectOpenHashMap<>(scores.size());
        int rank = 0;
        int size;
        long score;
        IntList ordered = decreasingKeys(scores);
        while(rank < ordered.size()) {
            score = scores.get(ordered.getInt(rank));
            size = 1;
            while(rank+size < ordered.size() && score == scores.get(ordered.getInt(rank+size))) {
                size += 1;
            }
            for (int i = 0; i < size; i++) {
                result.put(ordered.getInt(rank+i), new IntPair(rank, size));
            }
            rank += size;
        }
        return result;
    }
    
    /**
     * Compute the ``a'' set for a query, e.g the predictions already in the KB,
     * using the prediction set already precomputed.
     * @param head
     * @param xpos
     * @param xvalue
     * @param dRank
     * @return 
     */
    
    public IntSet computeASetForX(int[] head, int xpos, int xvalue, Int2ObjectMap<IntPair> dRank) {
        int ypos = (xpos == 0) ? 2 : 0;
        int[] hquery = Arrays.copyOf(head, 3);
        hquery[xpos] = xvalue;
        List<int[]> query = new ArrayList<>(1);
        query.add(hquery);
        return (new IntOpenHashSet(new SetU.intersectionIntIterator(
            dRank.keySet(),
            kb.selectDistinct(hquery[ypos], query))));
    }
    
    /**
     * Computes all the precision at K of the ranked elements and returns them
     * in a array of double.
     * 
     * An element y of the aSet has a contribution to the precision at K of 
     *  (d being the number of entity with the same rank than y): 
     * * 0 if first-rank(y) &lt; K
     * * (K - first-rank(y)) / d if first-rank(y) &lt;= K &lt; first-rank(y) + d
     * * 1 otherwise
     * 
     * The normalization by the factor K is done at the end.
     * 
     * @param aSet
     * @param dRank
     * @return 
     */
    public double[] computeDPrecisionAtKForX(IntSet aSet, Int2ObjectMap<IntPair> dRank) {
        double[] result = new double[dRank.size()];
        for (int y : aSet) {
            IntPair rs = dRank.get(y);
            for (int i = rs.first; i < rs.first + rs.second; i++) {
                result[i] += ((double) (i - rs.first + 1)) / rs.second;
            }
            for (int i = rs.first + rs.second; i < dRank.size(); i++) {
                result[i] += 1;
            }
        }
        for (int i = 0; i < dRank.size(); i++) {
            result[i] /= (i+1);
        }
        return result;
    }
    
    /**
     * Computes the Average Precision given ranked elements.
     * 
     * The rank score at rank K of y of the aSet is always:
     *  (d being the number of entity with the same rank than y): 
     * * 0 if first-rank(y) &lt; K
     * * 1 / d if first-rank(y) &lt;= K &lt; first-rank(y) + d
     * * 0 otherwise
     * 
     * @param aSet
     * @param dRank
     * @return 
     */
    public double computeDAveragePrecisionForX(IntSet aSet, Int2ObjectMap<IntPair> dRank) {
        double result = 0;
        if (aSet.isEmpty()) return 0;
        double[] kPrecision = computeDPrecisionAtKForX(aSet, dRank);
        for (int y : aSet) {
            IntPair rs = dRank.get(y);
            for (int i = rs.first; i < rs.first + rs.second; i++) {
                result += kPrecision[i] / rs.second;
            }
        }
        return (result / aSet.size());
    }
    
    public IntSet computeQSetForX(int[] head, List<int[]> injectiveBody, int xpos) {
        int[] existentialAtom = head.clone();
        List<int[]> query = U.deepCloneInt(injectiveBody);
        existentialAtom[2-xpos] = KB.map("?x9");
        query.add(existentialAtom);
        return kb.selectDistinct(head[xpos], query);
    }
    
    public double computeDMapForX(Rule r, int xpos) {
        int[] head = r.getHead();
        List<int[]> query = r.injectiveBody();
        IntSet qSet = computeQSetForX(head, query, xpos);
        if (qSet.isEmpty()) { return 0; }
        Int2ObjectMap<IntPair> dRank;
        IntSet aSet;
        double result = 0;
        Int2LongMap scores;
        try (Instantiator insty = new Instantiator(query, head[xpos])) {
            for (int x : qSet) {
                scores = kb.selectDistinctMappings(head[2-xpos], insty.instantiate(x));
                dRank = this.computeDRankOfX(scores);
                aSet = this.computeASetForX(head, xpos, x, dRank);
                result += this.computeDAveragePrecisionForX(aSet, dRank);
            }
        }
        return (result / qSet.size());
    }
    
    public void computeDMaps(Rule r) {
        int[] head = r.getHead();
        if (!KB.isVariable(head[0]) || !KB.isVariable(head[2])) {
            throw new UnsupportedOperationException("Constants in the head atoms are not (yet ?) supported by GRank");
        }
        
        long t1 = System.currentTimeMillis();
        r.setMeasure("GRank_map_tail", computeDMapForX(r, 0));
        long t2 = System.currentTimeMillis();
        if ((t2 - t1) > 20000 && this.verbose) {
            System.err.println("GRank_map_tail of " + KB.toString(r.getTriples()) + " has taken " + (t2 - t1) + " ms");
        }
        
        t1 = System.currentTimeMillis();
        r.setMeasure("GRank_map_head", computeDMapForX(r, 2));
        t2 = System.currentTimeMillis();
        if ((t2 - t1) > 20000 && this.verbose) {
            System.err.println("GRank_map_head of " + KB.toString(r.getTriples()) + " has taken " + (t2 - t1) + " ms");
        }
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
                double lazyval = rule.getMeasure("GRank_map_tail");
                if (lazyval < this.minPcaConfidence) {
                    result.append("\t< " + df.format(this.minPcaConfidence));
                } else {
                    result.append("\t" + df.format(lazyval));
                }
                lazyval = rule.getMeasure("GRank_map_head");
                if (lazyval < this.minPcaConfidence) {
                    result.append("\t< " + df.format(this.minPcaConfidence));
                } else {
                    result.append("\t" + df.format(lazyval));
                }
                
		return result.toString();
	}
}
