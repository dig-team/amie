package amie.typing.heuristics;

import java.util.List;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.data.U;

public class Spread extends TypingHeuristic {

	public Spread(KB kb) {
		super(kb);
		name = "Spread";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<int[]> clause,
			ByteString variable) {
		// TODO Auto-generated method stub
		double sc = getStandardConfidence(typeL(type, variable), clause, variable, true);
		double t,scm = 0;
		clause.add(typeT(type, variable));
		for (ByteString subType : amie.data.Schema.getSubtypes(db, type)) {
			t = getStandardConfidence(typeL(subType, variable), clause, variable, true);
			System.err.println("S\t"+clause.get(0)[1].toString()+(variable.toString().equals("?y") ? "-1" : "")+"\t"+type.toString()+"\t"+subType.toString()+"\t"+Double.toString((t == 0) ? sc : sc / t));
			if (t > scm)
				scm = t;
		}
		clause.remove(clause.size()-1);
		if (scm == 0)
			return sc;
		return sc / scm;
	}

}
