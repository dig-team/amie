package amie.typing.heuristics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import amie.data.KB;
import amie.data.U;



public abstract class TypingHeuristic {
	
	protected KB db;
        protected int defaultSupportThreshold;
	public String name = "";
	
	public TypingHeuristic(KB kb) {
            db = kb;
            defaultSupportThreshold = -1;
	}
        
        public TypingHeuristic(KB kb, int supportThreshold) {
            db = kb;
            this.defaultSupportThreshold = supportThreshold;
        }
	
	protected double getStandardConfidence(int type, List<int[]> clause, int variable) {
		return getStandardConfidence(clause, typeL(type, variable), variable, false);
	}
	
	public static int[] typeT(int type, int variable) { return KB.triple(variable, amie.data.Schema.typeRelationBS, type); }
	public static List<int[]> typeL(int type, int variable) {
		List<int[]> l = new LinkedList<>();
		l.add(typeT(type, variable));
		return l;
	}
	
	protected double getStandardConfidence(List<int[]> head, List<int[]> body, int variable) {
		return getStandardConfidence(head, body, variable, false);
	}
	
	protected double getStandardConfidence(List<int[]> head, List<int[]> body, int variable, int threshold) {
		return getStandardConfidenceWithThreshold(head, body, variable, threshold, false);
	}
	
	protected double getStandardConfidence(List<int[]> head, List<int[]> body, int variable, boolean safe) {
		return getStandardConfidenceWithThreshold(head, body, variable, defaultSupportThreshold, safe);
	}
	
	protected double getStandardConfidenceWithThreshold(List<int[]> head, List<int[]> body, int variable, int threshold, boolean safe) {
		List<int[]> bodyC = (safe) ? new LinkedList<>(body) : body;
		long bodySize = db.countDistinct(variable, bodyC);
		bodyC.addAll(head);
		long support = db.countDistinct(variable, bodyC);
		if (support < threshold || bodySize == 0)
			return 0;
		return (double) support / bodySize;
	}
	
	public abstract double evaluate(int type, List<int[]> clause, int variable);
}
