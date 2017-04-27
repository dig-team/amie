package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import amie.data.KB;
import javatools.datatypes.ByteString;

public class TrivialRelevanceCalculator {

	public static void main(String[] args) throws IOException {
		KB kb = new KB();
		kb.load(new File(args[0]));
		List<ByteString[]> query = KB.triples(KB.triple("?s", "?p", "?o"));
		Set<ByteString> allEntities = kb.selectDistinct(ByteString.of("?s"), query);
		allEntities.addAll(kb.selectDistinct(ByteString.of("?o"), query));
		for (ByteString entity : allEntities) {			
			int nFacts = (int) kb.count(entity, ByteString.of("?p"), ByteString.of("?o"));
			nFacts += kb.count(ByteString.of("?s"), ByteString.of("?p"), ByteString.of(entity));
			
			System.out.println(entity + "\t<hasNumberOfFacts>\t" + nFacts);								
		}
	}
}
