package amie.mining.assistant.pyclause;

import java.util.Collection;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class AnyBurlMiningAssistant extends DefaultMiningAssistant {

	public AnyBurlMiningAssistant(KB dataSource) {
		super(dataSource);
	}

	/**
	 * Returns all candidates obtained by adding a closing edge (an edge with two
	 * existing variables).
	 * 
	 * @param rule
	 * @param minSupportThreshold
	 * @param output
	 */
	@MiningOperator(name = "closing")
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (rule.isEmpty()) {
			throw new IllegalArgumentException("This method expects a non-empty query");
		}

		if (!isNotTooLong(rule)) {
			return;
		}

		// If the object or the subject is bounded, then
		if (KB.numVariables(rule.getHead()) < 2) {
			return;
		}

		if (rule.isClosed(false) || this.enforceConstants) {
			return;
		}

		IntList sourceVariables = null;
		IntList targetVariables = null;

		if (rule.getRealLength() > 1) {
			sourceVariables = new IntArrayList();
			targetVariables = new IntArrayList();
			sourceVariables.add(rule.getLastRealTriplePattern()[2]);
			targetVariables.add(rule.getHead()[2]);
		} else {
			sourceVariables = rule.getOpenVariables();
			targetVariables = rule.getOpenableVariables();
		}

		super.getClosingAtoms(rule, minSupportThreshold, sourceVariables, targetVariables, output);
	}

	/**
	 * Returns all candidates obtained by adding a new triple pattern to the query
	 * 
	 * @param rule           and will therefore predict too many new facts with
	 *                       scarce evidence,
	 * @param minCardinality
	 * @param output
	 */
	@MiningOperator(name = "dangling")
	public void getDanglingAtoms(Rule rule, double minCardinality, Collection<Rule> output) {
		int[] newEdge = rule.fullyUnboundTriplePattern();

		if (rule.isEmpty()) {
			throw new IllegalArgumentException("This method expects a non-empty query");
		}

		if (!isNotTooLong(rule))
			return;

		// Pruning by maximum length for the \mathcal{O}_D operator.
		if (rule.getRealLength() == this.maxDepth - 1) {
			if (this.exploitMaxLengthOption) {
				if (!rule.getOpenVariables().isEmpty()
						&& !this.allowConstants
						&& !this.enforceConstants) {
					return;
				}
			}
		}

		IntList joinVariables = new IntArrayList();

		// Then do it for all values
		if (rule.isClosed(true)) {
			return;
		}

		int[] lastTriplePattern = rule.getLastRealTriplePattern();
		if (KB.numVariables(lastTriplePattern) == 0) {
			throw new IllegalArgumentException("This rule has a fully instantiated atom: " + rule.toString());
		}

		if (rule.getRealLength() > 1) {
			if (rule.getOpenVariables().contains(lastTriplePattern[0])) {
				joinVariables.add(lastTriplePattern[0]);
			} else {
				joinVariables.add(lastTriplePattern[2]);
			}
		} else {
			if (KB.isVariable(rule.getHead()[0]))
				joinVariables.add(rule.getHead()[0]);
			else
				joinVariables.add(rule.getHead()[2]);
		}

		int[] joinPositions = null;
		if (KB.isVariable(lastTriplePattern[0])) {
			if (KB.isVariable(lastTriplePattern[2])) {
				joinPositions = new int[] { 0, 2 };
			} else {
				joinPositions = new int[] { 0 };
			}
		} else {
			joinPositions = new int[] { 2 };
		}

		super.getDanglingAtoms(rule, newEdge, minCardinality, joinVariables, joinPositions, output);
	}

	/**
	 * Returns all candidates obtained by instantiating the dangling variable of the
	 * last added
	 * triple pattern in the rule
	 * 
	 * @param rule
	 * @param minSupportThreshold
	 * @param danglingEdges
	 * @param output
	 */
	@MiningOperator(name = "instantiated", dependency = "dangling")
	public void getInstantiatedAtoms(Rule rule, double minSupportThreshold,
			Collection<Rule> danglingEdges, Collection<Rule> output) {
		if (!canAddInstantiatedAtoms()) {
			return;
		}
		if (KB.numVariables(rule.getHead()) > 1) {
			return;
		}

		// AnyBurl cares only about rules up to 3 atoms for constants
		if (rule.getRealLength() >= this.maxDepthConst) {
			return;
		}

		IntList queryFreshVariables = rule.getOpenVariables();
		if (this.exploitMaxLengthOption
				|| rule.getRealLength() < this.maxDepth - 1
				|| queryFreshVariables.size() < 2) {
			for (Rule candidate : danglingEdges) {
				// Find the dangling position of the query
				int lastTriplePatternIndex = candidate.getLastRealTriplePatternIndex();
				int[] lastTriplePattern = candidate.getTriples().get(lastTriplePatternIndex);

				IntList candidateFreshVariables = candidate.getOpenVariables();
				int danglingPosition = 0;
				if (candidateFreshVariables.contains(lastTriplePattern[0])) {
					danglingPosition = 0;
				} else if (candidateFreshVariables.contains(lastTriplePattern[2])) {
					danglingPosition = 2;
				} else {
					throw new IllegalArgumentException("The query " + rule.getRuleString() +
							" does not contain fresh variables in the last triple pattern.");
				}
				if (optimAdaptiveInstantiations) {
					getInstantiatedAtoms(candidate, candidate,
							lastTriplePatternIndex, danglingPosition,
							candidate.getSupport() / 5, output);
				} else {
					getInstantiatedAtoms(candidate, candidate,
							lastTriplePatternIndex, danglingPosition,
							minSupportThreshold, output);
				}
			}
		}
	}

}
