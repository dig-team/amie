package amie.linkprediction;

import amie.data.AbstractKB;
/**
 * A query in the context of a link prediction ranking evaluation, namely
 * a triple where either the subject or the object has been been masked and
 * should be predicted.
 */

public class Query {
	private AbstractKB kb;
	public int[] triple;

	public Query(AbstractKB kb, int h, int r, int t) {
		this.kb = kb;
		this.triple = new int[]{h, r, t};
	}

	public String toString() {
		return "" + this.kb.unmap(this.triple[1]) + 
		"(" + (!AbstractKB.isVariable(this.triple[0]) ? this.kb.unmap(this.triple[0]) : "?s") + ", " 
		+ (!AbstractKB.isVariable(this.triple[2]) ? this.kb.unmap(this.triple[2]) : "?o")  + ")"; 
	}
}
