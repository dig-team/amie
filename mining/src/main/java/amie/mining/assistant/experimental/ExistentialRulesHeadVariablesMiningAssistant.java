package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.List;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;
import java.util.Set;

/**
 * Extension of the default mining assistant that also reports existential
 * rules,
 * i.e., rules where one of the head variables is allowed to be non-closed.
 * 
 * @author galarrag
 *
 */
public class ExistentialRulesHeadVariablesMiningAssistant extends
		DefaultMiningAssistant {

	public ExistentialRulesHeadVariablesMiningAssistant(KB dataSource) {
		super(dataSource);
	}

	@Override
	public String getDescription() {
		return "Reporting also existential rules. "
				+ "Counting on both head variables.";
	}

	@Override
	public void calculateConfidenceMetrics(Rule candidate) {
		List<int[]> antecedent = new ArrayList<int[]>();
		antecedent.addAll(candidate.getAntecedent());
		List<int[]> succedent = new ArrayList<int[]>();
		succedent.addAll(candidate.getTriples().subList(0, 1));
		double pcaDenominator = 0.0;
		double denominator = 0.0;
		int[] head = candidate.getHead();
		int[] existentialTriple = head.clone();
		int freeVarPos, countVarPos;

		if (!antecedent.isEmpty()) {
			try {
				if (KB.numVariables(head) == 2) {
					int var1, var2;
					var1 = head[KB.firstVariablePos(head)];
					var2 = head[KB.secondVariablePos(head)];
					denominator = (double) computeBodySize(var1, var2, candidate);
				} else {
					denominator = (double) kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
				}
				candidate.setBodySize((long) denominator);
			} catch (UnsupportedOperationException e) {

			}

			// In this case, still report the PCA.
			if (candidate.isClosedExcludeSpecialAtoms()) {
				countVarPos = candidate.getFunctionalVariablePosition();
				if (KB.numVariables(existentialTriple) == 1) {
					freeVarPos = KB.firstVariablePos(existentialTriple) == 0 ? 2 : 0;
				} else {
					freeVarPos = (existentialTriple[0] == candidate.getFunctionalVariable()) ? 2 : 0;
				}
				existentialTriple[freeVarPos] = kb.map("?x");

				try {
					List<int[]> redundantAtoms = Rule.redundantAtoms(existentialTriple, antecedent);
					boolean existentialQueryRedundant = false;

					// If the counting variable is in the same position of any of the unifiable
					// patterns => redundant
					for (int[] atom : redundantAtoms) {
						if (existentialTriple[countVarPos] == atom[countVarPos])
							existentialQueryRedundant = true;
					}

					if (existentialQueryRedundant) {
						pcaDenominator = denominator;
					} else {
						if (KB.numVariables(head) == 2) {
							int var1, var2;
							var1 = head[KB.firstVariablePos(head)];
							var2 = head[KB.secondVariablePos(head)];
							pcaDenominator = computePcaBodySize(var1,
									var2, candidate, antecedent, existentialTriple,
									candidate.getFunctionalVariablePosition());
						} else {
							antecedent.add(existentialTriple);
							pcaDenominator = (double) kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
						}
					}

					candidate.setPcaBodySize((long) pcaDenominator);
				} catch (UnsupportedOperationException e) {

				}
			}
		}
	}

	@Override
	public boolean testConfidenceThresholds(Rule candidate) {
		boolean addIt = true;

		if (candidate.getLength() == 1) {
			return false;
		}

		if (candidate.containsLevel2RedundantSubgraphs()) {
			return false;
		}

		calculateConfidenceMetrics(candidate);

		if (candidate.getStdConfidence() >= minStdConfidence && candidate.getPcaConfidence() >= minPcaConfidence) {
			// Now check the confidence with respect to its ancestors
			Set<Rule> ancestors = candidate.getAncestors();
			for (Rule ancestor : ancestors) {
				if ((ancestor.getLength() > 1) && ancestor.isClosedExcludeSpecialAtoms()
						&&
						(candidate.getStdConfidence() <= ancestor.getStdConfidence()
								|| candidate.getPcaConfidence() <= ancestor.getPcaConfidence())) {
					addIt = false;
					break;
				}
			}
		} else {
			return false;
		}

		return addIt;
	}
}