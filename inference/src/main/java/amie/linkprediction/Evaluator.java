/**
 * A class to 
 */
package amie.linkprediction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import amie.data.Dataset;
import amie.rules.Rule;
import amie.rules.AMIEParser;

public class Evaluator {
	private Dataset dataset;
	private Collection<Rule> rules;

	public static Evaluator getEvaluator(String dataFolder, String rulesFile) throws IOException {
		Dataset d = new Dataset(dataFolder);
		List<Rule> rules = AMIEParser.parseRules(new File(rulesFile), d.training);
		return new Evaluator(d, rules);
	}

	protected Evaluator(Dataset d, Collection<Rule> rules) {
		this.dataset = d;
		this.rules = rules;
	}

	public EvaluationResult evaluate() {
		// Get all predicates
		EvaluationResult resultMetrics = new EvaluationResult();
		for (int rel : this.dataset.testing.keySet()) {
			List<int[]> batch = this.dataset.testing.get(rel);
			evaluateBatch(batch, resultMetrics);
		}

		computeGlobalEvaluationMetrics(resultMetrics);

		return resultMetrics;
	}

	private void computeGlobalEvaluationMetrics(EvaluationResult resultMetrics) {
		throw new UnsupportedOperationException("Unimplemented method 'computeGlobalEvaluationMetrics'");
	}

	private void evaluateBatch(List<int[]> batch, EvaluationResult resultMetrics) {
		List<Ranking> rankingsHead = new ArrayList<>();
		List<Ranking> rankingsTail = new ArrayList<>();
		for (int[] triple : batch) {
			rankingsHead.add(new Ranking(new Query(this.dataset.training, -1, triple[1], triple[2])));
			rankingsTail.add(new Ranking(new Query(this.dataset.training, triple[0], triple[1], -1)));
		}
		getHeadQueryCandidatesStream(batch.get(0)[1]).forEach(e -> this.updateRankings(e, rankingsHead));
		getTailQueryCandidatesStream(batch.get(0)[1]).forEach(e -> this.updateRankings(e, rankingsTail));

		computeBatchEvaluationMetrics(rankingsHead, rankingsTail, resultMetrics);
	}

	private void computeBatchEvaluationMetrics(List<Ranking> rankingsHead, List<Ranking> rankingsTail,
			EvaluationResult resultMetrics) {
		throw new UnsupportedOperationException("Unimplemented method 'computeEvaluationMetrics'");
	}

	private void updateRankings(int entity, List<Ranking> rankings) {
		for (Ranking r : rankings) {
			Query q = r.getQuery();
		}
	}

	private Stream<Integer> getHeadQueryCandidatesStream(int relation) {
		// TODO: Look at the rules and decide which entity ranges to query
		return null;
	}

	private Stream<Integer> getTailQueryCandidatesStream(int relation) {
		// TODO: Look at the rules and decide which entity ranges to query
		return null;
	}
}
