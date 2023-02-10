package amie.mining.assistant.experimental;

import java.util.List;

import amie.data.KB;
import amie.rules.Rule;

public class VeryLazyILPMiningAssistant extends ILPMiningAssistant{

	public VeryLazyILPMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	@Override
	public String getDescription() {
       	return "Mining assistant that defines support "
       			+ "by counting support on both head variables " 
       			+ "and using explicit counter-examples (it does not calculate precision scores).";
	}
	
	@Override
	public double computeStandardConfidence(Rule candidate) {
		candidate.setBodySize((long)candidate.getSupport());
		return candidate.getStdConfidence();
	}
	
	@Override
	protected List<Rule> filterRuleWithNegatedHeadInBody(List<Rule> input) {
		return input;
	}

}
