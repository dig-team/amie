package amie.typing.heuristics;

import java.util.List;


import amie.data.KB;

public class Spread extends TypingHeuristic {

	public Spread(KB kb) {
		super(kb);
		name = "Spread";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		// TODO Auto-generated method stub
		double sc = getStandardConfidence(typeL(type, variable), clause, variable, true);
		double t,scm = 0;
		clause.add(typeT(type, variable));
		for (int subType : amie.data.Schema.getSubtypes(db, type)) {
			t = getStandardConfidence(typeL(subType, variable), clause, variable, true);
                        System.err.println("S\t"+KB.unmap(clause.get(0)[1])
                                + ((variable == KB.map("?y")) ? "-1" : "")
                                +"\t"+KB.unmap(type)
                                +"\t"+KB.unmap(subType)+"\t"+Double.toString((t == 0) ? sc : sc / t));
			if (t > scm)
				scm = t;
		}
		clause.remove(clause.size()-1);
		if (scm == 0)
			return sc;
		return sc / scm;
	}

}
