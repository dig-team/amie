package amie.data;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

public class QueryKB {

	public static void main(String[] args) throws IOException {
		String query = args[0];
		KB kb = amie.data.U.loadFiles(args, 1);
		
		String[] queryParts = query.split("\\|");
		String variables = queryParts[0];
		String[] variableParts = variables.split(",");
		String selection = queryParts[1].trim();
		List<ByteString[]> selectionAtoms = KB.triples(selection);
		System.out.println("Projection variables: " + KB.toString(variableParts));
		System.out.println("Conditions: " + KB.toString(selectionAtoms));
		if (variableParts.length == 1) {
			Set<ByteString> result = kb.selectDistinct(ByteString.of(variables.trim()), selectionAtoms);
			System.out.println(result);
			System.out.println(result.size() + " results");
		} else if (variableParts.length == 2) {
			Map<ByteString, IntHashMap<ByteString>> result = kb.selectDistinct(ByteString.of(variableParts[0].trim()), 
					ByteString.of(variableParts[1].trim()), selectionAtoms);
			System.out.println(result);
			System.out.println(KB.aggregate(result) + " results");			
		}
	}
}
