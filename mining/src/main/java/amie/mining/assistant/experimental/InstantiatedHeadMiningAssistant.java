package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;

public class InstantiatedHeadMiningAssistant extends DefaultMiningAssistant {

	public InstantiatedHeadMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	public String getDescription() {
        return "Counting on one variable. "
        		+ "Head relation is always instantiated in one argument";
	}
	
	@Override
	public Collection<Rule> getInitialAtomsFromSeeds(Collection<ByteString> relations, double minCardinality) {
		Collection<Rule> output = new ArrayList<>();
		Rule query = new Rule();
		//The query must be empty
		if(!query.isEmpty()) {
			throw new IllegalArgumentException("Expected an empty query");
		}
		
		ByteString[] newEdge = query.fullyUnboundTriplePattern();		
		query.getTriples().add(newEdge);
		
		for(ByteString relation: relations) {
			newEdge[1] = relation;
			
			int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(newEdge);
			List<ByteString[]> emptyList = Collections.emptyList();
			long cardinality = kb.countProjection(query.getHead(), emptyList);
			
			ByteString[] succedent = newEdge.clone();
			Rule candidate = new Rule(succedent, cardinality);
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
		Rule query = new Rule();
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		
		if(query.isEmpty()) {
			//Initial case
			query.getTriples().add(newEdge);
			List<ByteString[]> emptyList = Collections.emptyList();
			IntHashMap<ByteString> relations = kb.countProjectionBindings(query.getHead(), emptyList, newEdge[1]);
			for(ByteString relation : relations){
				if(headExcludedRelations != null && headExcludedRelations.contains(relation))
					continue;
				
				int cardinality = relations.get(relation);
				if (cardinality >= minSupportThreshold) {
					ByteString[] succedent = newEdge.clone();
					succedent[1] = relation;
					int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(succedent);
					Rule candidate = new Rule(succedent, cardinality);
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
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		
		if(query.isEmpty()) {
			//Initial case
			query.getTriples().add(newEdge);
			List<ByteString[]> emptyList = Collections.emptyList();
			IntHashMap<ByteString> relations = kb.countProjectionBindings(query.getHead(), emptyList, newEdge[1]);
			for(ByteString relation : relations){
				if(headExcludedRelations != null && headExcludedRelations.contains(relation))
					continue;
				
				int cardinality = relations.get(relation);
				if (cardinality >= minCardinality) {
					ByteString[] succedent = newEdge.clone();
					succedent[1] = relation;
					int countVarPos = countAlwaysOnSubject ? 0 : findCountingVariable(succedent);
					Rule candidate = new Rule(succedent, cardinality);
					candidate.setFunctionalVariablePosition(countVarPos);
					registerHeadRelation(candidate);
					getInstantiatedAtoms(candidate, null, 0, countVarPos == 0 ? 2 : 0, minCardinality, output);
					output.add(candidate);
				}
			}			
			query.getTriples().remove(0);
		} else {
			if (!isNotTooLong(query)) {
				return;
			}
			
			// Enforce this only for n > 2
			if (maxDepth > 2) {
				if(query.getRealLength() == maxDepth - 1) {
					if(!query.getOpenVariables().isEmpty() && (!allowConstants && !enforceConstants)) {
						return;
					}
				}
			}
			
			getDanglingAtoms(query, newEdge, minCardinality, output);
		}
	}
	
	/**
	 * Simplified version that calculates only standard confidence.
	 */
	public void calculateConfidenceMetrics(Rule candidate) {	
		if (candidate.getLength() == 2) {
			List<ByteString[]> antecedent = new ArrayList<ByteString[]>();
			antecedent.addAll(candidate.getTriples().subList(1, candidate.getTriples().size()));
			List<ByteString[]> succedent = new ArrayList<ByteString[]>();
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
