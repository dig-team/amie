package amie.typing.heuristics;

import java.util.List;


import amie.data.KB;
import it.unimi.dsi.fastutil.ints.IntSet;

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
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		double sc = getStandardConfidence(typeL(type, variable), clause, variable, true);
		double t,scm = 0;
		IntSet subtypes = amie.data.Schema.getSubtypes(db, type);
		clause.add(typeT(type, variable));
		for (int subType : subtypes) {
			t = getStandardConfidence(typeL(subType, variable), clause, variable, true);
			scm += t;
		}
		clause.remove(clause.size()-1);
		if (scm == 0)
			return 1;
		return sc * subtypes.size() / scm;
	}

}
