package amie.mining.assistant.experimental;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import amie.data.KB;
import amie.mining.assistant.*;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Set;

public class TypingMiningAssistantWithTT extends DefaultMiningAssistant {
	
	public static String topType = "owl:Thing";
	
	public static int topTypeBS = KB.map(topType);

	public TypingMiningAssistantWithTT(KB dataSource) {
		super(dataSource);
		System.out.println("Materializing taxonomy...");
		amie.data.Schema.materializeTaxonomy(dataSource);
		this.exploitMaxLengthOption = false;
	}
	
	public Rule getInitialRule() {
		Rule query = new Rule();
		int[] newEdge = query.fullyUnboundTriplePattern();
		int[] succedent = newEdge.clone();
		succedent[1] = KB.TRANSITIVETYPEbs;
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
	
	public void getSubTypingRules(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (rule.getHeadRelationBS() != KB.TRANSITIVETYPEbs) {
			return;
		}
		if (rule.getBody().isEmpty()) {
			return;
		}
		int[] head = rule.getHead();
		List<int[]> body = rule.getBody();
		IntSet subTypes = amie.data.Schema.getSubTypes(kb, head[2]);
		int parentTypePos = Rule.firstIndexOfRelation(body, KB.TRANSITIVETYPEbs);
		for (int subType : subTypes) {
			Rule succedent = new Rule(rule, rule.getSupport());
			if (parentTypePos == -1) {
				succedent = succedent.addAtom(KB.triple(head[0], KB.TRANSITIVETYPEbs, head[2]), 0);
			} else {
				succedent.getBody().get(parentTypePos)[2] = head[2];
			}
			succedent.getHead()[2] = subType;
			double cardinality = (double)kb.countDistinct(head[0], succedent.getTriples());
			if (cardinality >= minSupportThreshold) {
				succedent.setSupport(cardinality);
				succedent.setSupportRatio((double)cardinality / (double)this.kb.size());
				succedent.setId(rule.getId()+1);
				output.add(succedent);
			}
		}
	}
	
	public void getDomainRangeRules(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (rule.getHeadRelationBS() != KB.TRANSITIVETYPEbs) {
			return;
		}
		IntList openVariables = rule.getOpenVariables();
		if (openVariables.isEmpty()) {
			return;
		}
		if (Rule.firstIndexOfRelation(rule.getBody(), KB.TRANSITIVETYPEbs) == -1) {
			return;
		}
		int cardinality;
		for (int openVariable : openVariables) {
			int[] newEdge = rule.fullyUnboundTriplePattern();
			newEdge[0] = openVariable;
			newEdge[1] = KB.TRANSITIVETYPEbs;
			
			Rule pattern = rule.addAtom(newEdge, 0);
			Int2IntMap promisingTypes = kb.frequentBindingsOf(newEdge[2], pattern.getFunctionalVariable(), pattern.getTriples());
			for (int promisingType : promisingTypes.keySet()) {
				cardinality = promisingTypes.get(promisingType);
				if (cardinality >= minSupportThreshold) {
					newEdge[2] = promisingType;
					Rule candidate = rule.addAtom(newEdge, cardinality);
					candidate.addParent(rule);
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
					  (ancestorConfidence >= candidate.getStdConfidence())) {
					return false;
				}
			}
		}else{
			return false;
		}
		
		return true;
	}
	
	@Override
	public void applyMiningOperators(Rule rule, double minSupportThreshold,
			Collection<Rule> danglingOutput, Collection<Rule> output) {
		System.err.println("Candidate: " + rule.getRuleString());
		// System.err.println("refined ?");
		if (!rule.isPerfect()) {
		//	System.err.println("yes");
			super.applyMiningOperators(rule, minSupportThreshold, danglingOutput, output);
		}
		getSubTypingRules(rule, minSupportThreshold, output);
		//getDomainRangeRules(rule, minSupportThreshold, output);
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
		TypingMiningAssistantWithTT assistant = new TypingMiningAssistantWithTT(kb);
		Rule newRule;
		/*Rule initialRule = assistant.getInitialRule();
		System.out.println("Initial rule: " + initialRule.getRuleString());
		List<Rule> output = new ArrayList<>();
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(initialRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		output.clear();
		assistant.getDanglingAtoms(initialRule, -1, output);
		assert(!output.isEmpty());
		newRule = output.get(0);
		System.out.println("New rule: " + newRule.getRuleString());
		output.clear();
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}
		assert(!output.isEmpty());
		newRule = output.get(0);
		System.out.println("New rule: " + newRule.getRuleString());
		output.clear();
		System.out.println("SubTyping rules:");
		assistant.getSubTypingRules(newRule, -1, output);
		for (Rule rule : output) {
			System.out.println(rule.getRuleString() + "\t" + String.valueOf(rule.getStdConfidence()));
		}*/
		newRule = new Rule(KB.triple(KB.map("?x"), KB.TRANSITIVETYPEbs, KB.map("<wordnet_abstraction_100002137>")),
							KB.triples(KB.triple(KB.map("?x"), KB.map("<isMarriedTo>"), KB.map("?y")),
									   KB.triple(KB.map("?x"), KB.TRANSITIVETYPEbs, topTypeBS)), 0);
		System.out.println("New rule: " + newRule.getRuleString());
		long support = kb.countDistinct(KB.map("?x"), newRule.getTriples());
		System.out.println("Support: " + String.valueOf(support));
		System.out.println("MRT calls: " + String.valueOf(KB.STAT_NUMBER_OF_CALL_TO_MRT.get()));
		/*
		output.clear();
		System.out.println("SubTyping rules:");
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
		}*/
	}	
}
