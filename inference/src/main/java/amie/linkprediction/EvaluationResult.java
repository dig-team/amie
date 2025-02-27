package amie.linkprediction;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

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
	double sumOfFilteredInvRanks;
	double sumOfFilteredHeadInvRanks;
	double sumOfFilteredTailInvRanks;
	int headRanks;
	int tailRanks;
	int headHitsAt1;
	int headHitsAt3;
	int headHitsAt5;
	int headHitsAt10;
	int tailHitsAt1;
	int tailHitsAt3;
	int tailHitsAt5;
	int tailHitsAt10;
	int filteredHeadHitsAt1;
	int filteredHeadHitsAt3;
	int filteredHeadHitsAt5;
	int filteredHeadHitsAt10;
	int filteredTailHitsAt1;
	int filteredTailHitsAt3;
	int filteredTailHitsAt5;
	int filteredTailHitsAt10;

	public EvaluationResult() {
		this.headMetrics = new HashMap<>();
		this.tailMetrics = new HashMap<>();
		this.bothMetrics = new HashMap<>();
		this.sumOfInvRanks = this.sumOfHeadInvRanks = this.sumOfTailInvRanks = 0.0;
		this.sumOfFilteredInvRanks = this.sumOfFilteredHeadInvRanks = this.sumOfFilteredTailInvRanks = 0.0;
		this.headRanks = this.tailRanks = 0;
		this.headHitsAt1 = this.headHitsAt3 = this.headHitsAt5 = this.headHitsAt10 = 0;
		this.tailHitsAt1 = this.tailHitsAt3 = this.tailHitsAt5 = this.tailHitsAt10 = 0;
		this.filteredHeadHitsAt1 = this.filteredHeadHitsAt3 = this.filteredHeadHitsAt5 = this.filteredHeadHitsAt10 = 0;
		this.filteredTailHitsAt1 = this.filteredTailHitsAt3 = this.filteredTailHitsAt5 = this.filteredTailHitsAt10 = 0;

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
				this.putResult(focus, relation, LinkPredictionMetric.FilteredMRR,
							this.sumOfFilteredHeadInvRanks / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.MRR,
							this.sumOfHeadInvRanks / this.headRanks);
				break;
			case Tail:
				this.putResult(focus, relation, LinkPredictionMetric.FilteredMRR,
							this.sumOfFilteredTailInvRanks / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.MRR,
							this.sumOfTailInvRanks / this.tailRanks);
				break;
			case Both:
				this.putResult(focus, relation, LinkPredictionMetric.FilteredMRR,
							this.sumOfFilteredInvRanks / (this.headRanks + this.tailRanks));
				this.putResult(focus, relation, LinkPredictionMetric.MRR,
							this.sumOfInvRanks / (this.headRanks + this.tailRanks));
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

	public void computeHits(EvaluationFocus focus, String relation) {
		switch (focus){
			case Head:
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt1,
						(double)this.headHitsAt1 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt1,
						(double)this.filteredHeadHitsAt1 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt3,
						(double)this.headHitsAt3 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt3,
						(double)this.filteredHeadHitsAt3 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt5,
						(double)this.headHitsAt5 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt5,
						(double)this.filteredHeadHitsAt5 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt10,
						(double)this.headHitsAt10 / this.headRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt10,
						(double)this.filteredHeadHitsAt10 / this.headRanks);
				break;
			case Tail:
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt1,
						(double)this.tailHitsAt1 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt1,
						(double)this.filteredTailHitsAt1 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt3,
						(double)this.tailHitsAt3 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt3,
						(double)this.filteredTailHitsAt3 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt5,
						(double)this.tailHitsAt5 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt5,
						(double)this.filteredTailHitsAt5 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt10,
						(double)this.tailHitsAt10 / this.tailRanks);
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt10,
						(double)this.filteredTailHitsAt10 / this.tailRanks);
				break;
			case Both:
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt1,
						(double)(this.tailHitsAt1 + this.headHitsAt1) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt1,
						(double)(this.filteredTailHitsAt1 + this.filteredHeadHitsAt1) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt3,
						(double)(this.tailHitsAt3 + this.headHitsAt3) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt3,
						(double)(this.filteredTailHitsAt3 + this.filteredHeadHitsAt3) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt5,
						(double)(this.tailHitsAt5 + this.headHitsAt5) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt5,
						(double)(this.filteredTailHitsAt5 + this.filteredHeadHitsAt5) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.HitsAt10,
						(double)(this.tailHitsAt10 + this.headHitsAt10) / (this.tailRanks + this.headRanks));
				this.putResult(focus, relation, LinkPredictionMetric.FilteredHitsAt10,
						(double)(this.filteredTailHitsAt10 + this.filteredHeadHitsAt10) / (this.tailRanks + this.headRanks));
				break;
		}
	}

	public static EvaluationResult merge(Collection<EvaluationResult> partialResults) {
		EvaluationResult mergedResult = new EvaluationResult();
		for (EvaluationResult result : partialResults) {
			mergedResult.headMetrics.putAll(result.headMetrics);
			mergedResult.tailMetrics.putAll(result.tailMetrics);
			mergedResult.bothMetrics.putAll(result.bothMetrics);
			mergedResult.sumOfHeadInvRanks += result.sumOfHeadInvRanks;
			mergedResult.sumOfTailInvRanks += result.sumOfTailInvRanks;
			mergedResult.sumOfInvRanks += result.sumOfInvRanks;
			mergedResult.headRanks += result.headRanks;
			mergedResult.tailRanks += result.tailRanks;
			mergedResult.sumOfFilteredHeadInvRanks += result.sumOfFilteredHeadInvRanks;
			mergedResult.sumOfFilteredTailInvRanks += result.sumOfFilteredTailInvRanks;
			mergedResult.sumOfFilteredInvRanks += result.sumOfFilteredInvRanks;
			mergedResult.headHitsAt1 += result.headHitsAt1;
			mergedResult.headHitsAt3 += result.headHitsAt3;
			mergedResult.headHitsAt5 += result.headHitsAt5;
			mergedResult.headHitsAt10 += result.headHitsAt10;
			mergedResult.tailHitsAt1 += result.tailHitsAt1;
			mergedResult.tailHitsAt3 += result.tailHitsAt3;
			mergedResult.tailHitsAt5 += result.tailHitsAt5;
			mergedResult.tailHitsAt10 += result.tailHitsAt10;
			mergedResult.filteredHeadHitsAt1 += result.filteredHeadHitsAt1;
			mergedResult.filteredHeadHitsAt3 += result.filteredHeadHitsAt3;
			mergedResult.filteredHeadHitsAt5 += result.filteredHeadHitsAt5;
			mergedResult.filteredHeadHitsAt10 += result.filteredHeadHitsAt10;
			mergedResult.filteredTailHitsAt1 += result.filteredTailHitsAt1;
			mergedResult.filteredTailHitsAt3 += result.filteredTailHitsAt3;
			mergedResult.filteredTailHitsAt5 += result.filteredTailHitsAt5;
			mergedResult.filteredTailHitsAt10 += result.filteredTailHitsAt10;
		}
		mergedResult.computeCount(EvaluationFocus.Head, "All");
		mergedResult.computeCount(EvaluationFocus.Tail, "All");
		mergedResult.computeCount(EvaluationFocus.Both, "All");
		mergedResult.computeMRR(EvaluationFocus.Head, "All");
		mergedResult.computeMRR(EvaluationFocus.Tail, "All");
		mergedResult.computeMRR(EvaluationFocus.Both, "All");
		mergedResult.computeHits(EvaluationFocus.Head, "All");
		mergedResult.computeHits(EvaluationFocus.Tail, "All");
		mergedResult.computeHits(EvaluationFocus.Both, "All");

		return mergedResult;
	}
}
