package amie.data.wikilinks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import javatools.filehandlers.TSVFile;

public class JoinKBsWithTypes {

	public static void main(String[] args) throws IOException {
		Set<String> subjects = new HashSet<>();
		// Load the YAGO facts file
		try(TSVFile factsFile = new TSVFile(new File(args[0]))) {
			for (List<String> fact : factsFile) {
				if (fact.size() < 3) {
					continue;
				}
				subjects.add(fact.get(0));
				subjects.add(fact.get(2));
			}
		}
		
		List<String> types = Collections.emptyList();
		if (!args[1].equals("-")) {
			types = Files.readAllLines(Paths.get(args[1]), Charset.defaultCharset());
		}
		
		// Load the YAGO types file
		try(TSVFile typesFile = new TSVFile(new File(args[2]))) {
			for (List<String> fact : typesFile) {
				if (fact.size() < 3) {
					continue;
				}
				String subject = fact.get(1).trim();
				String object = fact.get(3).trim();
				
				if (!types.isEmpty()) {
					if (subjects.contains(subject) && types.contains(object)) {
						System.out.println(subject + "\t" + fact.get(2).trim() + "\t" + object);
					}
				} else {
					if (subjects.contains(subject)) {
						System.out.println(subject + "\t" + fact.get(2).trim() + "\t" + object);
					}
				}
			}
		}
	}
}
