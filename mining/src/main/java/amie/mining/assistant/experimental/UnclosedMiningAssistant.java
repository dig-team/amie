package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.ConfidenceMetric;
import amie.rules.Rule;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import java.util.Set;

public class UnclosedMiningAssistant extends DefaultMiningAssistant {

	public UnclosedMiningAssistant(KB dataSource) {
		super(dataSource);
		confidenceMetric = ConfidenceMetric.StandardConfidence;
		amie.data.Schema.materializeTaxonomy(dataSource);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getDescription() {
		return "Return also unclosed rules.";
	}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return true;
	}
	
	@Override
	public double computePCAConfidence(Rule rule) {
		return -1.0;
	}
	
	@Override
	protected Collection<Rule> buildInitialQueries(Int2IntMap relations, double minSupportThreshold) {
		List<Rule> output = new ArrayList<>();
		Rule query = new Rule();
		int[] newEdge = query.fullyUnboundTriplePattern();
		int relation = KB.TRANSITIVETYPEbs;
		int[] succedent = newEdge.clone();
		succedent[1] = relation;
		Rule candidate = new Rule(succedent, kb.size());
		candidate.setFunctionalVariablePosition(0);
		registerHeadRelation(candidate);
		getInstantiatedAtoms(candidate, null, 0, 2, minSupportThreshold, output);
		for (Rule r : output) {
			System.err.println(r.getFullRuleString());
		}
		return output;
	}
	
	@Override
	public boolean shouldBeClosed() {
		return false;
	}
	
	@Override
	public boolean testConfidenceThresholds(Rule candidate) {
		
		if(candidate.containsLevel2RedundantSubgraphs()) {
			return false;
		}	
		
		if(candidate.getStdConfidence() >= minStdConfidence){
			//Now check the confidence with respect to its ancestors
			Set<Rule> ancestors = candidate.getAncestors();
			for(Rule ancestor : ancestors){
				double ancestorConfidence = ancestor.getStdConfidence();
				// Skyline technique on PCA confidence					
				if ((ancestor.getRealLength() > 1) &&
					  (ancestorConfidence >= .95)) {
					return false;
				}
			}
		}else{
			return false;
		}
		
		return true;
	}

}
