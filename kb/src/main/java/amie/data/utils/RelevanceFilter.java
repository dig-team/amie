package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import javatools.datatypes.Triple;
import javatools.filehandlers.TSVFile;

/**
 * Given a file with the relevance score of a set of entities and a KB, 
 * it outputs the facts corresponding to entities on the top X% of the
 * relevance ranking.
 * 
 * @author galarrag
 *
 */
public class RelevanceFilter {

	public static void main(String[] args) throws IOException {
		List<Triple<String, String, Double>> relevanceList = new 
				ArrayList<>();
		try(TSVFile relevanceFile = new TSVFile(new File(args[0]))) {
			for (List<String> line : relevanceFile) {
				if (line.size() < 3)
					continue;
				relevanceList.add(new Triple<String, String, Double>(line.get(0), 
						line.get(1), Double.valueOf(line.get(2))));
			}
			
			Collections.sort(relevanceList, new Comparator<Triple<String, String, Double>>(){
				@Override
				public int compare(Triple<String, String, Double> o1, 
						Triple<String, String, Double> o2) {
					return o2.third.compareTo(o1.third);
				}
			});
		}
		
		String[] subarray = new String[args.length - 1];
		System.arraycopy(args, 1, subarray, 0, args.length - 1);
		KB kb = amie.data.U.loadFiles(subarray);
		
		Int2ObjectMap<Double> relevanceMap = new Int2ObjectOpenHashMap<>();
		for (Triple<String, String, Double> t : relevanceList.subList(0, 20000)) {
			if (t.third.isNaN()) {
				System.err.println(t.first + " is Nan");
				System.exit(1);
			}
			relevanceMap.put(KB.map(t.first), t.third);
		}
		
		// Now filter the facts
		int s = KB.map("?s");
		int r = KB.map("?r");
		int o = KB.map("?o");		
		List<int[]> query =  KB.triples(KB.triple(s, r, o));
		for (int relation : kb.selectDistinct(KB.map("?r"), query)) {			
			int[] query2 = KB.triple(s, relation, o);
			Int2ObjectMap<IntSet> bindings = null;
			boolean inversed = false;
			if (kb.isFunctional(relation) || relation == amie.data.Schema.typeRelationBS) {				
				bindings = kb.resultsTwoVariables(s, o, query2);
			} else {
				inversed = true;
				bindings = kb.resultsTwoVariables(o, s, query2);			
			}
			
			for (int argument : bindings.keySet()) {
				Double relevanceValue = relevanceMap.get(argument);
				if (relevanceValue != null) {
					//Output the entry
					outputEntry(argument, relation, bindings.get(argument), inversed);
				}
			}
		}
	}

	/**
	 * Output a set of triples when the relation and one of the arguments are 
	 * fixed.
	 * @param argument Either the subject or the object of the triples
	 * @param relation
	 * @param intHashMap
	 * @param inversed If true, then the object is fixed, otherwise the subject
	 */
	private static void outputEntry(int argument, int relation, 
			IntSet values, boolean inversed) {
		if (inversed) {
			for (int value : values) {
				System.out.println(value + "\t" + relation + "\t" + argument);
			}	
		} else {
			for (int value : values) {
				System.out.println(argument + "\t" + relation + "\t" + value);
			}				
		}
	}
}
