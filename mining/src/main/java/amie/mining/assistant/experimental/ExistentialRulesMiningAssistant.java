package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntList;

public class ExistentialRulesMiningAssistant extends DefaultMiningAssistant {

	public ExistentialRulesMiningAssistant(KB dataSource) {
		super(dataSource);
		headCardinalities.put(KB.EXISTSbs, -1.0);
		headCardinalities.put(KB.EXISTSINVbs, -1.0);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public long getHeadCardinality(Rule query){
		if (query.getHeadRelationBS() == (KB.EXISTSbs) || query.getHeadRelationBS() == (KB.EXISTSINVbs)) {
			return (long) headCardinalities.get(query.getHead()[0]);
		} else {
			return (long) headCardinalities.get(query.getHeadRelationBS());
		}	
	}
	
	@Override
	public String getDescription() {
		return "Also mine existential rules";
	}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return candidate.isClosed(false);
	}

	@Override
	/**
	 * Transforms atoms with an open variable to exists statements
	 */
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output){
		super.getClosingAtoms(rule, minSupportThreshold, output);
		IntList openVariables = rule.getOpenVariables();
		List<int[]> candidate = new ArrayList<>();
		if (openVariables.size() > 0) {
			for (int[] triple : rule.getTriplesCopy()) {
				for (int openVariable : openVariables) {
					if (triple[0] == openVariable && openVariables.contains(triple[2])) {
						return;
					}
					if (triple[0] == openVariable) {
						triple = Rule.triple(triple[1], KB.EXISTSbs, triple[2]);
						break;
					}
					if (triple[2] == openVariable) {
						triple = Rule.triple(triple[1], KB.EXISTSINVbs, triple[0]);
						break;
					}
				}
				candidate.add(triple);
			}
			int[] head = candidate.get(0);
			long cardinality = -1;
			if (KB.numVariables(head) == 2) {
				cardinality = kb.countDistinctPairs(head[0], head[2], candidate);
			} else if (KB.numVariables(head) == 1) {
				cardinality = kb.countDistinct(KB.isVariable(head[0]) ? head[0] : head[2], candidate);
			}
			Rule nRule = new Rule(candidate.get(0), candidate.subList(1, candidate.size()), cardinality);
			nRule.setHeadCoverage((double)cardinality / (double)getHeadCardinality(nRule));
			nRule.setSupportRatio((double)cardinality / (double)this.kb.size());
			nRule.addParent(rule);
			output.add(nRule);
		}
	}
	
}
