package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;

public class InstantiatedHeadMiningAssistant extends DefaultMiningAssistant {

	public InstantiatedHeadMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	public String getDescription() {
        return "Counting on one variable. "
        		+ "Head relation is always instantiated in one argument";
	}
	
	@Override
	public Collection<Rule> getInitialAtomsFromSeeds(IntCollection relations, double minCardinality) {
		Collection<Rule> output = new ArrayList<>();
		Rule query = new Rule(kb);
		//The query must be empty
		if(!query.isEmpty()) {
			throw new IllegalArgumentException("Expected an empty query");
		}
		
		int[] newEdge = query.fullyUnboundTriplePattern();		
		query.getTriples().add(newEdge);
		
		for(int relation: relations) {
			newEdge[1] = relation;
			
			int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(newEdge);
			List<int[]> emptyList = Collections.emptyList();
			long cardinality = kb.countProjection(query.getHead(), emptyList);
			
			int[] succedent = newEdge.clone();
			Rule candidate = new Rule(succedent, cardinality, kb);
			candidate.setFunctionalVariablePosition(countVarPos);
			registerHeadRelation(candidate);
			ArrayList<Rule> tmpOutput = new ArrayList<>();
			getInstantiatedAtoms(candidate, null, 0, countVarPos == 0 ? 2 : 0, minCardinality, tmpOutput);			
			output.addAll(tmpOutput);
		}
		
		query.getTriples().remove(0);		
		return output;
	}
	
	@Override
	public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
		Collection<Rule> output = new ArrayList<>();
		Rule query = new Rule(kb);
		int[] newEdge = query.fullyUnboundTriplePattern();
		
		if(query.isEmpty()) {
			//Initial case
			query.getTriples().add(newEdge);
			List<int[]> emptyList = Collections.emptyList();
			Int2IntMap relations = kb.countProjectionBindings(query.getHead(), emptyList, newEdge[1]);
			for(int relation : relations.keySet()){
				if(headExcludedRelations != null && headExcludedRelations.contains(relation))
					continue;
				
				int cardinality = relations.get(relation);
				if (cardinality >= minSupportThreshold) {
					int[] succedent = newEdge.clone();
					succedent[1] = relation;
					int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(succedent);
					Rule candidate = new Rule(succedent, cardinality, kb);
					candidate.setFunctionalVariablePosition(countVarPos);
					registerHeadRelation(candidate);
					getInstantiatedAtoms(candidate, null, 0, countVarPos == 0 ? 2 : 0, minSupportThreshold, output);
					output.add(candidate);
				}
			}			
			query.getTriples().remove(0);
		}
		return output;
	}
	
	@Override
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule rule, double minCardinality, Collection<Rule> output) {
		int[] newEdge = rule.fullyUnboundTriplePattern();
		
		if (rule.isEmpty()) {
			rule.getTriples().add(newEdge);
			List<int[]> emptyList = Collections.emptyList();
			Int2IntMap relations = kb.countProjectionBindings(rule.getHead(), emptyList, newEdge[1]);
			for(int relation : relations.keySet()){
				if(headExcludedRelations != null && headExcludedRelations.contains(relation))
					continue;
				
				int cardinality = relations.get(relation);
				if (cardinality >= minCardinality) {
					int[] succedent = newEdge.clone();
					succedent[1] = relation;
					int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(succedent);
					Rule candidate = new Rule(succedent, cardinality, kb);
					candidate.setFunctionalVariablePosition(countVarPos);
					registerHeadRelation(candidate);
					getInstantiatedAtoms(candidate, null, 0, countVarPos == 0 ? 2 : 0, minCardinality, output);
					output.add(candidate);
				}
			}			
			rule.getTriples().remove(0);
		} else {
			if (!isNotTooLong(rule)) {
				return;
			}
			
			// Enforce this only for n > 2
			if (maxDepth > 2) {
				if(rule.getRealLength() == maxDepth - 1) {
					if(!rule.getOpenVariables().isEmpty() && (!allowConstants && !enforceConstants)) {
						return;
					}
				}
			}
		}

		IntList joinVariables = null;
		IntList openVariables = rule.getOpenVariables();
		
		//Then do it for all values
		if (rule.isClosed(true)) {
			joinVariables = rule.getOpenableVariables();
		} else {
			joinVariables = openVariables;
		}
		
		int[] joinPositions = new int[]{0, 2};
			
		super.getDanglingAtoms(rule, newEdge, minCardinality, joinVariables, joinPositions, output);
		
	}
	
	/**
	 * Simplified version that calculates only standard confidence.
	 */
	public void calculateConfidenceMetrics(Rule candidate) {	
		if (candidate.getLength() == 2) {
			List<int[]> antecedent = new ArrayList<int[]>();
			antecedent.addAll(candidate.getTriples().subList(1, candidate.getTriples().size()));
			List<int[]> succedent = new ArrayList<int[]>();
			succedent.addAll(candidate.getTriples().subList(0, 1));
			double denominator = 0.0;
			denominator = (double) kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
			candidate.setBodySize((int)denominator);
		} else {
			super.calculateConfidenceMetrics(candidate);
		}
	}
	
	public boolean testConfidenceThresholds(Rule candidate) {
		if (candidate.getLength() == 2) {
			calculateConfidenceMetrics(candidate);
			return true;
		} else {
			return super.testConfidenceThresholds(candidate);
		}
	}
}
