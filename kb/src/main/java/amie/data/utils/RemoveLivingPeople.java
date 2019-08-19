package amie.data.utils;

import java.io.IOException;
import java.util.List;

import amie.data.KB;
import amie.data.Schema;
import javatools.administrative.D;


public class RemoveLivingPeople {

	public static void main(String[] args) throws IOException {
		KB kb = amie.data.U.loadFiles(args);
		int wikicatLivingPeople = KB.map("<wikicat_Living_people>");
		List<int[]> query = KB.triples(KB.triple("?a", Schema.typeRelation, "<wikicat_Living_people>"),
				KB.triple("?a", "<diedIn>", "?b"));
		
		for (int entity : kb.selectDistinct(KB.map("?a"), query)) {
			System.err.println(D.toString(entity, Schema.typeRelationBS, wikicatLivingPeople));
			kb.delete(entity, Schema.typeRelationBS, wikicatLivingPeople);
		}
		
		kb.dump();
	}

}
