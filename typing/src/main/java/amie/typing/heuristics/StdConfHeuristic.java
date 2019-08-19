package amie.typing.heuristics;

import java.util.List;

import amie.data.KB;


public class StdConfHeuristic extends TypingHeuristic {
	
	public StdConfHeuristic(KB kb) {
		super(kb);
		name = "StdConf";
		// TODO Auto-generated constructor stub
	}
        
        public StdConfHeuristic(KB kb, int supportThreshold) {
		super(kb, supportThreshold);
		name = "StdConf";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		return getStandardConfidence(type, clause, variable);
		// TODO Auto-generated method stub
	}

}
