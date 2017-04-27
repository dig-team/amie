package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javatools.datatypes.ByteString;
import amie.data.KB;

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
		Set<ByteString> relationsInCommon = new LinkedHashSet<ByteString>();
		
		Set<ByteString> relationsDb1 = db1.selectDistinct(ByteString.of("?p"), 
				KB.triples(KB.triple(ByteString.of("?s"), 
						ByteString.of("?p"), ByteString.of("?o"))));
		if (db2 != null) {
			Set<ByteString> relationsDb2 = db2.selectDistinct(ByteString.of("?p"), 
					KB.triples(KB.triple(ByteString.of("?s"), 
							ByteString.of("?p"), ByteString.of("?o"))));
			
			for (ByteString relation : relationsDb1) {
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
