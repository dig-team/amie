/**
 * A class to evaluate the link prediction capabilities of a set of rules
 */
package amie.linkprediction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import amie.data.Dataset;
import amie.data.javatools.datatypes.Pair;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import amie.rules.AMIEParser;

public class Evaluator {
	private Dataset dataset;
	private Map<Integer, Collection<Rule>> rules;

	public static Evaluator getEvaluator(String dataFolder, String rulesFile) throws IOException {
		Dataset d = new Dataset(dataFolder);
		List<Rule> rules = AMIEParser.parseRules(new File(rulesFile), d.training);
		return new Evaluator(d, rules);
	}

	protected Evaluator(Dataset d, Collection<Rule> inputRules) {
		this.dataset = d;
		this.rules = new HashMap<>();
		this.indexRules(inputRules);
	}

	/**
	 * It builds an index keyed by the rules' head relation.
	 * The rules that predict that relation are sorted in descending order
	 * of PCA confidence -- support is used as a second criterion to break ties
	 * 
	 * @param inputRules
	 */
	private void indexRules(Collection<Rule> inputRules) {
		for (Rule r : inputRules) {
			int headR = r.getHeadRelationBS();
			if (!this.rules.containsKey(headR)) {
				this.rules.put(headR, new TreeSet<>(new Comparator<Rule>() {
					@Override
					public int compare(Rule arg0, Rule arg1) {
						double conf0 = arg0.getPcaConfidence();
						double conf1 = arg1.getPcaConfidence();
						if (conf0 == conf1) {
							return Double.compare(arg0.getSupport(), arg1.getSupport());
						} else {
							return Double.compare(conf0, conf1);
						}
					}

				}));
			}
		}
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
		getQueryCandidatesStream(batch.get(0)[1], 0).forEach(e -> this.updateRankings(e, rankingsHead));
		getQueryCandidatesStream(batch.get(0)[1], 2).forEach(e -> this.updateRankings(e, rankingsTail));

		computeBatchEvaluationMetrics(rankingsHead, rankingsTail, resultMetrics);
	}

	private void computeBatchEvaluationMetrics(List<Ranking> rankingsHead, List<Ranking> rankingsTail,
			EvaluationResult resultMetrics) {
		throw new UnsupportedOperationException("Unimplemented method 'computeEvaluationMetrics'");
	}

	private void updateRankings(int entity, List<Ranking> rankings) {
		for (Ranking r : rankings) {
			Query q = r.getQuery();
			Rank entityRank = getEntityScoresForQuery(entity, q);
			r.addSolution(entityRank);
		}
	}

	/**
	 * Compute the solution score of an entity for queries of the type
	 * r(?s, o) or r(s, ?o)
	 * 
	 * @param entity The entity whose scores we want to know
	 * @param q      The query
	 * @return A rank object with the scores provided for the query and
	 *         the entity according to the input rules
	 */
	private Rank getEntityScoresForQuery(int entity, Query q) {
		double confidence = 0.0;
		double support = 0.0;
		int headR = q.triple[1];
		if (this.rules.containsKey(headR)) {
			for (Rule candidate : this.rules.get(headR)) {
				if (this.ruleSupportsEntityForQuery(candidate, entity, q)) {
					// We are done
					confidence = candidate.getPcaConfidence();
					support = candidate.getSupport();
				}
			}
		}

		return new Rank(entity, confidence, support);
	}

	/**
	 * It determines whether a rule provides evidence for entity as a solution
	 * for the query
	 * 
	 * @param rules
	 * @param entity
	 * @param query
	 * @return
	 */
	public boolean ruleSupportsEntityForQuery(Rule rule, int entity, Query query) {
		int[] headAtom = rule.getHead();
		if (Rule.isUnifiable(headAtom, query.triple)) {
			List<int[]> triples = rule.getTriplesCopy();
			int[] newTriple = query.triple.clone();
			int varPos = query.variablePosition();
			int otherPos = query.constantPosition();
			// TODO: The instantiation must be done in the entire query
			// We instantiate the variable
			newTriple[varPos] = entity;
			// And we make sure we use the same variable name for the other side if needed
			newTriple[otherPos] = headAtom[otherPos];
			triples.set(0, newTriple);
			triples.stream().forEach(e -> System.out.println(e[0] + " " + e[1] + " " + e[2]));
			return this.dataset.training.existsBS1(triples);
		} else {
			return false;
		}
	}

	private <T> Stream<T> iteratorToStream(Iterator<T> it) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED),
				false);
	}

	private Stream<Integer> getQueryCandidatesStream(int relation, int varPos) {
		Iterator<Integer> returnIterator = null;
		if (this.rules.containsKey(relation)) {
			int variable = varPos == 0 ? -1 : -2;
			returnIterator = this.dataset.training.selectDistinctIterator(
					new IntLinkedOpenHashSet(), variable,
					Collections.singletonList(new int[] { -1, relation, -2 }));
			Stream<Integer> returnStream = this.iteratorToStream(returnIterator);

			for (int[] triplePattern : getAllCommonDomainTriplePatterns(this.rules.get(relation), varPos)) {
				if (triplePattern[1] != relation) {
					Stream<Integer> currentStream = iteratorToStream(this.dataset.training.selectDistinctIterator(
							new IntLinkedOpenHashSet(), variable,
							Collections.singletonList(triplePattern)));
					returnStream = Stream.concat(returnStream, currentStream);
				}
			}
			return returnStream;
		} else {
			// Return all subjects
			returnIterator = this.dataset.training.getSubjects().iterator();
			return StreamSupport.stream(
					Spliterators.spliteratorUnknownSize(returnIterator, Spliterator.ORDERED),
					false);
		}

	}

	private Collection<int[]> getAllCommonDomainTriplePatterns(Collection<Rule> rules, int headAtomVarPos) {
		ArrayList<int[]> triplePatterns = new ArrayList<>();
		LinkedHashSet<Pair<Integer, Integer>> seenPredicates = new LinkedHashSet<>();
		for (Rule rule : rules) {
			int[] headAtom = rule.getHead();
			int variableOfInterest = headAtom[headAtomVarPos];
			for (int[] atom : rule.getBody()) {
				int varPos = -1;
				if (atom[0] == variableOfInterest)
					varPos = 0;
				if (atom[2] == variableOfInterest)
					varPos = 2;
				if (varPos == -1) {
					continue;
				} else {
					if (seenPredicates.contains(new Pair<>(atom[1], varPos))) {
						continue;
					} else {
						triplePatterns.add(atom.clone());
						seenPredicates.add(new Pair<>(atom[1], varPos));
					}
				}
			}
		}
		return triplePatterns;
	}
}
