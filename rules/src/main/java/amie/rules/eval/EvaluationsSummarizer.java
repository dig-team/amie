package amie.rules.eval;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javatools.filehandlers.TSVFile;

/**
 * 
 * @author lgalarra
 *
 */
public class EvaluationsSummarizer {
	
	public static void main(String args[]) throws IOException{
		if(args.length < 1){
			System.err.println("EvaluationsSummarizer <inputfile> [outputfile]");
			System.exit(1);
		}
		
		File inputFile = new File(args[0]);
		File outFile = new File(args[1]);
		if(!outFile.exists())
			outFile.createNewFile();
		
		FileWriter outputFile = new FileWriter(outFile);
		EvaluationsSummarizer summarizer = new EvaluationsSummarizer();
		Map<String, int[]> evalData = summarizer.parse(inputFile);
		summarizer.output(outputFile, evalData);
	}

	private void output(FileWriter outputFile, Map<String, int[]> evalData) {
		// TODO Auto-generated method stub
		PrintWriter pw = new PrintWriter(outputFile);
		for(String rule: evalData.keySet()){
			int summary[] = evalData.get(rule);
			pw.print(rule);
			pw.print("\t");
			pw.print(summary[0]);
			pw.print("\t");
			pw.print(summary[1]);
			pw.print("\t");
			pw.println(summary[2]);			
		}
		
		pw.close();
	}

	private Map<String, int[]> parse(File inputFile) throws IOException {
		TSVFile tsv = new TSVFile(inputFile);
		Map<String, int[]> result = new LinkedHashMap<String, int[]>();		
		String lastRule = null;
		
		for(List<String> record: tsv){
			if(record.size() < 6) continue;
			
			String currentRule = record.get(0);
			if(lastRule == null || !currentRule.equals(lastRule)){
				if(result.get(currentRule) != null)
					System.err.println("Overriding " + currentRule);
				result.put(currentRule, new int[]{0, 0, 0});
			}
			int[] summary = result.get(currentRule);
			//Check its evaluation
			switch(record.get(5)){
			case "True":
				++summary[0];
				break;
			case "False":
				++summary[1];
				break;
			case "Unknown":
				++summary[2];
				break;
			}
			
			lastRule = currentRule;
		}
		
		tsv.close();
		
		return result;
	}
}