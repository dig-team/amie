package amie.typing.heuristics;

import java.util.List;
import java.util.Set;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.data.U;

/**
 * Harmonic mean of the Spread measure
 * @author jlajus
 *
 */
public class HSpread extends TypingHeuristic {

	public HSpread(KB kb) {
		super(kb);
		name = "HSpread";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<int[]> clause,
			ByteString variable) {
		double sc = getStandardConfidence(typeL(type, variable), clause, variable, true);
		double t,scm = 0;
		IntSet subtypes = amie.data.Schema.getSubtypes(db, type);
		clause.add(typeT(type, variable));
		for (ByteString subType : subtypes) {
			t = getStandardConfidence(typeL(subType, variable), clause, variable, true);
			scm += t;
		}
		clause.remove(clause.size()-1);
		if (scm == 0)
			return 1;
		return sc * subtypes.size() / scm;
	}

}
