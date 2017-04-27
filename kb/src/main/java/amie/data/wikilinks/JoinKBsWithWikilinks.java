package amie.data.wikilinks;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javatools.filehandlers.TSVFile;

public class JoinKBsWithWikilinks {

	public static void main(String[] args) throws IOException {
		Set<String> subjects = new HashSet<>();
		boolean joinOnlyOnSubject = false;
		if (args.length > 2) {
			joinOnlyOnSubject = Boolean.parseBoolean(args[2]);
		}
		// Load the YAGO facts file
		try(TSVFile factsFile = new TSVFile(new File(args[0]))) {
			for (List<String> fact : factsFile) {
				if (fact.size() < 3) {
					continue;
				}
				subjects.add(fact.get(0));
				if (!joinOnlyOnSubject) {
					subjects.add(fact.get(2).substring(0, fact.get(2).length() - 1));
				}
			}
		}
		
		// Load the wikilinks file
		try(TSVFile typesFile = new TSVFile(new File(args[1]))) {
			for (List<String> fact : typesFile) {
				if (fact.size() < 3) {
					continue;
				}
				
				String subject = fact.get(0).trim();
				String object = fact.get(2).replaceFirst(" .", "");
				if (joinOnlyOnSubject) {
					if (subjects.contains(subject)) {
						System.out.println(subject + "\t" + fact.get(1).trim() + "\t" + object);
					}
				} else {
					if (subjects.contains(subject) && subjects.contains(object)) {
						System.out.println(subject + "\t" + fact.get(1).trim() + "\t" + object);
					}					
				}
			}
		}
	}
}
