package amie.data.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import amie.data.KB;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

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
		IntSet seeds = new IntOpenHashSet();
		//reading file line by line
		String line = bufferedReader.readLine().trim();
		while(line != null){
			seeds.add(KB.map("<" + line + ">"));
			line = bufferedReader.readLine();
		}
				
		Int2ObjectMap<Int2ObjectMap<IntSet>> factSourcesMap = 
				factsSource.resultsThreeVariables(KB.map("?s"), KB.map("?p"), KB.map("o"), 
						KB.triple("?s", "?p", "?o"));
		IntSet subjects = factSourcesMap.keySet();
		for(int subject: subjects){
			if(seeds.contains(subject)){
				//Then produce the facts
				Int2ObjectMap<IntSet> subjectsMap = factSourcesMap.get(subject);
				if(subjectsMap == null) continue;
				
				IntSet predicates = subjectsMap.keySet(); 
				for(int predicate: predicates){
					IntSet objects = subjectsMap.get(predicate);
					for(int object: objects){
						int nTimes = 1;
						for(int k = 0; k < nTimes; ++k){
							out.append(KB.unmap(subject));
							out.append('\t');
							out.append(KB.unmap(predicate));
							out.append('\t');
							out.append(KB.unmap(object));
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
