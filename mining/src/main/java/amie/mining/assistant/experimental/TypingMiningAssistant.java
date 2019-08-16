package amie.mining.assistant.experimental;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.MultiMap;
import javatools.datatypes.Pair;
import amie.data.KB;
import amie.data.TransitiveTypesKB;
import amie.data.Schema;
import amie.mining.assistant.*;
import amie.rules.Rule;

public class TypingMiningAssistant extends DefaultMiningAssistant {
	
	public static String topType = "owl:Thing";
	
	public static ByteString topTypeBS = ByteString.of(topType);

	public TypingMiningAssistant(KB dataSource) {
		super(dataSource);
		this.exploitMaxLengthOption = false;
	}
	
	@Override
	protected void buildRelationsDictionary() {
		super.buildRelationsDictionary();
		
		// Build all defined classes
		List<ByteString[]> query = KB.triples(KB.triple("?class", Schema.subClassRelation, "?c"));		
		IntSet types = new IntOpenHashSet(kb.selectDistinct(ByteString.of("?class"), query));
		types.add(topTypeBS);
		for (ByteString type : types) {
			ByteString[] query_c = KB.triple(ByteString.of("?x"), Schema.typeRelationBS, type);
			double relationSize = kb.count(query_c);
			headCardinalities.put(type.toString(), relationSize);
		}
	}
	
	@Override
	public long getHeadCardinality(Rule query){
		ByteString[] head = query.getHead();
		if(head[1].equals(Schema.typeRelationBS)) {
			return headCardinalities.get(head[2].toString()).longValue();
		}
		return super.getHeadCardinality(query);
	}
	
	public Rule getInitialRule() {
		Rule query = new Rule();
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		ByteString[] succedent = newEdge.clone();
		succedent[1] = Schema.typeRelationBS;
		succedent[2] = topTypeBS;
		Rule candidate = new Rule(succedent, (double)kb.countOneVariable(succedent));
		candidate.setId(1);
		candidate.setFunctionalVariablePosition(0);
		registerHeadRelation(candidate);
		return candidate;
	}
	
	@Override
	public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
		return Arrays.asList(getInitialRule());
	}
	
	@Override
	public Collection<Rule> getInitialAtomsFromSeeds(Collection<ByteString> relations, 
			double minSupportThreshold) {
		return Arrays.asList(getInitialRule());
	}
	
	@Override
	public boolean isNotTooLong(Rule rule) {
		return rule.getRealLength() <= rule.getId();
	}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return true;
	}
	
	@Override
	public boolean shouldBeClosed() {
		return false;
	}
	
	
	@Override
	/**
	 * It adds to the output all the rules resulting from adding dangling atom instantiation of "edge"
	 * to the query.
	 * @param query
	 * @param edge
	 * @param minSupportThreshold Minimum support threshold.
	 * @param output
	 */
	protected void getDanglingAtoms(Rule query, ByteString[] edge, double minSupportThreshold, Collection<Rule> output) {
		List<ByteString> joinVariables = null;
		List<ByteString> openVariables = query.getOpenVariables();
		
		//Then do it for all values
		if(query.isClosed(true)) {				
			joinVariables = query.getHeadVariables();
		} else {
			joinVariables = openVariables;
			joinVariables.add(query.getHead()[0]);
		}
		
		int nPatterns = query.getLength();
		
		for(int joinPosition = 0; joinPosition <= 2; joinPosition += 2){			
			for(ByteString joinVariable: joinVariables){
				ByteString[] newEdge = edge.clone();
				
				newEdge[joinPosition] = joinVariable;
				query.getTriples().add(newEdge);
				Int2IntMap promisingRelations = null;
				Rule rewrittenQuery = null;
				if (this.enableQueryRewriting) {
					rewrittenQuery = rewriteProjectionQuery(query, nPatterns, joinPosition == 0 ? 0 : 2);	
				}
				
				if(rewrittenQuery == null){
					long t1 = System.currentTimeMillis();
					promisingRelations = this.kb.countProjectionBindings(query.getHead(), query.getAntecedent(), newEdge[1]);
					long t2 = System.currentTimeMillis();
					if((t2 - t1) > 20000 && this.verbose) {
						System.err.println("countProjectionBindings var=" + newEdge[1] + " "  + query + " has taken " + (t2 - t1) + " ms");
					}
				}else{
					long t1 = System.currentTimeMillis();
					promisingRelations = this.kb.countProjectionBindings(rewrittenQuery.getHead(), rewrittenQuery.getAntecedent(), newEdge[1]);
					long t2 = System.currentTimeMillis();
					if((t2 - t1) > 20000 && this.verbose)
					System.err.println("countProjectionBindings on rewritten query var=" + newEdge[1] + " "  + rewrittenQuery + " has taken " + (t2 - t1) + " ms");						
				}
				
				query.getTriples().remove(nPatterns);								
				// The relations are sorted by support, therefore we can stop once we have reached
				// the minimum support.
				for(ByteString relation: promisingRelations){
					int cardinality = promisingRelations.get(relation);
					
					if (cardinality < minSupportThreshold) {
						continue;
					}			
					
					// Language bias test
					if (query.cardinalityForRelation(relation) >= recursivityLimit) {
						continue;
					}
					
					if (bodyExcludedRelations != null 
							&& bodyExcludedRelations.contains(relation)) {
						continue;
					}
					
					if (bodyTargetRelations != null 
							&& !bodyTargetRelations.contains(relation)) {
						continue;
					}
					
					newEdge[1] = relation;
					//Before adding the edge, verify whether it leads to the hard case
					if(containsHardCase(query, newEdge))
						continue;
					
					Rule candidate = query.addAtom(newEdge, cardinality);
					List<ByteString[]> recursiveAtoms = candidate.getRedundantAtoms();
					if(!recursiveAtoms.isEmpty()){
						continue;
					}
					
					candidate.setHeadCoverage(candidate.getSupport() / getHeadCardinality(candidate));
					candidate.setSupportRatio(candidate.getSupport() / this.kb.size());
					candidate.addParent(query);	
					output.add(candidate);
				}
			}
		}
	}
	
	public void getSubTypingRules(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (!rule.getHeadRelationBS().equals(Schema.typeRelationBS)) {
			return;
		}
		if (rule.getBody().isEmpty()) {
			return;
		}
		ByteString[] head = rule.getHead();
		List<ByteString[]> body = rule.getBody();
		IntSet subTypes = Schema.getSubTypes(kb, head[2]);
		int parentTypePos = Rule.firstIndexOfRelation(body, Schema.typeRelationBS);
		for (ByteString subType : subTypes) {
			Rule succedent = new Rule(rule, rule.getSupport());
			if (parentTypePos == -1) {
				succedent = succedent.addAtom(KB.triple(head[0], Schema.typeRelationBS, head[2]), 0);
			} else {
				succedent.getBody().get(parentTypePos)[2] = head[2];
			}
			succedent.getHead()[2] = subType;
			double cardinality = (double)kb.countDistinct(head[0], succedent.getTriples());
			if (cardinality >= minSupportThreshold) {
				succedent.setSupport(cardinality);
				succedent.setHeadCoverage((double)cardinality / (double)getHeadCardinality(succedent));
				succedent.setSupportRatio((double)cardinality / (double)this.kb.size());
				succedent.setId(rule.getId()+1);
				output.add(succedent);
			}
		}
	}
	
	public void getDomainRangeRules(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (!rule.getHeadRelationBS().equals(Schema.typeRelationBS)) {
			return;
		}
		
		if (rule.getId() < 6)
			return;
		
		List<ByteString> openVariables = rule.getOpenVariables();
		if (openVariables.isEmpty()) {
			return;
		}
		if (Rule.firstIndexOfRelation(rule.getBody(), Schema.typeRelationBS) == -1) {
			return;
		}
		int cardinality;
		for (ByteString openVariable : openVariables) {
			ByteString[] newEdge = rule.fullyUnboundTriplePattern();
			newEdge[0] = openVariable;
			newEdge[1] = Schema.typeRelationBS;
			
			Rule pattern = rule.addAtom(newEdge, 0);
			Int2IntMap promisingTypes = kb.frequentBindingsOf(newEdge[2], pattern.getFunctionalVariable(), pattern.getTriples());
			for (ByteString promisingType : promisingTypes) {
				cardinality = promisingTypes.get(promisingType);
				if (cardinality >= minSupportThreshold) {
					newEdge[2] = promisingType;
					Rule candidate = rule.addAtom(newEdge, cardinality);
					candidate.setHeadCoverage((double)cardinality / (double)getHeadCardinality(candidate));
					candidate.setSupportRatio((double)cardinality / (double)this.kb.size());
					candidate.addParent(rule);
					candidate.setFinal();
					output.add(candidate);
				}
			}
		}
	}
	
	@Override
	public boolean testConfidenceThresholds(Rule candidate) {
		
		if(candidate.containsLevel2RedundantSubgraphs()) {
			return false;
		}	
		
		if(candidate.getStdConfidence() >= minStdConfidence){
			//Now check the confidence with respect to its ancestors
			List<Rule> ancestors = candidate.getAncestors();			
			for(int i = 0; i < ancestors.size(); ++i){
				double ancestorConfidence = ancestors.get(i).getStdConfidence();
				// Skyline technique on PCA confidence					
				if ((ancestors.get(i).getLength() > 1) && 
					  (ancestorConfidence > candidate.getStdConfidence())) {
					return false;
				}
			}
		}else{
			return false;
		}
		
		return true;
	}
	
	//public static ConcurrentHashMap<queryCache, Boolean> instantiationsCache = new ConcurrentHashMap<>(10000, (float)0.75, 40);
	
	public void getInstantiatedAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (!canAddInstantiatedAtoms()) {
			return;
		}
		
		int lastTriplePatternIndex = rule.getLastNotTypeTriplePatternIndex();
		if (lastTriplePatternIndex == -1)
			return;
		ByteString[] lastTriplePattern = rule.getTriples().get(lastTriplePatternIndex);
		
		List<ByteString> openVariables = rule.getOpenVariables();
		int danglingPosition = 0;
		if (openVariables.contains(lastTriplePattern[0])) {
			danglingPosition = 0;
		} else if (openVariables.contains(lastTriplePattern[2])) {
			danglingPosition = 2;
		} else {
			return;
		}
		
		//queryCache qC = new queryCache(rule.getTriples(), Arrays.asList(lastTriplePattern[danglingPosition]));
		//if(instantiationsCache.containsKey(qC)) {
		//	return;
		//}
		//instantiationsCache.put(qC, true);
		
		getInstantiatedAtoms(rule, rule, lastTriplePatternIndex, danglingPosition,
					minSupportThreshold, output);
	}
	
	@Override
	public void applyMiningOperators(Rule rule, double minSupportThreshold,
			Collection<Rule> danglingOutput, Collection<Rule> output) {
		// System.err.println("Candidate: " + rule.getRuleString());
		// System.err.println("refined ?");
		if (!rule.isPerfect()) {
		//	System.err.println("yes");
			getDanglingAtoms(rule, minSupportThreshold, output);
			getClosingAtoms(rule, minSupportThreshold, output);
			getInstantiatedAtoms(rule, minSupportThreshold, output);
			getDomainRangeRules(rule, minSupportThreshold, output);
		}
		getSubTypingRules(rule, minSupportThreshold, output);
	}

	public static void main(String[] args) {
		KB kb = new KB();
		List<File> files = new ArrayList<>();
		for (String arg : args) {
			files.add(new File(arg));
		}
		try {
			kb.load(files);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TypingMiningAssistant assistant = new TypingMiningAssistant(kb);
		assistant.allowConstants = true;
		Rule newRule;
		List<Rule> output = new ArrayList<>();
		Rule initialRule = assistant.getInitialRule();
		System.out.println("Initial rule: " + initialRule.getRuleString());
		
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(initialRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		output.clear();
		assistant.getDanglingAtoms(initialRule, -1, output);
		assert(!output.isEmpty());
		newRule = output.get(1);
		System.out.println("New rule: " + newRule.getRuleString());
		output.clear();
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		assert(!output.isEmpty());
		newRule = output.get(22);
		System.out.println("New rule: " + newRule.getRuleString());
		output.clear();
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		newRule = output.get(1);
		assistant.getHeadCardinality(newRule);
		
		System.out.println("New rule: " + newRule.getRuleString());
		long support = kb.countDistinct(ByteString.of("?x"), newRule.getTriples());
		System.out.println("Support: " + String.valueOf(support));
		System.out.println("MRT calls: " + String.valueOf(KB.STAT_NUMBER_OF_CALL_TO_MRT.get()));
		
		output.clear();
		System.out.println("SubTyping rules:");
		//assistant.getInstantiatedAtoms(newRule, -1, output);
		assistant.getSubTypingRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		System.out.println("New rule: " + newRule.getRuleString());
		output.clear();
		System.out.println("DomainRange rules:");
		assistant.getDomainRangeRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
	}	
}
