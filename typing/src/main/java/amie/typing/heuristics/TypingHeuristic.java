package amie.typing.heuristics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import amie.data.KB;
import amie.data.U;

import javatools.datatypes.ByteString;

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
	
	protected double getStandardConfidence(ByteString type, List<ByteString[]> clause, ByteString variable) {
		return getStandardConfidence(clause, typeL(type, variable), variable, false);
	}
	
	public static ByteString[] typeT(ByteString type, ByteString variable) { return KB.triple(variable, amie.data.Schema.typeRelationBS, type); }
	public static List<ByteString[]> typeL(ByteString type, ByteString variable) {
		List<ByteString[]> l = new LinkedList<>();
		l.add(typeT(type, variable));
		return l;
	}
	
	protected double getStandardConfidence(List<ByteString[]> head, List<ByteString[]> body, ByteString variable) {
		return getStandardConfidence(head, body, variable, false);
	}
	
	protected double getStandardConfidence(List<ByteString[]> head, List<ByteString[]> body, ByteString variable, int threshold) {
		return getStandardConfidenceWithThreshold(head, body, variable, threshold, false);
	}
	
	protected double getStandardConfidence(List<ByteString[]> head, List<ByteString[]> body, ByteString variable, boolean safe) {
		return getStandardConfidenceWithThreshold(head, body, variable, defaultSupportThreshold, safe);
	}
	
	protected double getStandardConfidenceWithThreshold(List<ByteString[]> head, List<ByteString[]> body, ByteString variable, int threshold, boolean safe) {
		List<ByteString[]> bodyC = (safe) ? new LinkedList<>(body) : body;
		long bodySize = db.countDistinct(variable, bodyC);
		bodyC.addAll(head);
		long support = db.countDistinct(variable, bodyC);
		if (support < threshold || bodySize == 0)
			return 0;
		return (double) support / bodySize;
	}
	
	public abstract double evaluate(ByteString type, List<ByteString[]> clause, ByteString variable);
}
