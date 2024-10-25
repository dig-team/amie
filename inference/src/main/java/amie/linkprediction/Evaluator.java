/**
 * A class to evaluate the link prediction capabilities of a set of rules
 */
package amie.linkprediction;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import amie.data.AbstractKB;
import amie.data.Dataset;
import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import amie.rules.AMIEParser;
import org.apache.commons.collections4.ListUtils;

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
						int confCmp = Double.compare(conf1, conf0);
						if (confCmp == 0) {
							double support0 = arg0.getSupport();
							double support1 = arg1.getSupport();
							int suppCmp = Double.compare(support1, support0);
							if (suppCmp == 0) {
								// Put shorter -- more general -- rules before
								return Integer.compare(arg0.getLength(), arg1.getLength());
							} else {
								return suppCmp;
							}
						} else {
							return confCmp;
						}
					}

				}));
			}
			this.rules.get(headR).add(r);
		}
	}

	/**
	 * Returns all the link prediction evaluation metrics, per predicate and for the entire
	 * test set. The evaluation corresponds to a classical transductive link prediction task:
	 * For each triple in the test set, we mask its subject and object and use the rules to rank
	 * the masked value. Those ranks allow for computing aggregated values for Hits@k (1, 3, 10) and
	 * MRR (Mean reciprocal rank) for the rules on the test set.
	 * @return
	 */
	public EvaluationResult evaluate() {
		// Get all predicates
		EvaluationResult resultMetrics = new EvaluationResult();
		for (int rel : this.dataset.testing.keySet()) {
			List<int[]> relationBatch = this.dataset.testing.get(rel);
			System.err.println("Evaluating " + relationBatch.size() + " triples in relation " + this.dataset.training.unmap(rel));
			evaluateRelation(relationBatch, resultMetrics);
		}

		computeGlobalEvaluationMetrics(resultMetrics);

		return resultMetrics;
	}

	private void computeGlobalEvaluationMetrics(EvaluationResult resultMetrics) {
	}

	private void evaluateRelation(List<int[]> batch, EvaluationResult resultMetrics) {
		evaluateRelationOnOneFocus(batch, EvaluationFocus.Head, resultMetrics);
		evaluateRelationOnOneFocus(batch, EvaluationFocus.Tail, resultMetrics);
		// Get the MRR for both metrics
		resultMetrics.putResult(EvaluationFocus.Both, this.dataset.training.unmap(batch.get(0)[1]),
				LinkPredictionMetric.MRR, resultMetrics.sumOfInvRanks / (2 * batch.size())); // both rankings should have the same size
	}

	private void evaluateRelationOnOneFocus(List<int[]> batch, EvaluationFocus focus, EvaluationResult resultMetrics) {
		IntLinkedOpenHashSet candidatesSet = null;
		// We have to carry out the evaluation per sub-batches, otherwise it consumes too much memory
		for (List<int[]> subBatch : ListUtils.partition(batch, 100)) {
			List<Pair<Integer, Ranking>> rankings = new ArrayList<>();
			for (int[] triple : subBatch) {
				rankings.add(new Pair<>(triple[0], new Ranking(new Query(this.dataset.training, -1, triple[1], triple[2]))));
			}
			if (candidatesSet == null) {
				candidatesSet = new IntLinkedOpenHashSet();
				IntLinkedOpenHashSet finalCandidatesSet = candidatesSet;
				getQueryCandidatesStream(batch.get(0)[1], focus == EvaluationFocus.Head ? 0 : 2).forEach(
						e -> {
							this.updateRankings(e, rankings);
							finalCandidatesSet.add(e);
						});
			} else {
				candidatesSet.stream().forEach(e -> this.updateRankings(e, rankings));
			}
			updateBatchEvaluationMetrics(rankings, focus, resultMetrics);
		}

		String relation = this.dataset.training.unmap(batch.get(0)[1]);
		resultMetrics.computeMRR(focus, relation);
		resultMetrics.computeCount(focus, relation);
	}

	private void updateBatchEvaluationMetrics(List<Pair<Integer, Ranking>> rankings, EvaluationFocus focus,
											  EvaluationResult resultMetrics) {
		for (Pair<Integer, Ranking> solutionAndRanking: rankings) {
			Ranking ranking = solutionAndRanking.second;
			ranking.build();
			int solution = solutionAndRanking.first;
			int rank = ranking.rank(solution);
			double invRank = 1.0 / rank;
			if (focus == EvaluationFocus.Head) {
				resultMetrics.sumOfHeadInvRanks += invRank;
				resultMetrics.headRanks++;
			} else if (focus == EvaluationFocus.Tail) {
				resultMetrics.sumOfTailInvRanks += invRank;
				resultMetrics.tailRanks++;
			}
			resultMetrics.sumOfInvRanks += invRank;
		}
	}

	private void updateRankings(int entity, List<Pair<Integer, Ranking>> rankings) {
		for (Pair<Integer, Ranking> entityAndRanking : rankings) {
			Ranking r = entityAndRanking.second;
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
	public Rank getEntityScoresForQuery(int entity, Query q) {
		double confidence = 0.0;
		double support = 0.0;
		int headR = q.triple[1];
		if (this.rules.containsKey(headR)) {
			for (Rule candidate : this.rules.get(headR)) {
				if (this.ruleSupportsEntityForQuery(candidate, entity, q)) {
					// We are done
					confidence = candidate.getPcaConfidence();
					support = candidate.getSupport();
					break;
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
		List<int[]> allTriples = rule.getTriplesCopy();
		List<int[]> bodyTriples = allTriples.subList(1, allTriples.size());
		int varPos = query.variablePosition();
		int otherPos = query.constantPosition();
		KB.Instantiator solutionInst = null;
		if (AbstractKB.isVariable(headAtom[varPos])) {
			solutionInst = new KB.Instantiator(bodyTriples, headAtom[varPos]);
			solutionInst.instantiate(entity);
		} else {
			// The head atom has a constant argument
			// The query and the rule are not unifiable
			if (headAtom[varPos] != entity) {
				return false;
			}
		}
		// At this stage we know that the query variable is unifiable to the rule
		// Let us see the other side
		KB.Instantiator queryInst = null;
		if (AbstractKB.isVariable(headAtom[otherPos])){
			// And we make sure we use the same variable name for the other side if needed
			queryInst = new KB.Instantiator(bodyTriples, headAtom[otherPos]);
			queryInst.instantiate(query.triple[otherPos]);
		} else {
			if (headAtom[otherPos] != query.triple[otherPos])
				return false;
		}
		// Now let us check if this body instantiation exists
		//bodyTriples.stream().forEach(e -> System.out.println(e[0] + " " + e[1] + " " + e[2]));
		boolean ruleSupportsSolution = this.dataset.training.existsBS1(bodyTriples);

		if (solutionInst != null)
			solutionInst.close();

		if (queryInst != null)
			queryInst.close();

		return ruleSupportsSolution;
	}

	private <T> Stream<T> iteratorToStream(Iterator<T> it) {
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED),
				false);
	}

	public Stream<Integer> getQueryCandidatesStream(int relation, int varPos) {
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
				if (varPos != -1) {
					if (!seenPredicates.contains(new Pair<>(atom[1], varPos))) {
						triplePatterns.add(atom.clone());
						seenPredicates.add(new Pair<>(atom[1], varPos));
					}
				}
			}
		}
		return triplePatterns;
	}
}
