package amie.typing.heuristics;

import java.util.List;


import amie.data.KB;

public class Amplification extends TypingHeuristic {

	public Amplification(KB kb) {
		super(kb);
		name = "Amplification";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		// TODO Auto-generated method stub
		double t, stdConf, superClassMaxConf = 0;
		stdConf = getStandardConfidence(type, clause, variable);
		if (stdConf == 0) return 0;
		for (int c : amie.data.Schema.getSuperTypes(db, type)) {
			t = getStandardConfidence(c, clause, variable);
			System.err.println("A\t"+KB.unmap(clause.get(0)[1])
                                + ((variable == KB.map("?y")) ? "-1" : "")
                                +"\t"+KB.unmap(type)
                                +"\t"+KB.unmap(c)+"\t"+Double.toString((t == 0) ? stdConf : stdConf / t));
			if (t > superClassMaxConf) {
				superClassMaxConf = t;
			}
		}
		if (superClassMaxConf == 0)
			return stdConf;
		return stdConf / superClassMaxConf;
	}

}
