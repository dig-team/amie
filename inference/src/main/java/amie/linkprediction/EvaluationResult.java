package amie.linkprediction;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for the information resulting from evaluating a list of rules
 * for link prediction on a data set.
 */
public class EvaluationResult {
	public Map<String, Map<LinkPredictionMetric, Double>> headMetrics;
	public Map<String, Map<LinkPredictionMetric, Double>> tailMetrics;
	public Map<String, Map<LinkPredictionMetric, Double>> bothMetrics;

	public EvaluationResult() {
		this.headMetrics = new HashMap<>();
		this.tailMetrics = new HashMap<>();
		this.bothMetrics = new HashMap<>();
	}

	public void putResult(EvaluationFocus focus, String scope, LinkPredictionMetric metric, double value) {
		switch (focus) {
			case Head:
				putResult(headMetrics, scope, metric, value);
				break;
			case Both:
				putResult(bothMetrics, scope, metric, value);
				break;
			case Tail:
				putResult(tailMetrics, scope, metric, value);
				break;
		}
	}

	private void putResult(Map<String, Map<LinkPredictionMetric, Double>> metricsMap,
			String scope, LinkPredictionMetric metric, double value) {
		if (metricsMap.containsKey(scope)) {
			Map<LinkPredictionMetric, Double> metricSubMap = metricsMap.get(scope);
			metricSubMap.put(metric, value);
		} else {
			Map<LinkPredictionMetric, Double> metricSubMap = new HashMap<>();
			metricSubMap.put(metric, value);
			metricsMap.put(scope, metricSubMap);
		}

	}
}
