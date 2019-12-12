package amie.data.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import amie.data.KB;
import amie.data.tuple.IntPair;
import amie.data.tuple.IntTriple;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Given a KB with non-canonicalized verbal phrases and types for the entities, 
 * it defines new relations as the combination of a verbal phrase and a signature (domain
 * and range types) and outputs information about the domain and range overlaps of the
 * identified relations.
 * 
 * @author galarrag
 *
 */
public class OutputSignedRelationPhrases {

	public OutputSignedRelationPhrases() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		KB db = new KB();
		List<File> files = new ArrayList<File>();
		for (int i = 0; i < args.length; ++i) {
			files.add(new File(args[i]));
		}
		db.load(files);
		Map<IntTriple, Set<IntPair>> signedRelations = 
				buildSignedRelations(db);
		List<IntTriple> signatures = 
				new ArrayList<>(signedRelations.keySet());
		FileWriter out = new FileWriter(new File("relations.txt"));
		PrintWriter pWriter = new PrintWriter (out);
		for (int i = 0; i < signatures.size(); ++i) {
			pWriter.println(signatures.get(i).toString());
			Set<IntPair> seti = signedRelations.get(signatures.get(i));
			if (seti.size() < 3) {
				continue;
			}
			IntSet subjectsI = projectPairSet(seti);
			for (int j = i + 1; j < signatures.size(); ++j) {
				Set<IntPair> setj = signedRelations.get(signatures.get(j));				
				if (setj.size() < 3) {
					continue;
				}
				int intsr = intersectionSize(seti, setj);
				IntSet subjectsJ = projectPairSet(setj);
				int pcaCount1 = pcaIntersection(seti, subjectsJ); // ri => rj
				int pcaCount2 = pcaIntersection(setj, subjectsI); // rj => ri
				if (intsr > 1) {
					System.out.println(signatures.get(i) + "\t" + signatures.get(j) 
							+ "\t" + intsr + "\t" + seti.size() + "\t" + setj.size() + "\t" 
							+ pcaCount1 + "\t" + pcaCount2 + "\t" + ((double)intsr / pcaCount1) 
							+ "\t" + ((double)intsr / pcaCount2));
				}
			}
		}
		out.close();
	}

	private static int pcaIntersection(
			Set<IntPair> seti,
			IntSet subjectsJ) {
		int count = 0;
		for (IntPair pair : seti) {
			if (subjectsJ.contains(pair.first)) {
				++count;
			}
		}
		
		return count;
	}

	private static IntSet projectPairSet(
			Set<IntPair> seti) {
		IntSet result = new IntOpenHashSet();
		for (IntPair pair : seti) {
			result.add(pair.first);
		}
		return result;
	}

	private static int intersectionSize(
			Set<IntPair> set,
			Set<IntPair> set2) {
		int count = 0;
		Set<IntPair> smaller = set.size() < set2.size() ? set : set2;
		Set<IntPair> bigger = smaller == set ? set2 : set;
		for (IntPair pair : smaller) {
			if (bigger.contains(pair)) {
				++count;
			}
		}
		
		return count;
	}

	private static Map<IntTriple, 
	Set<IntPair>> buildSignedRelations(KB db) {
		// TODO Auto-generated method stub
		Map<IntTriple, Set<IntPair>> result 
		= new HashMap<IntTriple, Set<IntPair>>();
		int typeRelation = KB.map("<rdf:type>");
		int defaultStr = KB.map("default");
		Int2ObjectMap<Int2ObjectMap<IntSet>> map =
				db.resultsThreeVariables(KB.map("?p"), KB.map("?s"), KB.map("o"), 
						KB.triple("?s", "?p", "?o"));
		for (int relation : map.keySet()) {
			if (relation != typeRelation) {
				Int2ObjectMap<IntSet> tail = map.get(relation);
				for (int subject : tail.keySet()) {
					for (int object : tail.get(subject)) {
						// Get the types
						IntSet subjectTypes = 
								map.get(typeRelation).get(subject);
						IntSet objectTypes = 
								map.get(typeRelation).get(object);
						if (subjectTypes == null) {
							subjectTypes = new IntOpenHashSet();
							subjectTypes.add(defaultStr);
						}
						
						if (objectTypes == null) {
							objectTypes = new IntOpenHashSet();
							objectTypes.add(defaultStr);
						}
						
						for (int domain : subjectTypes) {
							for (int range : objectTypes) {
								IntTriple triple = 
										new IntTriple(
												relation, domain, range);
								Set<IntPair> pairs = result.get(triple);
								if (pairs == null) {
									pairs = new LinkedHashSet<IntPair>();
									result.put(triple, pairs);
								}
								pairs.add(new IntPair(subject, object));
							}
						}
					}
				}
			}
		}
		
		return result;
	}

}
