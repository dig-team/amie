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
	double sumOfInvRanks;
	double sumOfHeadInvRanks;
	double sumOfTailInvRanks;
	int headRanks;
	int tailRanks;

	public EvaluationResult() {
		this.headMetrics = new HashMap<>();
		this.tailMetrics = new HashMap<>();
		this.bothMetrics = new HashMap<>();
		this.sumOfInvRanks = this.sumOfHeadInvRanks = this.sumOfTailInvRanks = 0.0;
		this.headRanks = this.tailRanks = 0;
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


	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		headMetrics.entrySet().stream().forEach(e -> str.append(e.getKey() + " (Head): " + formatMetrics(e.getValue()) + "\n"));
		tailMetrics.entrySet().stream().forEach(e -> str.append(e.getKey() + " (Tail): " + formatMetrics(e.getValue()) + "\n"));
		bothMetrics.entrySet().stream().forEach(e -> str.append(e.getKey() + " (Both): " + formatMetrics(e.getValue()) + "\n"));
		return str.toString();
	}

	private String formatMetrics(Map<LinkPredictionMetric, Double> metricsMap) {
		StringBuilder str = new StringBuilder();
		metricsMap.entrySet().stream().forEach(e -> str.append(e.getKey() + "=" + e.getValue() + "; "));
		return str.toString();
	}

	public void computeMRR(EvaluationFocus focus, String relation) {
		switch (focus){
			case Head:
				this.putResult(focus, relation, LinkPredictionMetric.MRR, this.sumOfHeadInvRanks / this.headRanks);
				break;
			case Tail:
				this.putResult(focus, relation, LinkPredictionMetric.MRR, this.sumOfTailInvRanks / this.tailRanks);
				break;
			case Both:
				this.putResult(focus, relation, LinkPredictionMetric.MRR, this.sumOfInvRanks / (this.headRanks + this.tailRanks));
				break;
		}

	}

	public void computeCount(EvaluationFocus focus, String relation) {
		switch(focus) {
			case Head:
				this.putResult(focus, relation, LinkPredictionMetric.Count, this.headRanks);
				break;
			case Tail:
				this.putResult(focus, relation, LinkPredictionMetric.Count, this.tailRanks);
				break;
			case Both:
				this.putResult(focus, relation, LinkPredictionMetric.Count, this.tailRanks + this.headRanks);
				break;
		}

	}


}
