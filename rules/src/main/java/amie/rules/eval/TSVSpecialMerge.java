package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javatools.filehandlers.TSVFile;

public class TSVSpecialMerge {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		TSVFile t1 = new TSVFile(new File(args[0]));
		TSVFile t2 = new TSVFile(new File(args[1]));
		PrintStream pout = null;

		if(args.length > 2){
			pout = new PrintStream(new File(args[2]));
		}else{
			pout = System.out;
		}
		
		List<List<String>> evals = new ArrayList<List<String>>();
		for(List<String> eval: t2){
			evals.add(eval);
		}
		
		for(List<String> prediction: t1){
			//Look for the prediction in the other file. If found take it from the second file
			List<String> evaluation = findEvaluation(evals, prediction);
			if(evaluation != null)
				outputRecord(pout, evaluation);
			else
				outputRecord(pout, prediction);
		}
		
		if(args.length > 2)
			pout.close();
		
		t1.close();
		t2.close();
		
	}
	
	private static List<String> findEvaluation(List<List<String>> f, List<String> prediction){
		for(List<String> eval: f){
			if(prediction.get(1).equals(eval.get(1)) && prediction.get(2).equals(eval.get(2)) 
					&& prediction.get(3).equals(eval.get(3))){
				return eval;
			}				
		}
		
		return null;
	}

	private static void outputRecord(PrintStream pout, List<String> record) {
		// TODO Auto-generated method stub
		pout.print(record.get(0));
		for(String field: record.subList(1, record.size())){
			pout.print("\t" + field);
		}
		
		pout.println();
	}

}
