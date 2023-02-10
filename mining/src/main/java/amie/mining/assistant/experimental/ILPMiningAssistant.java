package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

import amie.mining.assistant.MiningOperator;

public class ILPMiningAssistant extends DefaultMiningAssistant {

	public ILPMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	@Override
	public String getDescription() {
       	return "Mining assistant that defines support "
       			+ "by counting support on both head variables " 
       			+ "and using explicit counter-examples.";
	}
	
	/**
	 * Negate the string form of a predicate.
	 */
	private String negateRelation(String target) {
		String negatedTarget = null;

		if (target.startsWith("<")) {
			if (target.startsWith("<neg_")) {
				negatedTarget = target.replace("<neg_", "<");
			} else {
				negatedTarget = target.replace("<", "<neg_");			
			}
		} else {
			if (target.startsWith("neg_")) {
				negatedTarget = target.replace("neg_", "");
			} else {
				negatedTarget = "neg_" + target;
			}
		}
		
		return negatedTarget;
	}
	
	/**
	 * It computes the PCA confidence of the given rule based on the evidence in database.
	 * The value is both returned and set to the rule
	 * @param rule
	 * @return
	 */
	@Override
	public double computePCAConfidence(Rule rule) {
		if (rule.isEmpty()) {
			return rule.getPcaConfidence();
		}
		
		List<int[]> antecedent = new ArrayList<int[]>();
		antecedent.addAll(rule.getTriples().subList(1, rule.getTriples().size()));
		int[] succedent = rule.getTriples().get(0);
		double pcaDenominator = 0.0;
		int[] existentialTriple = succedent.clone();
		int noOfHeadVars = KB.numVariables(succedent);
		
		String target = KB.unmap(succedent[1]);
		String negatedTarget = this.negateRelation(target);
		existentialTriple[1] = KB.map(negatedTarget);
		
		if (!antecedent.isEmpty()) {
			antecedent.add(existentialTriple);
			try{
				if (noOfHeadVars == 1) {
					pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
				} else {
					pcaDenominator = (double) this.kb.countDistinctPairs(succedent[0], succedent[2], antecedent);					
				}
				rule.setPcaBodySize(pcaDenominator + (long)rule.getSupport());
			}catch(UnsupportedOperationException e){
				
			}
		}
		
		return rule.getPcaConfidence();
		
	}
	
	protected List<Rule> filterRuleWithNegatedHeadInBody(List<Rule> input) {
		// Check if the rule contains an atom with the negated version of the head
		// predicate
		List<Rule> tmpOutput = new ArrayList<>();
		
		for (Rule r : input) {
			int head[] = r.getHead();
			// But only for functional relations
			if (this.kb.isFunctional(head[1]) && this.kb.functionality(head[1]) >= 0.9) {
				boolean addRule = true;
				String headRelation = kb.unmap(head[1]);
				for (int[] atom : r.getBody()) {
					String relation = kb.unmap(atom[1]);
					String negatedRelation = this.negateRelation(relation);
					if (negatedRelation.equals(headRelation)) {
						addRule = false;
						break;
					}
				}
				
				if (addRule)
					tmpOutput.add(r);
			}

		}
		
		return tmpOutput;
	}
		
	
	@Override
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {		
		List<Rule> tmpOutput = new ArrayList<>();
		super.getDanglingAtoms(query, minCardinality, tmpOutput);
		List<Rule> tmpOutput2 = this.filterRuleWithNegatedHeadInBody(tmpOutput);
		output.addAll(tmpOutput2);
	}

}
