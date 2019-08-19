package amie.typing.heuristics;

import java.util.List;


import amie.data.KB;

public class TrueType extends TypingHeuristic {

	public static final int trueTypeBS = KB.map("<TrueType>");
	
	public TrueType(KB kb) {
		super(kb);
		name = "TrueType";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		int t = amie.data.Schema.typeRelationBS;
		amie.data.Schema.typeRelationBS = trueTypeBS;
		double res = getStandardConfidence(type, clause, variable);
		amie.data.Schema.typeRelationBS = t;
		// TODO Auto-generated method stub
		return res;
	}

}
