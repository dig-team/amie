package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.IntSet;


public class TrivialRelevanceCalculator {

	public static void main(String[] args) throws IOException {
		KB kb = new KB();
		kb.load(new File(args[0]));
		List<int[]> query = KB.triples(KB.triple("?s", "?p", "?o"));
		IntSet allEntities = kb.selectDistinct(KB.map("?s"), query);
		allEntities.addAll(kb.selectDistinct(KB.map("?o"), query));
		for (int entity : allEntities) {			
			int nFacts = (int) kb.count(entity, KB.map("?p"), KB.map("?o"));
			nFacts += kb.count(KB.map("?s"), KB.map("?p"), entity);
			
			System.out.println(entity + "\t<hasNumberOfFacts>\t" + nFacts);								
		}
	}
}
