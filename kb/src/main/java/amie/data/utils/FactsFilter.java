package amie.data.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import amie.data.KB;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

/**
 * Given two sets of KBs: trim KBs and fact KBs, it outputs all the triples
 * of the fact KBs that contain entities occurring in the trim KBs.
 * @author galarrag
 *
 */
public class FactsFilter {

	public FactsFilter() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * The list of KBs to process. Start the path of trim KBs with the prefix :t
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		KB trimSource = null;
		KB factsSource = null;
		ArrayList<File> trimFiles = new ArrayList<File>();
		ArrayList<File> factsFiles = new ArrayList<File>();
		boolean includeObjects = false;
		int start = 0;
		
		if(args[0].equals("-withObjects")){
			includeObjects = true;
			start = 1;
		}
		
		for(int i = start; i < args.length; ++i){
			if(args[i].startsWith(":t")){
				trimFiles.add(new File(args[i].substring(2)));
			}else{
				factsFiles.add(new File(args[i]));
			}
		}
		
		if(trimFiles.isEmpty()){
			System.err.println("No files to trim");
			System.exit(1);
		}
		
		if(factsFiles.isEmpty()){
			System.err.println("No source files");
			System.exit(1);			
		}
		
		trimSource = new KB();
		factsSource = new KB();
		
		factsSource.load(factsFiles);
		trimSource.load(trimFiles);
		
		FileWriter fstream = new FileWriter("trimed.tsv");
		BufferedWriter out = new BufferedWriter(fstream);
		
		//Now iterate over the trim source and only output triples whose subject appears in the facts source
		ByteString[] triple = new ByteString[3];
		triple[0] = KB.compress("?s");
		triple[1] = KB.compress("?p");
		triple[2] = KB.compress("?o");		
		
		Set<ByteString> subjects =  factsSource.selectDistinct(triple[0], KB.triples(KB.triple(triple)));
		if(includeObjects)
			subjects.addAll(factsSource.selectDistinct(triple[2], KB.triples(KB.triple(triple))));
		
		for(ByteString subject: subjects){
			Map<ByteString, IntHashMap<ByteString>> subjectsMap = 
					trimSource.resultsTwoVariables(ByteString.of("?p"), ByteString.of("?o"),
							KB.triple(subject, ByteString.of("?p"), ByteString.of("?o")));
			if(subjectsMap == null) continue;			
			Set<ByteString> predicates = subjectsMap.keySet(); 
			for(ByteString predicate: predicates){
				IntHashMap<ByteString> objects = subjectsMap.get(predicate);
				for(ByteString object: objects){
					out.append(subject);
					out.append('\t');
					out.append(predicate);
					out.append('\t');
					out.append(object);
					out.append('\n');
				}
			}
		}
		
		out.close();
	}
}
