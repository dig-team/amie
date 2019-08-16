package amie.typing.heuristics;

import java.util.List;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.data.U;

public class TrueType extends TypingHeuristic {

	public static final ByteString trueTypeBS = KB.map("<TrueType>");
	
	public TrueType(KB kb) {
		super(kb);
		name = "TrueType";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
		ByteString t = amie.data.Schema.typeRelationBS;
		amie.data.Schema.typeRelationBS = trueTypeBS;
		double res = getStandardConfidence(type, clause, variable);
		amie.data.Schema.typeRelationBS = t;
		// TODO Auto-generated method stub
		return res;
	}

}
