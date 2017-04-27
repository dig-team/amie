package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;
import amie.data.KB;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.ConfidenceMetric;
import amie.rules.Rule;

public class SeedsCountMiningAssistant extends MiningAssistant {

	protected long subjectSchemaCount;
	
	private Set<ByteString> allSubjects;
	
	public SeedsCountMiningAssistant(KB dataSource, KB schemaSource) {
		super(dataSource);
		this.kbSchema = schemaSource;
		ByteString[] rootPattern = Rule.fullyUnboundTriplePattern1();
		List<ByteString[]> triples = new ArrayList<ByteString[]>();
		triples.add(rootPattern);
		allSubjects = this.kbSchema.selectDistinct(rootPattern[0], triples);
		subjectSchemaCount = allSubjects.size();
		confidenceMetric = ConfidenceMetric.StandardConfidence;
	}
	
	public long getTotalCount(Rule candidate){
		return subjectSchemaCount;
	}

	protected void getInstantiatedAtoms(Rule query, Rule originalQuery, ByteString[] danglingEdge, 
			int danglingPosition, double minSupportThreshold, Collection<Rule> output) {
		IntHashMap<ByteString> constants = kb.frequentBindingsOf(danglingEdge[danglingPosition], query.getFunctionalVariable(), query.getTriples());
		for(ByteString constant: constants){
			ByteString tmp = danglingEdge[danglingPosition];
			danglingEdge[danglingPosition] = constant;
			int cardinality = seedsCardinality(query);
			danglingEdge[danglingPosition] = tmp;
			if(cardinality >= minSupportThreshold){
				ByteString[] lastPatternCopy = query.getLastTriplePattern().clone();
				lastPatternCopy[danglingPosition] = constant;
				long cardLastEdge = kb.count(lastPatternCopy);
				if(cardLastEdge < 2)
					continue;
				
				Rule candidate = query.instantiateConstant(danglingPosition, constant, cardinality);
				if(candidate.getRedundantAtoms().isEmpty()){
					candidate.setHeadCoverage((double)cardinality / headCardinalities.get(candidate.getHeadRelation()));
					candidate.setSupportRatio((double)cardinality / (double)getTotalCount(candidate));
					candidate.addParent(originalQuery);					
					output.add(candidate);
				}
			}
		}
	}
	
	/**
	 * Returns all candidates obtained by binding two values
	 * @param currentNode
	 * @param minSupportThreshold
	 * @param omittedVariables
	 * @return
	 */
	@MiningOperator(name="closing")
	public void getClosingAtoms(Rule query, double minSupportThreshold, Collection<Rule> output){		
		int nPatterns = query.getTriples().size();

		if(query.isEmpty())
			return;
		
		if(!isNotTooLong(query))
			return;
		
		List<ByteString> sourceVariables = null;
		List<ByteString> allVariables = query.getVariables();
		List<ByteString> targetVariables = null;		
		List<ByteString> openVariables = query.getOpenVariables();
		
		if(query.isClosed(true)){
			sourceVariables = allVariables;
			targetVariables = allVariables;
		}else{
			sourceVariables = openVariables; 
			if(sourceVariables.size() > 1){
				if (this.exploitMaxLengthOption) {
					// Pruning by maximum length for the \mathcal{O}_C operator.
					if (sourceVariables.size() > 2 
							&& query.getRealLength() == this.maxDepth - 1) {
						return;
					}
				}
				//Give preference to the non-closed variables
				targetVariables = sourceVariables;
			}else{
				targetVariables = allVariables;
			}
		}
		
		Pair<Integer, Integer>[] varSetups = new Pair[2];
		varSetups[0] = new Pair<Integer, Integer>(0, 2);
		varSetups[1] = new Pair<Integer, Integer>(2, 0);
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		ByteString relationVariable = newEdge[1];
		
		for(Pair<Integer, Integer> varSetup: varSetups){			
			int joinPosition = varSetup.first.intValue();
			int closeCirclePosition = varSetup.second.intValue();
			ByteString joinVariable = newEdge[joinPosition];
			ByteString closeCircleVariable = newEdge[closeCirclePosition];
						
			for(ByteString sourceVariable: sourceVariables){					
				newEdge[joinPosition] = sourceVariable;
				
				for(ByteString variable: targetVariables){
					if(!variable.equals(sourceVariable)){
						newEdge[closeCirclePosition] = variable;
						
						query.getTriples().add(newEdge);
						IntHashMap<ByteString> promisingRelations = kb.frequentBindingsOf(newEdge[1], query.getFunctionalVariable(), query.getTriples());
						query.getTriples().remove(nPatterns);
						
						for(ByteString relation: promisingRelations){
							if(bodyExcludedRelations != null && bodyExcludedRelations.contains(relation))
								continue;
							
							//Here we still have to make a redundancy check
							newEdge[1] = relation;
							query.getTriples().add(newEdge);
							int cardinality = seedsCardinality(query);
							query.getTriples().remove(nPatterns);
							if(cardinality >= minSupportThreshold){										
								Rule candidate = query.addAtom(newEdge, cardinality);
								if(!candidate.isRedundantRecursive()){
									candidate.setHeadCoverage((double)cardinality / (double)headCardinalities.get(candidate.getHeadRelation()));
									candidate.setSupportRatio((double)cardinality / (double)getTotalCount(candidate));
									candidate.addParent(query);
									output.add(candidate);
								}
							}
						}
					}
					newEdge[1] = relationVariable;
				}
				newEdge[closeCirclePosition] = closeCircleVariable;
				newEdge[joinPosition] = joinVariable;
			}
		}
	}
	
	private int seedsCardinality(Rule query) {
		// TODO Auto-generated method stub
		Set<ByteString> subjects = new HashSet<ByteString>(
				kb.selectDistinct(query.getFunctionalVariable(), query.getTriples()));
		subjects.retainAll(allSubjects);
		return subjects.size();
	}
	
	@Override
	public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
		Collection<Rule> output = new ArrayList<>();		
		Rule query = new Rule();
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		query.getTriples().add(newEdge);
		IntHashMap<ByteString> relations = 
				kb.frequentBindingsOf(newEdge[1], newEdge[0], query.getTriples());
		for (ByteString relation: relations) {
			if(headExcludedRelations != null && headExcludedRelations.contains(newEdge[1]))
				continue;

			newEdge[1] = relation;
			int countVarPos = countAlwaysOnSubject? 0 : findCountingVariable(newEdge);
			query.setFunctionalVariablePosition(countVarPos);		
			double cardinality = (double) seedsCardinality(query);
			query.setSupport(cardinality);
			if(cardinality >= minSupportThreshold) {
				ByteString[] succedent = newEdge.clone();					
				Rule candidate = new Rule(succedent, cardinality);
				candidate.setFunctionalVariablePosition(countVarPos);
				registerHeadRelation(candidate);
				if(allowConstants)
					getInstantiatedAtoms(candidate, null, candidate.getLastTriplePattern(), countVarPos == 0 ? 2 : 0, minSupportThreshold, output);
				
				output.add(candidate);
			}
		}
		return output;
	}

	@Override
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output){		
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		List<ByteString> openVariables = query.getOpenVariables();
		
		//General case
		if(!isNotTooLong(query))
			return;
		
		//General case
		if(query.getLength() == maxDepth - 1) {
			if (this.exploitMaxLengthOption) {
				if(!openVariables.isEmpty() 
						&& !this.allowConstants 
						&& !this.enforceConstants) {
					return;
				}
			}
		}
		
		List<ByteString> joinVariables = null;
		
		//Then do it for all values
		if(query.isClosed(true)){
			joinVariables = query.getVariables();
		}else{
			joinVariables = query.getOpenVariables();
		}

		int nPatterns = query.getTriples().size();
		ByteString originalRelationVariable = newEdge[1];		
		
		for(int joinPosition = 0; joinPosition <= 2; joinPosition += 2){
			ByteString originalFreshVariable = newEdge[joinPosition];
			
			for(ByteString joinVariable: joinVariables){					
				newEdge[joinPosition] = joinVariable;
				query.getTriples().add(newEdge);
				IntHashMap<ByteString> promisingRelations = kb.frequentBindingsOf(newEdge[1], query.getFunctionalVariable(), query.getTriples());
				query.getTriples().remove(nPatterns);
				
				int danglingPosition = (joinPosition == 0 ? 2 : 0);
				boolean boundHead = !KB.isVariable(query.getTriples().get(0)[danglingPosition]);
				for(ByteString relation: promisingRelations){
					if(bodyExcludedRelations != null && bodyExcludedRelations.contains(relation))
						continue;
					//Here we still have to make a redundancy check		
					newEdge[1] = relation;
					query.getTriples().add(newEdge);
					int cardinality = seedsCardinality(query);
					query.getTriples().remove(nPatterns);						
					if(cardinality >= minCardinality){
						Rule candidate = query.addAtom(newEdge, cardinality, newEdge[joinPosition], newEdge[danglingPosition]);
						if(candidate.containsUnifiablePatterns()){
							//Verify whether dangling variable unifies to a single value (I do not like this hack)
							if(boundHead && kb.countDistinct(newEdge[danglingPosition], candidate.getTriples()) < 2)
								continue;
						}
						
						candidate.setHeadCoverage((double)candidate.getSupport() / headCardinalities.get(candidate.getHeadRelation()));
						candidate.setSupportRatio((double)candidate.getSupport() / (double)getTotalCount(candidate));
						candidate.addParent(query);
						if (canAddInstantiatedAtoms()) {
							// Pruning by maximum length for the \mathcal{O}_E operator.
							if (this.exploitMaxLengthOption) {
								if (query.getRealLength() < this.maxDepth - 1 
										|| openVariables.size() < 2) {
									getInstantiatedAtoms(candidate, candidate, candidate.getLastTriplePattern(), danglingPosition, minCardinality, output);
								}
							} else {
								getInstantiatedAtoms(candidate, candidate, candidate.getLastTriplePattern(), danglingPosition, minCardinality, output);							
							}
						}
						
						output.add(candidate);
					}
				}
				
				newEdge[1] = originalRelationVariable;
			}
			newEdge[joinPosition] = originalFreshVariable;
		}
	}
	
	@Override
	public void calculateConfidenceMetrics(Rule candidate) {		
		// TODO Auto-generated method stub
		List<ByteString[]> antecedent = new ArrayList<ByteString[]>();
		antecedent.addAll(candidate.getTriples().subList(1, candidate.getTriples().size()));
		List<ByteString[]> succedent = new ArrayList<ByteString[]>();
		succedent.addAll(candidate.getTriples().subList(0, 1));
		long denominator = 0;
		long pcaDenominator = 0;
		ByteString[] existentialTriple = succedent.get(0).clone();
		int freeVarPos = 0;
		
		if(KB.numVariables(existentialTriple) == 1){
			freeVarPos = KB.firstVariablePos(existentialTriple);
		}else{
			if(existentialTriple[0].equals(candidate.getFunctionalVariable()))
				freeVarPos = 2;
			else
				freeVarPos = 0;
		}

		existentialTriple[freeVarPos] = ByteString.of("?x");
				
		if (!antecedent.isEmpty()) {			
			try{
				denominator = this.kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
				candidate.setBodySize(denominator);
			}catch(UnsupportedOperationException e){
				
			}
			
			//Improved confidence: Add an existential version of the head
			antecedent.add(existentialTriple);
			try{
				pcaDenominator = this.kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
				candidate.setPcaBodySize(pcaDenominator);
			}catch(UnsupportedOperationException e){
				
			}
			antecedent.remove(antecedent.size() - 1);
		}
	}
}