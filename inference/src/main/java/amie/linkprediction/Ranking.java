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
	private int[] nSolutionsUpTo;

	public Ranking(Query query) {
		this.query = query;
		this.solutions = new TreeSet<>();
		this.ranks = new HashMap<>();
		this.nSolutionsUpTo = null; // We need to know the size of the ranking to allocate memory for this
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
	 * that is, different entities with the same scores are assigned the same rank.
	 */
	public void build() {
		this.nSolutionsUpTo = new int[this.solutions.size()];
		int nSolutionsUpToHere = 0;
		int rankId = 0;
		for (Rank r : this.solutions) {
			// Required for filtered rank
			if (this.isSolutionForQuery(r.entity))
				nSolutionsUpToHere++;

			this.nSolutionsUpTo[rankId] = nSolutionsUpToHere;
			rankId++;
			this.ranks.put(r.entity, rankId);
		}
	}

	/**
	 * Determines if a solution for a query has been observed in the
	 * training KG
	 * @param entity
	 * @return
	 */
	public boolean isSolutionForQuery(int entity) {
		if (this.query == null)
			return false;
		int[] triple = this.query.instantiate(entity);
		return this.query.kb.count(triple) > 0;
	}

	/**
	 * It returns the rank of an entity within the ranking. The rank
	 * is a non-negative number. If an entity is not in the ranking
	 * the method returns maxRank + 1, i.e., the highest rank ever seen plus 1.
	 * Before using this method, please invoke the method build() -- required only
	 * once
	 */
	public Integer rank(int entity) {
		int maxRank = this.solutions.size();
		Integer r = this.ranks.get(entity);
		if (r == null) {
			return maxRank;
		} else {
			return r;
		}
	}

	/**
	 * It returns the filtered rank of an entity within the ranking. The rank
	 * is a non-negative number. This rank is filtered because any solution that
	 * is in the training knowledge base is excluded. If an entity is not in the ranking
	 * the method returns maxRank + 1, i.e., the highest rank ever seen plus 1.
	 * Before using this method, please invoke the method build() -- required only
	 * once
	 */
	public Integer filteredRank(int entity) {
		int rank = this.rank(entity);
		int nSolutions = this.getNSolutionsUpToRank(rank);
		return rank - nSolutions;
	}

	/**
	 * It computes the number of solutions that correspond observed triples such
	 * that their rank is smaller than the provided rank
	 * @param rank
	 * @return
	 */
	private int getNSolutionsUpToRank(int rank) {
		if (rank > this.solutions.size()) {
			return this.nSolutionsUpTo[nSolutionsUpTo.length - 1];
		} else {
			return this.nSolutionsUpTo[rank - 1];
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
		ranks.entrySet().stream().forEach(e -> str.append(this.query.kb.unmap(e.getKey()) + "--" + e.getValue() + "\n"));

		return str.toString();
	}
}
