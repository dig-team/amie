package amie.data.utils;

import java.io.IOException;
import java.util.List;

import amie.data.KB;
import amie.data.Schema;
import javatools.administrative.D;
import javatools.datatypes.ByteString;

public class RemoveLivingPeople {

	public static void main(String[] args) throws IOException {
		KB kb = amie.data.U.loadFiles(args);
		ByteString wikicatLivingPeople = ByteString.of("<wikicat_Living_people>");
		List<ByteString[]> query = KB.triples(KB.triple("?a", Schema.typeRelation, "<wikicat_Living_people>"),
				KB.triple("?a", "<diedIn>", "?b"));
		
		for (ByteString entity : kb.selectDistinct(ByteString.of("?a"), query)) {
			System.err.println(D.toString(entity, Schema.typeRelationBS, wikicatLivingPeople));
			kb.delete(entity, Schema.typeRelationBS, wikicatLivingPeople);
		}
		
		kb.dump();
	}

}
