package amie.mining.assistant.experimental;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import amie.data.KB;
import amie.data.Schema;
import amie.mining.assistant.*;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;

public class TypingMiningAssistant extends DefaultMiningAssistant {
	
	public static String topType = "owl:Thing";
	
	public static int topTypeBS = KB.map(topType);

	public TypingMiningAssistant(KB dataSource) {
		super(dataSource);
		this.exploitMaxLengthOption = false;
	}
	
	@Override
	protected void buildRelationsDictionary() {
		super.buildRelationsDictionary();
		
		// Build all defined classes
		List<int[]> query = KB.triples(KB.triple("?c9", Schema.subClassRelation, "?c"));		
		IntSet types = new IntOpenHashSet(kb.selectDistinct(KB.map("?c9"), query));
		types.add(topTypeBS);
		for (int type : types) {
			int[] query_c = KB.triple(KB.map("?x"), Schema.typeRelationBS, type);
			double relationSize = kb.count(query_c);
			headCardinalities.put(type, relationSize);
		}
	}
	
	@Override
	public long getHeadCardinality(Rule query){
		int[] head = query.getHead();
		if(head[1] == Schema.typeRelationBS) {
			return (long) headCardinalities.get(head[2]);
		}
		return super.getHeadCardinality(query);
	}
	
	public Rule getInitialRule() {
		Rule query = new Rule();
		int[] newEdge = query.fullyUnboundTriplePattern();
		int[] succedent = newEdge.clone();
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
	public Collection<Rule> getInitialAtomsFromSeeds(IntCollection relations, 
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
	

		/**
	 * Returns all candidates obtained by adding a new triple pattern to the query
	 * @param rule and will therefore predict too many new facts with scarce evidence, 
	 * @param minSupportThreshold
	 * @param output
	 */
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {		
		int[] newEdge = rule.fullyUnboundTriplePattern();
		
		if (rule.isEmpty()) {
			throw new IllegalArgumentException("This method expects a non-empty query");
		}
	
	
		if (!isNotTooLong(rule))
			return;
					
		// Pruning by maximum length for the \mathcal{O}_D operator.
		if(rule.getRealLength() == this.maxDepth - 1) {
			if (this.exploitMaxLengthOption) {
				if(!rule.getOpenVariables().isEmpty() 
						&& !this.allowConstants 
						&& !this.enforceConstants) {
					return;
				}
			}
		}

		IntList joinVariables = null;
		IntList openVariables = rule.getOpenVariables();
		
		//Then do it for all values
		if (rule.isClosed(true)) {				
			joinVariables = rule.getHeadVariables();
		} else {
			joinVariables = openVariables;
			joinVariables.add(rule.getHead()[0]);
		}
		
		int[] joinPositions = new int[]{0, 2};

		super.getDanglingAtoms(rule, newEdge, minSupportThreshold, joinVariables, joinPositions, output);
	}
	
	public void getSubTypingRules(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (rule.getHeadRelationBS() != Schema.typeRelationBS) {
			return;
		}
		if (rule.getBody().isEmpty()) {
			return;
		}
		int[] head = rule.getHead();
		List<int[]> body = rule.getBody();
		IntSet subTypes = Schema.getSubTypes(kb, head[2]);
		int parentTypePos = Rule.firstIndexOfRelation(body, Schema.typeRelationBS);
		for (int subType : subTypes) {
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
		if (rule.getHeadRelationBS() != Schema.typeRelationBS) {
			return;
		}
		
		if (rule.getId() < 6)
			return;
		
		IntList openVariables = rule.getOpenVariables();
		if (openVariables.isEmpty()) {
			return;
		}
		if (Rule.firstIndexOfRelation(rule.getBody(), Schema.typeRelationBS) == -1) {
			return;
		}
		int cardinality;
		for (int openVariable : openVariables) {
			int[] newEdge = rule.fullyUnboundTriplePattern();
			newEdge[0] = openVariable;
			newEdge[1] = Schema.typeRelationBS;
			
			Rule pattern = rule.addAtom(newEdge, 0);
			Int2IntMap promisingTypes = kb.frequentBindingsOf(newEdge[2], pattern.getFunctionalVariable(), pattern.getTriples());
			for (int promisingType : promisingTypes.keySet()) {
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
			Set<Rule> ancestors = candidate.getAncestors();
			for(Rule ancestor : ancestors){
				double ancestorConfidence = ancestor.getStdConfidence();
				// Skyline technique on PCA confidence					
				if ((ancestor.getLength() > 1) &&
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
		int[] lastTriplePattern = rule.getTriples().get(lastTriplePatternIndex);
		
		IntList openVariables = rule.getOpenVariables();
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
		long support = kb.countDistinct(KB.map("?x"), newRule.getTriples());
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
