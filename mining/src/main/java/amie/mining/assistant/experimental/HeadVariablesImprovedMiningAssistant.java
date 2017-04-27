package amie.mining.assistant.experimental;

import java.util.List;

import javatools.datatypes.ByteString;
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

	protected double computePcaBodySize(ByteString var1, 
			ByteString var2, Rule query, 
			List<ByteString[]> antecedent, 
			ByteString[] existentialTriple, 
			int nonExistentialPosition) {
		antecedent.add(existentialTriple);
		ByteString[] typeConstraint1, typeConstraint2;
		typeConstraint1 = new ByteString[3];
		typeConstraint2 = new ByteString[3];
		typeConstraint1[1] = typeConstraint2[1] = ByteString.of("rdf:type");
		typeConstraint1[2] = typeConstraint2[2] = ByteString.of("?w");
		typeConstraint1[0] = existentialTriple[nonExistentialPosition == 0 ? 2 : 0];
		typeConstraint2[0] = existentialTriple[nonExistentialPosition].equals(var1) ? var2 : var1;
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