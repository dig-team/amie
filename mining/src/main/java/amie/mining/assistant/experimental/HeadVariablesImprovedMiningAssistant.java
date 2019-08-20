package amie.mining.assistant.experimental;

import java.util.List;


import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

public class HeadVariablesImprovedMiningAssistant extends
		DefaultMiningAssistant {

	public HeadVariablesImprovedMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	@Override
	public String getDescription() {
        return "Counting on both head variables. "
        		+ "Adding type constraints when calculating support and confidence.";
	}

	protected double computePcaBodySize(int var1, 
			int var2, Rule query, 
			List<int[]> antecedent, 
			int[] existentialTriple, 
			int nonExistentialPosition) {
		antecedent.add(existentialTriple);
		int[] typeConstraint1, typeConstraint2;
		typeConstraint1 = new int[3];
		typeConstraint2 = new int[3];
		typeConstraint1[1] = typeConstraint2[1] = KB.map("rdf:type");
		typeConstraint1[2] = typeConstraint2[2] = KB.map("?w");
		typeConstraint1[0] = existentialTriple[nonExistentialPosition == 0 ? 2 : 0];
		typeConstraint2[0] = existentialTriple[nonExistentialPosition] == (var1) ? var2 : var1;
		antecedent.add(typeConstraint1);
		antecedent.add(typeConstraint2);
		long result = kb.countDistinctPairs(var1, var2, antecedent);
		if (result == 0) {
			antecedent.remove(antecedent.size() - 1);
			antecedent.remove(antecedent.size() - 1);
			result = kb.countDistinctPairs(var1, var2, antecedent);
		}
		return (double) result;
	}
}