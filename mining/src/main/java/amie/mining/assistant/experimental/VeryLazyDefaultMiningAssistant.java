package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

/**
 * Version of the default mining assistant that does not calculate confidence scores.
 * Useful when we only care about the rules and not their precision.
 * 
 * @author lgalarra
 *
 */
public class VeryLazyDefaultMiningAssistant extends DefaultMiningAssistant {

	public VeryLazyDefaultMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	@Override
	public String getDescription() {
       	return "Default mining assistant that defines support "
       			+ "by counting support on both head variables "
       			+ "(very lazy version that does not compute confidence scores)";
	}
	
	@Override
	public double computePCAConfidence(Rule rule) {
		rule.setPcaBodySize((long)rule.getSupport());
		return rule.getPcaConfidence();
	}
	
	@Override
	public double computeStandardConfidence(Rule candidate) {
		candidate.setBodySize((long)candidate.getSupport());
		return candidate.getStdConfidence();
	}

}
