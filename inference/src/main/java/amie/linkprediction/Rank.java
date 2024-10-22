package amie.linkprediction;

/**
 * An entity id labeled with a likelihood score
 */
public class Rank implements Comparable<Rank>{
	public int entity;
	public double score;
	public double tiesBreakerScore;

	public Rank(int entity, double score, double tiesBreakerScore) {
		this.entity = entity;
		this.score = score;
		this.tiesBreakerScore = tiesBreakerScore;
	}

	@Override 
	public int compareTo(Rank a){
        int firstComparison = Double.compare(this.score, a.score);
		return firstComparison != 0 ? Double.compare(this.tiesBreakerScore, 
														a.tiesBreakerScore) : firstComparison;
    }
}
