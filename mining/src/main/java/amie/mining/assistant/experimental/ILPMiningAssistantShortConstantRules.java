package amie.mining.assistant.experimental;

import java.util.Collection;

import amie.data.KB;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;

public class ILPMiningAssistantShortConstantRules extends ILPMiningAssistant {

	public ILPMiningAssistantShortConstantRules(KB dataSource) {
		super(dataSource);
	}
	
	@MiningOperator(name="instantiated", dependency="dangling")
	public void getInstantiatedAtoms(Rule rule, double minSupportThreshold, 
			Collection<Rule> danglingEdges, Collection<Rule> output) {
		if (rule.getRealLength() < 2) {
			super.getInstantiatedAtoms(rule, minSupportThreshold, danglingEdges, output);
		}
	}

}
