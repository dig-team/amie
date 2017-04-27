package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javatools.filehandlers.TSVFile;

/**
 * Simple class to join and project two tsv file on the rules
 * 
 * @author lgalarra
 *
 */
public class TSVRuleJoin {

	public static void main(String args[]) throws IOException{
		if(args.length < 2){
			System.out.println("TSVRuleJoin <file1> <file2> [col1,col2,...coln] [col1,col2,...colm] [outputfile]");			
			System.exit(1);
		}
				
		File file1 = new File(args[0]);
		File file2 = new File(args[1]);
		PrintStream pout = null;
		String[] proj1Columns = null;
		String[] proj2Columns = null;
		
		if(args.length > 2){
			if(!args[2].equals(""))
				proj1Columns = args[2].split(",");		
		}
		
		if(args.length > 3){
			if(!args[3].equals(""))
				proj2Columns = args[3].split(",");
		}
		
		if(args.length > 4){
			pout = new PrintStream(new File(args[4]));
		}else{
			pout = System.out;
		}
		
		TSVFile tsv1 = new TSVFile(file1);
		TSVFile tsv2 = new TSVFile(file2);
		//Map<Query, List<String>> recordsMap = new LinkedHashMap<Query, List<String>>();
		Map<String, List<String>> recordsMap = new LinkedHashMap<String, List<String>>();

		//Preprocess one of the files
		for(List<String> record1: tsv1){
			//Query q = AMIEreader.rule(record1.get(0));
			String q = record1.get(0);
			if(proj1Columns != null && proj1Columns.length > 0){
				List<String> projectedRecords = project(record1, proj1Columns);
				recordsMap.put(q, projectedRecords);
			}else{				
				recordsMap.put(q, record1.subList(1, record1.size()));
			}
		}
		
		for(List<String> record2: tsv2){			
			//Query q = AMIEreader.rule(record2.get(0));
			String q = record2.get(0);
			List<String> tail1 = recordsMap.get(q);
			if(tail1 != null){
				pout.print(q);
				for(String value: tail1){
					pout.print("\t" + value);
				}
				
				if(proj2Columns != null && proj2Columns.length > 0){
					List<String> tail2 = project(record2, proj2Columns);
					if(tail2 != null){
						for(String value: tail2){
							pout.print("\t" + value);
						}
					}
				}
				pout.println();
			}else{
//				System.err.println("No join for : " + q);				
			}
			
		}
		
		if(args.length > 4)
			pout.close();
		
		tsv1.close();
		tsv2.close();
	}

	private static List<String> project(List<String> record2, String[] cols) {		
		List<String> projectedRecords = new ArrayList<String>();
		for(String posStr: cols){
			int pos = Integer.parseInt(posStr);
			projectedRecords.add(record2.get(pos));
		}		
		return projectedRecords;
	}
}