package amie.linkprediction;

/**
 * An entity id labeled with a likelihood score
 */
public class Rank implements Comparable<Rank>{
	public int entity;
	public double score;

	@Override 
	public int compareTo(Rank a){
        return Double.compare(this.score, a.score);
    }
}
