package amie.typing.heuristics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import amie.data.KB;
import amie.data.U;

import javatools.datatypes.ByteString;

public class PopularityHeuristic extends TypingHeuristic {

	public PopularityHeuristic(KB kb) {
		super(kb);
		name = "Popularity";
		// TODO Auto-generated constructor stub
	}
	
	public static final ByteString popularityRelationBS = ByteString.of("<isPopular>");
	
	public PopularityHeuristic(KB kb, int popularityThreshold) {
		super(kb);
		ByteString variable = ByteString.of("?v1");
		List<ByteString[]> typeClause = new ArrayList<>(1);
		typeClause.add(KB.triple(variable, amie.data.Schema.typeRelationBS, ByteString.of("?v2")));
		IntSet entities = db.selectDistinct(variable, typeClause);
		for (ByteString e : entities) {
			if (db.count(KB.triple(e, ByteString.of("?x"), ByteString.of("?y"))) 
					+ db.count(KB.triple(ByteString.of("?x"), ByteString.of("?y"), e)) > popularityThreshold)
				db.add(KB.triple(e, popularityRelationBS, ByteString.of("")));
		}
		name = "Popularity";
	}
        
        public PopularityHeuristic(KB kb, int popularityThreshold, int supportThreshold) {
            this(kb, popularityThreshold);
            this.defaultSupportThreshold = supportThreshold;
        }

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
		List<ByteString[]> body = typeL(type, variable);
		body.add(KB.triple(variable, popularityRelationBS, ByteString.of("")));
		return getStandardConfidence(clause, body, variable);
	}

}
