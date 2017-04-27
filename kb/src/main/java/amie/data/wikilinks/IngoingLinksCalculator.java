package amie.data.wikilinks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javatools.datatypes.IntHashMap;
import javatools.filehandlers.TSVFile;

public class IngoingLinksCalculator {

	public static void main(String[] args) throws IOException {
		try(TSVFile tsv = new TSVFile(new File(args[0]))) {
			IntHashMap<String> ingoingLinksMap = new IntHashMap<>();
			for (List<String> line : tsv) {
				String relation = line.get(2);
				if (relation.equals("<linksTo>")) {
					String object = line.get(3);
					ingoingLinksMap.increase(object);
				}
			}
			
			for (String entity : ingoingLinksMap) {
				System.out.println(entity + 
						"\t<hasIngoingLinks>\t\"" + 
						ingoingLinksMap.get(entity) + 
						"\"^^xsd:integer");
			}
		}

	}
}
