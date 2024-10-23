package amie.linkprediction;

/**
 * An entity id labeled with a likelihood score
 */
public class Rank implements Comparable<Rank> {
	public int entity;
	public double score;
	public double tiesBreakerScore;

	public Rank(int entity, double score, double tiesBreakerScore) {
		this.entity = entity;
		this.score = score;
		this.tiesBreakerScore = tiesBreakerScore;
	}

	@Override
	public int compareTo(Rank a) {
		int firstComparison = Double.compare(a.score, this.score);
		if (firstComparison == 0) {
			int secondComparison = Double.compare(a.tiesBreakerScore,
					this.tiesBreakerScore);
			return secondComparison == 0 ? Integer.compare(a.entity, this.entity) : secondComparison;
		} else {
			return firstComparison;
		}
	}

	public int partialCompareTo(Rank a) {
		int firstComparison = Double.compare(a.score, this.score);
		return firstComparison == 0 ? Double.compare(a.tiesBreakerScore,
				this.tiesBreakerScore) : firstComparison;
	}

	@Override
	public String toString() {
		return "<" + entity + ", " + score + ", " + tiesBreakerScore + ">";
	}
}
