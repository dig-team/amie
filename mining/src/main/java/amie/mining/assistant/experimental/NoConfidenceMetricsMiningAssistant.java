package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

public class NoConfidenceMetricsMiningAssistant extends DefaultMiningAssistant {

	public NoConfidenceMetricsMiningAssistant(KB dataSource) {
		super(dataSource);
	}

	/**
	 * It computes the standard and the PCA confidence of a given rule. It
	 * assumes the rule's cardinality (absolute support) is known.
	 *
	 * @param candidate
	 */
	public void calculateConfidenceMetrics(Rule candidate) {
		// Do not compute any metrics
	}

	public boolean testConfidenceThresholds(Rule candidate) {
		return true;
	}
}
