package amie.typing.heuristics;

import java.util.List;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.data.U;

public class Amplification extends TypingHeuristic {

	public Amplification(KB kb) {
		super(kb);
		name = "Amplification";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
		// TODO Auto-generated method stub
		double t, stdConf, superClassMaxConf = 0;
		stdConf = getStandardConfidence(type, clause, variable);
		if (stdConf == 0) return 0;
		for (ByteString c : amie.data.Schema.getSuperTypes(db, type)) {
			t = getStandardConfidence(c, clause, variable);
			System.err.println("A\t"+clause.get(0)[1].toString()+(variable.toString().equals("?y") ? "-1" : "")+"\t"+type.toString()+"\t"+c.toString()+"\t"+Double.toString((t == 0) ? stdConf : stdConf / t));
			if (t > superClassMaxConf) {
				superClassMaxConf = t;
			}
		}
		if (superClassMaxConf == 0)
			return stdConf;
		return stdConf / superClassMaxConf;
	}

}
