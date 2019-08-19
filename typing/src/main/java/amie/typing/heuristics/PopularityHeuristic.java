package amie.typing.heuristics;

import java.util.ArrayList;
import java.util.List;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.IntSet;



public class PopularityHeuristic extends TypingHeuristic {

	public PopularityHeuristic(KB kb) {
		super(kb);
		name = "Popularity";
		// TODO Auto-generated constructor stub
	}
	
	public static final int popularityRelationBS = KB.map("<isPopular>");
	
	public PopularityHeuristic(KB kb, int popularityThreshold) {
		super(kb);
		int variable = KB.map("?v1");
		List<int[]> typeClause = new ArrayList<>(1);
		typeClause.add(KB.triple(variable, amie.data.Schema.typeRelationBS, KB.map("?v2")));
		IntSet entities = db.selectDistinct(variable, typeClause);
		for (int e : entities) {
			if (db.count(KB.triple(e, KB.map("?x"), KB.map("?y"))) 
					+ db.count(KB.triple(KB.map("?x"), KB.map("?y"), e)) > popularityThreshold)
				db.add(KB.triple(e, popularityRelationBS, KB.map("")));
		}
		name = "Popularity";
	}
        
        public PopularityHeuristic(KB kb, int popularityThreshold, int supportThreshold) {
            this(kb, popularityThreshold);
            this.defaultSupportThreshold = supportThreshold;
        }

	@Override
	public double evaluate(int type, List<int[]> clause,
			int variable) {
		List<int[]> body = typeL(type, variable);
		body.add(KB.triple(variable, popularityRelationBS, KB.map("")));
		return getStandardConfidence(clause, body, variable);
	}

}
