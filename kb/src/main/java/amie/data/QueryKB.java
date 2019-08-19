package amie.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.List;

public class QueryKB {

	public static void main(String[] args) throws IOException {
		String query = args[0];
		KB kb = amie.data.U.loadFiles(args, 1);
		
		String[] queryParts = query.split("\\|");
		String variables = queryParts[0];
		String[] variableParts = variables.split(",");
		String selection = queryParts[1].trim();
		List<int[]> selectionAtoms = KB.triples(selection);
		System.out.println("Projection variables: " + KB.toString(variableParts));
		System.out.println("Conditions: " + KB.toString(selectionAtoms));
		if (variableParts.length == 1) {
			IntSet result = kb.selectDistinct(KB.map(variables.trim()), selectionAtoms);
			System.out.println(result);
			System.out.println(result.size() + " results");
		} else if (variableParts.length == 2) {
			Int2ObjectMap<IntSet> result = kb.selectDistinct(KB.map(variableParts[0].trim()), 
					KB.map(variableParts[1].trim()), selectionAtoms);
			System.out.println(result);
			System.out.println(KB.aggregate(result) + " results");			
		}
	}
}
