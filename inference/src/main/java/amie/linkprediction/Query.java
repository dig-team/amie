package amie.linkprediction;

import amie.data.AbstractKB;

/**
 * A query in the context of a link prediction ranking evaluation, namely
 * a triple where either the subject or the object has been been masked and
 * should be predicted.
 */

public class Query {
	AbstractKB kb;
	public int[] triple;

	public Query(AbstractKB kb, int h, int r, int t) {
		this.kb = kb;
		if ((h < 0 && t < 0) || r < 0) {
			throw new IllegalArgumentException(
					"Valid queries can only have variables in one position (subject or object)");
		}
		this.triple = new int[] { h, r, t };
	}

	public int variablePosition() {
		return triple[0] < 0 ? 0 : 2;
	}

	public int constantPosition() {
		return variablePosition() == 0 ? 2 : 0;
	}

	public String toString() {
		return "" + this.kb.unmap(this.triple[1]) +
				"(" + (!AbstractKB.isVariable(this.triple[0]) ? this.kb.unmap(this.triple[0]) : "?s") + ", "
				+ (!AbstractKB.isVariable(this.triple[2]) ? this.kb.unmap(this.triple[2]) : "?o") + ")";
	}

	public int[] instantiate(int entity) {
		int varPos = this.variablePosition();
		int[] newTriple = triple.clone();
		newTriple[varPos] = entity;
		return newTriple;
	}
}
