package amie.mining.assistant.wikilinks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import amie.data.KB;
import amie.data.tuple.IntArrays;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;

public class WikilinksHeadVariablesMiningAssistant extends DefaultMiningAssistant {
	
	public static String wikiLinkProperty = "<linksTo>";
	
	public WikilinksHeadVariablesMiningAssistant(KB dataSource) {
		super(dataSource);
        headExcludedRelations = IntArrays.asList(KB.map(WikilinksHeadVariablesMiningAssistant.wikiLinkProperty), 
        		KB.map("rdf:type"));
        bodyExcludedRelations = headExcludedRelations;
	}
	
	public String getDescription() {
		return "Rules of the form .... linksTo(x, y) "
				+ "type(x, C) type(y, C') => r(x, y)";
	}
	
	@Override
	public void setHeadExcludedRelations(IntCollection headExcludedRelations) {};
	
	@Override
	public void setBodyExcludedRelations(IntCollection excludedRelations) {};
	
	@Override
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {		
		if (query.isEmpty()) {
			//Initial case
			int[] newEdge = query.fullyUnboundTriplePattern();
			query.getTriples().add(newEdge);
			List<int[]> emptyList = Collections.emptyList();
			Int2IntMap relations = kb.countProjectionBindings(query.getHead(), emptyList, newEdge[1]);
			for(int relation: relations.keySet()){
				// Language bias test
				if (query.cardinalityForRelation(relation) >= recursivityLimit) {
					continue;
				}
				
				if(headExcludedRelations != null && 
						headExcludedRelations.contains(relation)) {
					continue;
				}
				
				int cardinality = relations.get(relation);
				if(cardinality >= minCardinality){
					int[] succedent = newEdge.clone();
					succedent[1] = relation;
					int countVarPos = countAlwaysOnSubject? 0 : findCountingVariable(succedent);
					Rule candidate = new Rule(succedent, cardinality);
					candidate.setFunctionalVariablePosition(countVarPos);
					registerHeadRelation(candidate);
					output.add(candidate);
				}
			}			
			query.getTriples().remove(0);
		} else {
			super.getDanglingAtoms(query, minCardinality, output);
		}
	}
	
	@Override
	public void getTypeSpecializedAtoms(Rule query, double minSupportThreshold, Collection<Rule> output) {
		if (query.containsRelation(typeString))
			return;
		
		List<Rule> tmpCandidates = new ArrayList<Rule>();
		int[] head = query.getHead();
		
		//Specialization by type
		if(KB.isVariable(head[0])){
			int[] newEdge = query.fullyUnboundTriplePattern();
			newEdge[0] = head[0];
			newEdge[1] = typeString;				
			query.getTriples().add(newEdge);
			Int2IntMap subjectTypes = kb.countProjectionBindings(query.getHead(), 
					query.getAntecedent(), newEdge[2]);
			if(!subjectTypes.isEmpty()){
				for(int type: subjectTypes.keySet()){
					int cardinality = subjectTypes.get(type);
					if(cardinality >= minSupportThreshold){
						Rule newCandidate = new Rule(query, cardinality);
						newCandidate.getLastTriplePattern()[2] = type;
						tmpCandidates.add(newCandidate);
					}
				}
			}
			
			query.getTriples().remove(query.getTriples().size() - 1);
			//tmpCandidates.add(query);
		}
		
		if(KB.isVariable(head[2])){
			for(Rule candidate: tmpCandidates){
				int[] newEdge = query.fullyUnboundTriplePattern();
				newEdge[0] = head[2];
				newEdge[1] = typeString;
				candidate.getTriples().add(newEdge);
				Int2IntMap objectTypes = kb.countProjectionBindings(candidate.getHead(), candidate.getAntecedent(), newEdge[2]);
				for(int type: objectTypes.keySet()){
					int cardinality = objectTypes.get(type);
					if(cardinality >= minSupportThreshold){
						Rule newCandidate = new Rule(candidate, cardinality);
						newCandidate.setHeadCoverage((double)cardinality 
								/ (double)headCardinalities.get(newCandidate.getHeadRelationBS()));
						newCandidate.setSupportRatio((double)cardinality / (double)kb.size());
						newCandidate.addParent(query);
						newCandidate.getLastTriplePattern()[2] = type;
						newCandidate.addParent(query);
						output.add(newCandidate);
					}
				}
				
				/**if (candidate != query) {
					output.add(candidate);
					candidate.addParent(query);
				}**/
				candidate.getTriples().remove(candidate.getTriples().size() - 1);
			}
		}
	}
	
	@Override
	public void getClosingAtoms(Rule query, double minSupportThreshold, Collection<Rule> output) {
		int length = query.getLengthWithoutTypesAndLinksTo(typeString, KB.map(wikiLinkProperty));
		int[] head = query.getHead();
		if (length == maxDepth - 1) {
			IntList openVariables = query.getOpenVariables();
			for (int openVar : openVariables) {
				if (KB.isVariable(head[0]) && openVar != head[0]) {
					return;
				}
				
				if (KB.isVariable(head[2]) && openVar != head[2]) {
					return;
				}
			}
		}
		
		if (!query.containsRelation(KB.map(wikiLinkProperty))) {
			int[] newEdge = head.clone();
			newEdge[1] = KB.map(wikiLinkProperty);
			List<int[]> queryAtoms = new ArrayList<>();
			queryAtoms.addAll(query.getTriples());
			queryAtoms.add(newEdge);
			long cardinality = kb.countDistinctPairs(head[0], head[2], queryAtoms);
			if (cardinality >= minSupportThreshold) {
				Rule candidate1 = query.addAtom(newEdge, (int)cardinality);
				candidate1.setHeadCoverage((double)cardinality / (double)headCardinalities.get(candidate1.getHeadRelationBS()));
				candidate1.setSupportRatio((double)cardinality / (double)kb.size());
				candidate1.addParent(query);			
				output.add(candidate1);	
			}
			
			int tmp = newEdge[0];
			newEdge[0] = newEdge[2];
			newEdge[2] = tmp;
			cardinality = kb.countDistinctPairs(head[0], head[2], queryAtoms);
			if (cardinality >= minSupportThreshold) {
				Rule candidate2 = query.addAtom(newEdge, (int)cardinality);
				candidate2.setHeadCoverage((double)cardinality / (double)headCardinalities.get(candidate2.getHeadRelationBS()));
				candidate2.setSupportRatio((double)cardinality / (double)kb.size());
				candidate2.addParent(query);			
				output.add(candidate2);	
			}
		} else {
			super.getClosingAtoms(query, minSupportThreshold, output);
		}
	}

	protected boolean isNotTooLong(Rule candidate){
		return candidate.getLengthWithoutTypesAndLinksTo(typeString, KB.map(wikiLinkProperty)) < maxDepth;
	}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return candidate.isClosed(true) 
				&& candidate.containsRelation(typeString)
				&& candidate.containsRelation(KB.map(wikiLinkProperty));
	}	
}
