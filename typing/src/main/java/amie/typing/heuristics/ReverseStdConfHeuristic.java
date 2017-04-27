package amie.typing.heuristics;

import java.util.List;

import amie.data.KB;
import amie.data.U;

import javatools.datatypes.ByteString;

public class ReverseStdConfHeuristic extends TypingHeuristic {

	public ReverseStdConfHeuristic(KB kb) {
		super(kb);
		name = "RevConf";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
		// TODO Scale with max value of children
		double res = getStandardConfidence(typeL(type, variable), clause, variable, true);
		for (ByteString subType : amie.data.Schema.getSubtypes(db, type)) {
			if (res <= getStandardConfidence(typeL(subType, variable), clause, variable, true))
				return 0;
		}
		return res;
	}

}
