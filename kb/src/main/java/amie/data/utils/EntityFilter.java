package amie.data.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import amie.data.KB;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

/**
 * Given two sets of KBs: entity KBs and fact KBs, it outputs all the triples
 * of the fact KBs that occur in the entity KBs.
 * @author galarrag
 *
 */
public class EntityFilter {

	public EntityFilter() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * The list of KBs to process. Start the path of entity KBs with the prefix :e
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		KB factsSource = null;
		ArrayList<File> factsFiles = new ArrayList<File>();
		File entities = null;
				
		for(int i = 0; i < args.length; ++i){
			if(args[i].startsWith(":e")){
				entities = new File(args[i].substring(2));
			}else{
				factsFiles.add(new File(args[i]));
			}
		}
		
		if(entities == null){
			System.err.println("No entities input file");
			System.exit(1);
		}
		
		if(factsFiles.isEmpty()){
			System.err.println("No input data files");
			System.exit(1);			
		}

		factsSource = new KB();
		factsSource.load(factsFiles);
		
		FileWriter fstream = new FileWriter("output.tsv");
		BufferedWriter out = new BufferedWriter(fstream);
		
		FileReader fileReader = new FileReader(entities);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		Set<ByteString> seeds = new HashSet<ByteString>();
		//reading file line by line
		String line = bufferedReader.readLine().trim();
		while(line != null){
			seeds.add(ByteString.of("<" + line + ">"));
			line = bufferedReader.readLine();
		}
				
		Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> factSourcesMap = 
				factsSource.resultsThreeVariables(ByteString.of("?s"), ByteString.of("?p"), ByteString.of("o"), 
						KB.triple("?s", "?p", "?o"));
		Set<ByteString> subjects = factSourcesMap.keySet();
		for(ByteString subject: subjects){
			if(seeds.contains(subject)){
				//Then produce the facts
				Map<ByteString, IntHashMap<ByteString>> subjectsMap = factSourcesMap.get(subject);
				if(subjectsMap == null) continue;
				
				Set<ByteString> predicates = subjectsMap.keySet(); 
				for(ByteString predicate: predicates){
					IntHashMap<ByteString> objects = subjectsMap.get(predicate);
					for(ByteString object: objects){
						int nTimes = objects.get(object);
						for(int k = 0; k < nTimes; ++k){
							out.append(subject);
							out.append('\t');
							out.append(predicate);
							out.append('\t');
							out.append(object);
							out.append('\n');
						}
					}
				}
			}
		}	
		
		fileReader.close();
		bufferedReader.close();
		out.close();
	}
}
