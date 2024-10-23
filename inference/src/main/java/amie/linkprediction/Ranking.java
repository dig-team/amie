package amie.linkprediction;

import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that represents a ranking of solutions for a link prediction
 * query of the form r(?s, o) o r(s, ?o)
 */
public class Ranking {
	private Query query;
	private TreeSet<Rank> solutions;
	private Map<Integer, Integer> ranks;
	private int maxRank;

	public Ranking(Query query) {
		this.query = query;
		this.solutions = new TreeSet<>();
		this.ranks = new HashMap<>();
		this.maxRank = -1;
	}

	public Query getQuery() {
		return query;
	}

	/**
	 * Adds a potential solution to the query into the ranking. If the solution
	 * corresponds to an entity already ranked, the largest score (and therefore
	 * rank)
	 * will be assigned to the entity
	 */
	public boolean addSolution(Rank solution) {
		return this.solutions.add(solution);
	}

	/**
	 * This method takes the entities and scores stored in the object
	 * and builds a ranking [entity -> rank]. This ranking defines a partial order,
	 * that is,
	 * different entities with the same scores are assigned the same rank.
	 */
	public void build() {
		Rank lastRank = new Rank(-1, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
		int rankId = 0;
		for (Rank r : this.solutions) {
			if (lastRank.partialCompareTo(r) != 0) {
				rankId++;
			}
			ranks.put(r.entity, rankId);
			lastRank = r;
		}
		this.maxRank = rankId;
	}

	/**
	 * It returns the rank of an entity within the ranking. The rank
	 * is a non-negative number. If an entity is not in the ranking
	 * the method returns maxRank + 1, i.e., the highest rank ever seen plus 1.
	 * Before using this method, please invoke the method build() -- required only
	 * once
	 */
	public Integer rank(int entity) {
		Integer r = this.ranks.get(entity);
		if (r == null) {
			return maxRank + 1;
		} else {
			return r;
		}
	}

	/**
	 * It determines whether this entity is physically stored in the ranking.
	 * This method should be called after having called the .build() method
	 * @param entity
	 * @return
	 */
	public boolean containsSolution(int entity) {
		return this.ranks.containsKey(entity);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		if (this.query != null)
			str.append(this.query + "\n");
		str.append("Rankings\n");
		ranks.entrySet().stream().forEach(e -> str.append(e.getKey() + "--" + e.getValue() + "\n"));

		return str.toString();
	}
}
