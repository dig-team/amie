package amie.mining.assistant.experimental;

import java.util.Collection;

import amie.data.KB;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.ConfidenceMetric;
import amie.rules.Rule;

import it.unimi.dsi.fastutil.ints.Int2IntMap;

public class StarShapedMiningAssistant extends MiningAssistant {

	private CompletenessMiningAssistant wrapped;
	
	public StarShapedMiningAssistant(KB dataSource) {
		super(dataSource);
		wrapped = new CompletenessMiningAssistant(dataSource);
		this.maxDepth++;
		this.recursivityLimit = 1;
		this.confidenceMetric = ConfidenceMetric.StandardConfidence;
	}
	
	@Override
	public String getDescription() {
        return "Mining star completeness rules of the form "
        		+ "B => isComplete(x, relation) or B => isIncomplete(x, relation) "
        		+ "where B  = r1(x, y1) ^ r2(x, y2) ... [EXPERIMENTAL]";
	}
	
	@Override
	protected Collection<Rule> buildInitialQueries(Int2IntMap relations, 
			double minSupportThreshold) {
		return wrapped.buildInitialQueries(relations, minSupportThreshold);
	}
	
	@Override
	public void setRecursivityLimit(int recursitivityLimit) {
		System.err.println("StarShapedMiningAssistant: The recursivity limit for this class is fixed to 1 and cannot be changed.");
	}
	
	@Override
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {}
	
	@Override
	public void setMaxDepth(int maxAntecedentDepth) {
		super.setMaxDepth(maxAntecedentDepth + 1);
	};
	
	@Override
	public int getMaxDepth() {
		return this.maxDepth - 1;
	}
	
	@Override
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		int[] newEdge = rule.fullyUnboundTriplePattern();
		int[] head = rule.getHead();
		//General case
		if(!isNotTooLong(rule))
			return;

		int nPatterns = rule.getTriples().size();		
		newEdge[0] = head[0];
		rule.getTriples().add(newEdge);
		Int2IntMap promisingRelations = kb.frequentBindingsOf(newEdge[1], 
				rule.getFunctionalVariable(), rule.getTriples());
		rule.getTriples().remove(nPatterns);
		
		for(int relation: promisingRelations.keySet()){
			if (this.bodyExcludedRelations != null && 
					this.bodyExcludedRelations.contains(relation))
				continue;
			//Here we still have to make a redundancy check						
			int cardinality = promisingRelations.get(relation);
			if(cardinality >= minSupportThreshold) {
				if(rule.containsRelation(relation) 
						||(relation == head[2])) {
					continue;
				}
				
				newEdge[1] = relation;
				Rule candidate = rule.addAtom(newEdge, cardinality);
				candidate.setHeadCoverage((double)candidate.getSupport() 
						/ headCardinalities.get(candidate.getHeadRelationBS()));
				candidate.setSupportRatio((double)candidate.getSupport() 
						/ (double)getTotalCount(candidate));
				candidate.addParent(rule);
				output.add(candidate);
			}
		}
	}
	
	@Override
	public void getTypeSpecializedAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return candidate.getRealLength() > 1;
	}
	@Override
	public double computePCAConfidence(Rule rule) {
		return this.wrapped.computePCAConfidence(rule);
	}

}
