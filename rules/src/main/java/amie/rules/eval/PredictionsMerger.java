package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javatools.filehandlers.TSVFile;

public class PredictionsMerger {
	
	/**
	 * Given 2 files with evaluations, it merges them by grouping predictions made by the same rule.
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException{
		if(args.length < 2){
			System.err.println("PredictionsMerger <file1> <file2> [outputfile]");			
			System.exit(1);
		}
		
		File f1 = new File(args[0]);
		File f2 = new File(args[1]);
		PrintStream pout = null;
		
		TSVFile[] tsv = new TSVFile[2];
		tsv[0] = new TSVFile(f1);
		tsv[1] = new TSVFile(f2);
		
		if(args.length > 2){
			pout = new PrintStream(new File(args[2]));
		}else{
			pout = System.out;
		}
		
		Map<String, List<List<String>>> mergedResult = new HashMap<String, List<List<String>>>();
		
		for(int i = 0; i < 2; ++i){
			for(List<String> records: tsv[i]){
				String ruleStr = records.get(0);
				List<List<String>> remainingRecords = mergedResult.get(ruleStr);
				if(remainingRecords == null){
					remainingRecords = new ArrayList<List<String>>();
					mergedResult.put(ruleStr, remainingRecords);
				}
				
				remainingRecords.add(records.subList(1, records.size()));
			}
		}
		
		//Now output the merged results
		for(String ruleStr: mergedResult.keySet()){
			List<List<String>> evaluations = mergedResult.get(ruleStr);
			for(List<String> evaluation: evaluations){
				pout.print(ruleStr);
				for(String field: evaluation){
					pout.print("\t" + field);
				}
				pout.println();
			}
		}
		
		tsv[0].close();
		tsv[1].close();
		if(args.length > 2)
			pout.close();
	}

}
