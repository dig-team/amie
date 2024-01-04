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
	 * Returns all candidates obtained by adding a closing edge (an edge with two existing variables).
	 * @param rule
	 * @param minSupportThreshold
	 * @param output
	 */
	@MiningOperator(name="closing")
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (rule.isEmpty()) {
			throw new IllegalArgumentException("This method expects a non-empty query");
		}
		
		if (!isNotTooLong(rule))
			return;
		
		if (this.enforceConstants) {
			return;
		}
		
		if (rule.isClosed(false)) {
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
	 * @param rule and will therefore predict too many new facts with scarce evidence, 
	 * @param minCardinality
	 * @param output
	 */
	@MiningOperator(name="dangling")
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
				if(!rule.getOpenVariables().isEmpty() 
						&& !this.allowConstants 
						&& !this.enforceConstants) {
					return;
				}
			}
		}

		IntList joinVariables = new IntArrayList();
		
		//Then do it for all values
		if (rule.isClosed(true)) {
			return;
		}

		if (rule.getRealLength() > 1) {
			joinVariables.add(rule.getLastRealTriplePattern()[2]);
		} else {
			joinVariables.add(rule.getHead()[0]);
		}

		int[] joinPositions = new int[]{0};
		
		super.getDanglingAtoms(rule, newEdge, minCardinality, joinVariables, joinPositions, output);
	}
	
}
