package amie.data.utils;

import java.io.IOException;
import java.util.List;

import amie.data.KB;
import amie.data.Schema;
import amie.data.javatools.administrative.D;


public class RemoveLivingPeople {

	public static void main(String[] args) throws IOException {
		KB kb = amie.data.U.loadFiles(args);
		int wikicatLivingPeople = kb.map("<wikicat_Living_people>");
		List<int[]> query = KB.triples(kb.triple("?a", Schema.typeRelation, "<wikicat_Living_people>"),
				kb.triple("?a", "<diedIn>", "?b"));
		
		for (int entity : kb.selectDistinct(kb.map("?a"), query)) {
			System.err.println(D.toString(entity, kb.schema.typeRelationBS, wikicatLivingPeople));
			kb.delete(entity, kb.schema.typeRelationBS, wikicatLivingPeople);
		}
		
		kb.dump();
	}

}
