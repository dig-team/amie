package amie.linkprediction;

import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Iterator;

/**
 * A class that represents a ranking of solutions for a link prediction 
 * query of the form r(?s, o) o r(s, ?o)
 */
public class Ranking {
	private Query query;
	private TreeSet<Rank> solutions;
	// I will need an additional data structure to keep track of the ranks

	public Ranking(Query query, boolean descending) {
		this.query = query;
		this.solutions = new TreeSet<>();
	}

	public Query getQuery() {
		return query;
	}

	public Iterator<Rank> iterate(){
		return this.solutions.descendingIterator();
	}

	public int rank(Rank r) {
		return -1;
	}
}
