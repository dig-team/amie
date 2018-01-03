package amie.typing.heuristics;

import java.util.List;

import amie.data.KB;
import amie.data.Schema;
import amie.data.SimpleTypingKB;
import java.util.HashMap;
import java.util.Map;

import javatools.datatypes.ByteString;

public class ReverseStdConfHeuristic extends TypingHeuristic {

    public SimpleTypingKB localdb;
    public static Map<ByteString, Integer> bodySizes = new HashMap<>();
    
	public ReverseStdConfHeuristic(KB kb) {
		super(kb);
                localdb = (SimpleTypingKB) kb;
		name = "RevConf";
		// TODO Auto-generated constructor stub
	}
        
        public ReverseStdConfHeuristic(KB kb, int supportThreshold) {
		super(kb, supportThreshold);
                localdb = (SimpleTypingKB) kb;
		name = "RevConf";
		// TODO Auto-generated constructor stub
	}

	@Override
	public double evaluate(ByteString type, List<ByteString[]> clause,
			ByteString variable) {
                // TODO Scale with max value of children
                ByteString relation = (clause.get(0)[0].equals(variable)) ? clause.get(0)[1] : ByteString.of(clause.get(0)[1].toString() + "-1");
                //System.err.println(relation.toString());
                //System.err.println(phi.size());
                Integer bodySize = bodySizes.get(relation);
                if (bodySize == null) {
                    bodySize = (int) SimpleTypingKB.countIntersection(localdb.relations.get(relation), localdb.classes.get(Schema.topBS));
                    bodySizes.put(relation, bodySize);
                }
                int support = (int) SimpleTypingKB.countIntersection(localdb.relations.get(relation), localdb.classes.get(type));
                //System.err.println(support);
                if (support < defaultSupportThreshold || bodySize == 0)
			return 0;
                return ((double) support) / bodySize;
	}

}
