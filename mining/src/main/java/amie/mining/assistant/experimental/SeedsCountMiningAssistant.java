package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import amie.data.KB;
import amie.data.tuple.IntPair;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.ConfidenceMetric;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class SeedsCountMiningAssistant extends MiningAssistant {

	protected long subjectSchemaCount;
	
	private IntSet allSubjects;
	
	public SeedsCountMiningAssistant(KB dataSource, KB schemaSource) {
		super(dataSource);
		this.kbSchema = schemaSource;
		int[] rootPattern = Rule.fullyUnboundTriplePattern1();
		List<int[]> triples = new ArrayList<int[]>();
		triples.add(rootPattern);
		allSubjects = this.kbSchema.selectDistinct(rootPattern[0], triples);
		subjectSchemaCount = allSubjects.size();
		confidenceMetric = ConfidenceMetric.StandardConfidence;
	}
	
	public long getTotalCount(Rule candidate){
		return subjectSchemaCount;
	}

	protected void getInstantiatedAtoms(Rule query, Rule originalQuery, int[] danglingEdge, 
			int danglingPosition, double minSupportThreshold, Collection<Rule> output) {
		Int2IntMap constants = kb.frequentBindingsOf(danglingEdge[danglingPosition], query.getFunctionalVariable(), query.getTriples());
		for(int constant: constants.keySet()){
			int tmp = danglingEdge[danglingPosition];
			danglingEdge[danglingPosition] = constant;
			int cardinality = seedsCardinality(query);
			danglingEdge[danglingPosition] = tmp;
			if(cardinality >= minSupportThreshold){
				int[] lastPatternCopy = query.getLastTriplePattern().clone();
				lastPatternCopy[danglingPosition] = constant;
				long cardLastEdge = kb.count(lastPatternCopy);
				if(cardLastEdge < 2)
					continue;
				
				Rule candidate = query.instantiateConstant(danglingPosition, constant, cardinality);
				if(candidate.getRedundantAtoms().isEmpty()){
					candidate.setHeadCoverage((double)cardinality / headCardinalities.get(candidate.getHeadRelationBS()));
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
		
		IntList sourceVariables = null;
		IntList allVariables = query.getVariables();
		IntList targetVariables = null;		
		IntList openVariables = query.getOpenVariables();
		
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
		
		IntPair[] varSetups = new IntPair[2];
		varSetups[0] = new IntPair(0, 2);
		varSetups[1] = new IntPair(2, 0);
		int[] newEdge = query.fullyUnboundTriplePattern();
		int relationVariable = newEdge[1];
		
		for(IntPair varSetup: varSetups){			
			int joinPosition = varSetup.first;
			int closeCirclePosition = varSetup.second;
			int joinVariable = newEdge[joinPosition];
			int closeCircleVariable = newEdge[closeCirclePosition];
						
			for(int sourceVariable: sourceVariables){					
				newEdge[joinPosition] = sourceVariable;
				
				for(int variable: targetVariables){
					if(variable != sourceVariable){
						newEdge[closeCirclePosition] = variable;
						
						query.getTriples().add(newEdge);
						Int2IntMap promisingRelations = kb.frequentBindingsOf(newEdge[1], query.getFunctionalVariable(), query.getTriples());
						query.getTriples().remove(nPatterns);
						
						for(int relation: promisingRelations.keySet()){
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
									candidate.setHeadCoverage((double)cardinality / (double)headCardinalities.get(candidate.getHeadRelationBS()));
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
		IntSet subjects = new IntOpenHashSet(
				kb.selectDistinct(query.getFunctionalVariable(), query.getTriples()));
		subjects.retainAll(allSubjects);
		return subjects.size();
	}
	
	@Override
	public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
		Collection<Rule> output = new ArrayList<>();		
		Rule query = new Rule();
		int[] newEdge = query.fullyUnboundTriplePattern();
		query.getTriples().add(newEdge);
		Int2IntMap relations = 
				kb.frequentBindingsOf(newEdge[1], newEdge[0], query.getTriples());
		for (int relation: relations.keySet()) {
			if(headExcludedRelations != null && headExcludedRelations.contains(newEdge[1]))
				continue;

			newEdge[1] = relation;
			int countVarPos = countAlwaysOnSubject? 0 : findCountingVariable(newEdge);
			query.setFunctionalVariablePosition(countVarPos);		
			double cardinality = (double) seedsCardinality(query);
			query.setSupport(cardinality);
			if(cardinality >= minSupportThreshold) {
				int[] succedent = newEdge.clone();					
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
		int[] newEdge = query.fullyUnboundTriplePattern();
		IntList openVariables = query.getOpenVariables();
		
		//General case
		if(!isNotTooLong(query))
			return;
		
		//General case
		if(query.getLength() == maxDepth - 1) {
			if (this.exploitMaxLengthOption) {
				if (query.getOpenVariables().size() > 1) {
					// There will be more than 2 open variables and we will not be able to close all of them.
					return;
				}

				if (!canAddInstantiatedAtoms()) {
					// We can't count on instantiation operator to close the new dangling variable.
					return;
				}
			}
		}
		
		IntList joinVariables = null;
		
		//Then do it for all values
		if(query.isClosed(true)){
			joinVariables = query.getVariables();
		}else{
			joinVariables = query.getOpenVariables();
		}

		int nPatterns = query.getTriples().size();
		int originalRelationVariable = newEdge[1];		
		
		for(int joinPosition = 0; joinPosition <= 2; joinPosition += 2){
			int originalFreshVariable = newEdge[joinPosition];
			
			for(int joinVariable: joinVariables){					
				newEdge[joinPosition] = joinVariable;
				query.getTriples().add(newEdge);
				Int2IntMap promisingRelations = kb.frequentBindingsOf(newEdge[1], query.getFunctionalVariable(), query.getTriples());
				query.getTriples().remove(nPatterns);
				
				int danglingPosition = (joinPosition == 0 ? 2 : 0);
				boolean boundHead = !KB.isVariable(query.getTriples().get(0)[danglingPosition]);
				for(int relation: promisingRelations.keySet()){
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
						
						candidate.setHeadCoverage((double)candidate.getSupport() / headCardinalities.get(candidate.getHeadRelationBS()));
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
		List<int[]> antecedent = new ArrayList<int[]>();
		antecedent.addAll(candidate.getTriples().subList(1, candidate.getTriples().size()));
		List<int[]> succedent = new ArrayList<int[]>();
		succedent.addAll(candidate.getTriples().subList(0, 1));
		long denominator = 0;
		long pcaDenominator = 0;
		int[] existentialTriple = succedent.get(0).clone();
		int freeVarPos = 0;
		
		if(KB.numVariables(existentialTriple) == 1){
			freeVarPos = KB.firstVariablePos(existentialTriple);
		}else{
			if(existentialTriple[0] == candidate.getFunctionalVariable())
				freeVarPos = 2;
			else
				freeVarPos = 0;
		}

		existentialTriple[freeVarPos] = KB.map("?x");
				
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