package amie.data.utils;

import java.io.File;
import java.io.IOException;


import amie.data.KB;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Summarize 2 KBs and print their common relations. It also accepts
 * a single KB. In that case, the program just prints the info of a one KB 
 * 
 * @author galarrag
 *
 */
public class KBsSummarizer {
	
	public static boolean ShowDistribution = false;
	
	public static boolean showTypesStatistics = false;
	
	public static void main(String args[]) throws IOException {
		KB db1 = new KB();
		db1.load(new File(args[0]));
		KB db2 = null;
		if (args.length > 1) {
			db2 = new KB();
			db2.load(new File(args[1]));	
		}		
		IntSet relationsInCommon = new IntOpenHashSet();
		
		IntSet relationsDb1 = db1.selectDistinct(KB.map("?p"), 
				KB.triples(KB.triple(KB.map("?s"), 
						KB.map("?p"), KB.map("?o"))));
		if (db2 != null) {
			IntSet relationsDb2 = db2.selectDistinct(KB.map("?p"), 
					KB.triples(KB.triple(KB.map("?s"), 
							KB.map("?p"), KB.map("?o"))));
			
			for (int relation : relationsDb1) {
				if (relationsDb2.contains(relation)) {
					relationsInCommon.add(relation);
				}
			}
		}
		
		if (ShowDistribution) {
			db1.summarizeDistributions(true);
		} else {
			db1.summarize(true);
		}
		
		if (showTypesStatistics) {
			db1.summarizeTypes();
		}
		
		if (db2 != null) {
			if (showTypesStatistics) {
				db2.summarizeTypes();
			}
			
			System.out.println();
			if (ShowDistribution) {
				db2.summarizeDistributions(true);
			} else {
				db2.summarize(true);	
			}
			System.out.println(relationsInCommon);
		}
	}
}
