/**
 * A class to evaluate the link prediction capabilities of a set of rules
 */
package amie.linkprediction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import amie.data.AbstractKB;
import amie.data.Dataset;
import amie.data.KB;
import amie.data.javatools.datatypes.Pair;
import amie.data.javatools.datatypes.Triple;
import amie.rules.Rule;
import com.google.gson.Gson;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import amie.rules.AMIEParser;
import org.apache.commons.collections4.ListUtils;

public class Evaluator {
	private Dataset dataset;
	private Map<Integer, Collection<Rule>> rules;
	private int nCores;
	private int batchSize;

	public static int DEFAULT_BATCH_SIZE = 100;

	public static int MAX_RANKING_SIZE = 1000;

	public static Evaluator getEvaluator(String dataFolder, String rulesFile) throws IOException {
		Dataset d = new Dataset(dataFolder);
		List<Rule> rules = AMIEParser.parseRules(new File(rulesFile), d.training);
		return new Evaluator(d, rules, 1);
	}

	public static Evaluator getEvaluator(String dataFolder, String rulesFile, int nCores, boolean amieRules) throws IOException {
		Dataset d = new Dataset(dataFolder);
		List<Rule> rules = amieRules? AMIEParser.parseRules(new File(rulesFile), d.training) : AMIEParser.parseAnyBurlFormattedRules(new File(rulesFile), d.training);
		return new Evaluator(d, rules, nCores);
	}

	protected Evaluator(Dataset d, Collection<Rule> inputRules, int nCores) {
		this.dataset = d;
		this.rules = new HashMap<>();
		this.nCores = nCores;
		this.batchSize = DEFAULT_BATCH_SIZE;
		this.indexRules(inputRules);
	}

	private void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
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

	protected static class PredicateEvaluator implements Callable<EvaluationResult> {
		List<int[]> triplesInBatch;
		int rel;
		Evaluator ev;
		public PredicateEvaluator(int rel, List<int[]> triplesInBatch, Evaluator e) {
			this.rel = rel;
			this.triplesInBatch = triplesInBatch;
			this.ev = e;
		}
		@Override
		public EvaluationResult call() {
			EvaluationResult relationResultMetrics = new EvaluationResult();
			List<int[]> relationBatch = this.ev.dataset.testing.get(rel);
			System.err.println("Evaluating " + relationBatch.size() + " triples in relation " + this.ev.dataset.training.unmap(rel));
			this.ev.evaluateRelation(relationBatch, relationResultMetrics);
			return relationResultMetrics;
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
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.nCores);
		List<Callable<EvaluationResult>> tasks = new ArrayList<Callable<EvaluationResult>>();
		for (final int rel : this.dataset.testing.keySet()) {
			List<int[]> relationBatch = this.dataset.testing.get(rel);
			tasks.add(new PredicateEvaluator(rel, relationBatch, this));
		}
		List<Future<EvaluationResult>> results = null;
        try {
            results = executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

		return EvaluationResult.merge(results.stream().map(x -> {
			try {
				return x.get();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList()));

	}

	private void evaluateRelation(List<int[]> batch, EvaluationResult resultMetrics) {
		// To limit memory consumption we have to focus on one type of evaluation
		evaluateRelationOnOneFocus(batch, EvaluationFocus.Head, resultMetrics);
		evaluateRelationOnOneFocus(batch, EvaluationFocus.Tail, resultMetrics);
		if (!batch.isEmpty()) {
			resultMetrics.computeCount(EvaluationFocus.Both, this.dataset.training.unmap(batch.get(0)[1]));
			resultMetrics.computeMRR(EvaluationFocus.Both, this.dataset.training.unmap(batch.get(0)[1]));
			resultMetrics.computeHits(EvaluationFocus.Both, this.dataset.training.unmap(batch.get(0)[1]));
		}
	}

	private void evaluateRelationOnOneFocus(List<int[]> batch, EvaluationFocus focus, EvaluationResult resultMetrics) {
		IntLinkedOpenHashSet candidatesSet = null;
		// We have to carry out the evaluation per sub-batches, otherwise it consumes too much memory
		for (List<int[]> subBatch : ListUtils.partition(batch, this.batchSize)) {
			List<Pair<Integer, Ranking>> rankings = new ArrayList<>();
			if (focus == EvaluationFocus.Head) {
				for (int[] triple : subBatch) {
					Ranking ranking = new Ranking(new Query(this.dataset.training, -1, triple[1], triple[2]));
					updateRanking(triple[0], ranking);
					rankings.add(new Pair<>(triple[0], ranking));
				}
			} else if (focus == EvaluationFocus.Tail) {
				for (int[] triple : subBatch) {
					Ranking ranking = new Ranking(new Query(this.dataset.training, triple[0], triple[1], -1));
					updateRanking(triple[2], ranking);
					rankings.add(new Pair<>(triple[2], ranking));
				}
			} else {
				// This focus does not have sense here
				continue;
			}

			if (candidatesSet == null) {
				// This routine initializes the candidates from those computed for the first batch
				candidatesSet = new IntLinkedOpenHashSet();
				IntLinkedOpenHashSet finalCandidatesSet = candidatesSet;
				getQueryCandidatesMoreRestrictedStream(batch.get(0)[1], focus == EvaluationFocus.Head ? 0 : 2).limit(MAX_RANKING_SIZE).forEach(
						e -> {
							this.updateRankings(e, rankings);
							finalCandidatesSet.add(e);
						});
			} else {
				candidatesSet.forEach((IntConsumer) e -> this.updateRankings(e, rankings));
			}
			updateBatchEvaluationMetrics(rankings, focus, resultMetrics);
		}


		if (!batch.isEmpty()) {
			String relation = this.dataset.training.unmap(batch.get(0)[1]);
			resultMetrics.computeMRR(focus, relation);
			resultMetrics.computeCount(focus, relation);
			resultMetrics.computeHits(focus, relation);
		}
	}

	private void updateBatchEvaluationMetrics(List<Pair<Integer, Ranking>> rankings, EvaluationFocus focus,
											  EvaluationResult resultMetrics) {
		for (Pair<Integer, Ranking> solutionAndRanking: rankings) {
			Ranking ranking = solutionAndRanking.second;
			ranking.build();
			int solution = solutionAndRanking.first;
			int rank = ranking.rank(solution);
			double invRank = 1.0 / rank;
			int filteredRank = ranking.filteredRank(solution);
			double invFilteredRank = 1.0 / filteredRank;
			if (focus == EvaluationFocus.Head) {
				resultMetrics.sumOfHeadInvRanks += invRank;
				resultMetrics.headRanks++;
				resultMetrics.sumOfFilteredHeadInvRanks += invFilteredRank;
				if (rank == 1)
					resultMetrics.headHitsAt1++;
				if (rank <= 3)
					resultMetrics.headHitsAt3++;
				if (rank <= 5)
					resultMetrics.headHitsAt5++;
				if (rank <= 10)
					resultMetrics.headHitsAt10++;
				if (filteredRank == 1)
					resultMetrics.filteredHeadHitsAt1++;
				if (filteredRank <= 3)
					resultMetrics.filteredHeadHitsAt3++;
				if (filteredRank <= 5)
					resultMetrics.filteredHeadHitsAt5++;
				if (filteredRank <= 10)
					resultMetrics.filteredHeadHitsAt10++;
			} else if (focus == EvaluationFocus.Tail) {
				resultMetrics.sumOfTailInvRanks += invRank;
				resultMetrics.tailRanks++;
				resultMetrics.sumOfFilteredTailInvRanks += invFilteredRank;
				if (rank == 1)
					resultMetrics.tailHitsAt1++;
				if (rank <= 3)
					resultMetrics.tailHitsAt3++;
				if (rank <= 5)
					resultMetrics.tailHitsAt5++;
				if (rank <= 10)
					resultMetrics.tailHitsAt10++;
				if (filteredRank == 1)
					resultMetrics.filteredTailHitsAt1++;
				if (filteredRank <= 3)
					resultMetrics.filteredTailHitsAt3++;
				if (filteredRank <= 5)
					resultMetrics.filteredTailHitsAt5++;
				if (filteredRank <= 10)
					resultMetrics.filteredTailHitsAt10++;
			}
			resultMetrics.sumOfInvRanks += invRank;
			resultMetrics.sumOfFilteredInvRanks += invFilteredRank;
		}
	}

	private void updateRanking(int entity, Ranking r) {
		Query q = r.getQuery();
		Rank entityRank = getEntityScoresForQuery(entity, q);
		r.addSolution(entityRank);
	}

	private void updateRankings(int entity, List<Pair<Integer, Ranking>> rankings) {
		for (Pair<Integer, Ranking> entityAndRanking : rankings) {
			Ranking r = entityAndRanking.second;
			updateRanking(entity, r);
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
	 * @param rule
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
			System.err.println("No rules for relation " + relation);
			// Return all subjects
			returnIterator = this.dataset.training.getSubjects().iterator();
			return StreamSupport.stream(
					Spliterators.spliteratorUnknownSize(returnIterator, Spliterator.ORDERED),
					false);
		}
	}

	public Stream<Integer> getQueryCandidatesMoreRestrictedStream(int relation, int varPos) {
		Iterator<Integer> returnIterator = null;
		Stream<Integer> returnStream = Stream.empty();
		if (this.rules.containsKey(relation)) {
			for (Rule rule : this.rules.get(relation)) {
				int variable = varPos == 0 ? -1 : -2;
				int[] headAtom = rule.getHead();
				Stream<Integer> currentStream = null;
				if (headAtom[varPos] < 0) {
					// This is a constant
					currentStream =  Stream.of(headAtom[varPos]);
				} else {
					returnIterator = this.dataset.training.selectDistinctIterator(
							new IntLinkedOpenHashSet(), variable, rule.getBody());
					currentStream = this.iteratorToStream(returnIterator);
				}
				returnStream = Stream.concat(returnStream, currentStream);
			}
			return returnStream;
		} else {
			System.err.println("No rules for relation " + relation);
			// Return all subjects
			returnIterator = this.dataset.training.getSubjects().iterator();
			return StreamSupport.stream(
					Spliterators.spliteratorUnknownSize(returnIterator, Spliterator.ORDERED),
					false);
		}
	}

	private int[] normalizeAtom(int[] atom) {
		int[] newAtom = atom.clone();

		if (atom[0] < 0)
			newAtom[0] = -1;
		if (atom[2] < 0)
			newAtom[2] = -2;

		return newAtom;
	}

	private Collection<int[]> getAllCommonDomainTriplePatterns(Collection<Rule> rules, int headAtomVarPos) {
		ArrayList<int[]> triplePatterns = new ArrayList<>();
		LinkedHashSet<Triple<Integer, Integer, Integer>> seenPredicates = new LinkedHashSet<>();
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
					int[] newAtom = normalizeAtom(atom);
					Triple<Integer, Integer, Integer> triple = new Triple<>(newAtom[0], newAtom[1], newAtom[2]);
 					if (!seenPredicates.contains(triple)) {
						triplePatterns.add(newAtom);
						seenPredicates.add(triple);
					}
				}
			}
		}
		return triplePatterns;
	}

	public static String helpText(String headMessage) {
		String s = headMessage + "\n";
		s += "Evaluator <DATA_PATH> <RULES_FILE> [N_CORES=1]\n";
		s += "<DATA_PATH> is a directory that must contain at least files train.tsv and test.tsv -- optionally valid.tsv\n";
		s += "<RULES_FILE> is text file that contains one rule per line as output by AMIE";
		return s;
	}
	/**
	 * Main routine to evaluate a set of rules on link prediction
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println(helpText("Insufficient number of arguments"));
			System.exit(1);
		}
		int batchSize = Evaluator.DEFAULT_BATCH_SIZE;
		String datasetPath = args[0];
		String rulesPath = args[1];
		System.err.println("Rules file: " + rulesPath);
		int nc = 1;
		if (args.length > 2) {
			nc = Integer.parseInt(args[2]);
			System.err.println("Using " + nc + " cores");
		}
		boolean amieRules = true;
		if (args.length > 3) {
			amieRules = Boolean.parseBoolean(args[3]);
			System.err.println("Assuming AMIE's format: " + amieRules);
		}

		if (args.length > 4) {
			try {
				batchSize = Integer.parseInt(args[4]);
			} catch (NumberFormatException e) {
				System.err.println(e);
				System.err.println("Using the default batch size: " + DEFAULT_BATCH_SIZE);
			}
		}

		Instant inst1 = Instant.now();
		Evaluator e = null;
        try {
            e = Evaluator.getEvaluator(datasetPath, rulesPath, nc, amieRules);
        } catch (IOException ex) {
            System.err.println(ex);
			System.exit(2);
        }
		e.setBatchSize(batchSize);
		EvaluationResult eresult = e.evaluate();
		Instant inst2 = Instant.now();
		Gson gson = new Gson();
		gson = gson.newBuilder().serializeSpecialFloatingPointValues().create();
		String json = gson.toJson(eresult);
		try(FileWriter jsonOutFile = new FileWriter(rulesPath+ ".inference")){
			jsonOutFile.write(json);
			System.out.println(json);
			System.err.println("Elapsed Time: "+ Duration.between(inst1, inst2).toString());
		} catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
			System.exit(0);
		}
	}

}
