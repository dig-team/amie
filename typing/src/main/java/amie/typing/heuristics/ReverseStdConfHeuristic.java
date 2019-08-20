package amie.typing.heuristics;

import java.util.List;

import amie.data.KB;
import amie.data.Schema;
import amie.data.SetU;
import amie.data.SimpleTypingKB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;



public class ReverseStdConfHeuristic extends TypingHeuristic {

    public SimpleTypingKB localdb;
    public static Int2ObjectMap<Integer> bodySizes = new Int2ObjectOpenHashMap<>();
    
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
	public double evaluate(int type, List<int[]> clause,
			int variable) {
                // TODO Scale with max value of children
                int relation = (clause.get(0)[0] == (variable)) ? clause.get(0)[1] : KB.map(KB.unmap(clause.get(0)[1]) + "-1");
                //System.err.println(relation.toString());
                //System.err.println(phi.size());
                Integer bodySize = bodySizes.get(relation);
                if (bodySize == null) {
                    bodySize = (int) SetU.countIntersection(localdb.relations.get(relation), localdb.classes.get(Schema.topBS));
                    bodySizes.put(relation, bodySize);
                }
                int support = (int) SetU.countIntersection(localdb.relations.get(relation), localdb.classes.get(type));
                //System.err.println(support);
                if (support < defaultSupportThreshold || bodySize == 0)
			return 0;
                return ((double) support) / bodySize;
	}

}
