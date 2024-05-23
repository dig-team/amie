package amie.rules.eval;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;


import amie.data.javatools.datatypes.Pair;
import amie.data.javatools.filehandlers.TSVFile;
import amie.data.KB;
import amie.rules.Rule;

public class Evaluator {
	
	/**
	 * Given a triple predicted by a rule and the triple's position to be assumed as bounding variable,
	 * it evaluates the triple's correctness in the training and target datasets
	 * @param triple Prediction
	 * @param training The dataset from which the rule was mined
	 * @param target A newer version of the training dataset (superset)
	 * @param boundVarPos The variable position to be used when testing the prediction's precision
	 * @return 0, if the prediction is in the target dataset.
	 *         1, if it contradicts a functional constraint in the training dataset.
	 *         2, if it contradicts a functional constraint in the target dataset.
	 *         3, otherwise.
	 */
	public static int evaluate(int[] triple, KB training, KB target, int boundVarPos) {
		//If we know something else about the triple, PCA says it is false
		if (triple == null) {
			System.out.println("Triple is null");			
		}

		int returnVal = 3;
		int[] head = new int[3];
		head[0] = target.map("?s");
		head[1] = triple[1];
		head[2] = target.map("?o");

		boolean relationIsFunctional = 
				training.functionality(triple[1]) >= 0.9 
				|| training.inverseFunctionality(triple[1]) >= 0.84;

		if (target == null) {
			System.out.println("Target is null");
                        System.exit(2);
		}
		if(target.count(triple) > 0){
			//Bingo!
			returnVal = 0;
		}else{
			head[boundVarPos] = triple[boundVarPos];
			
			//Here apply PCA on the most functional variable
			if(training.count(head) > 0 && relationIsFunctional)
				returnVal = 1;
			else if(target.count(head) > 0 && relationIsFunctional)
				returnVal = 2;
			else
				returnVal = 3;
		}
		
		return returnVal;
	}
	
	/**
	 * Given a triple predicted by a rule, it evaluates its correctness in the training and target datasets
	 * @param triple Prediction
	 * @param training The dataset from which the rule was mined
	 * @param target A newer version of the training dataset (superset)
	 * @return 0, if the prediction is in the target dataset.
	 *         1, if it contradicts a functional constraint in the training dataset.
	 *         2, if it contradicts a functional constraint in the target dataset.
	 *         3, otherwise.
	 */
	public static int evaluate(int[] triple, KB training, KB target) {
		int boundVarPos =
				training.functionality(triple[1])
				> training.inverseFunctionality(triple[1]) ? 0 : 2;
		return evaluate(triple, training, target, boundVarPos);
	}
	
	public static void main(String args[]) throws Exception{
		if(args.length < 4){
			System.err.println("Evaluator <inputfile> <trainingDb> <targetDb> [outputManual] [outputAutomatic]");
			System.exit(1);
		}
		
		File inputFile = new File(args[0]);
		String lastRuleStr = "";
		Rule lastRule = null, currentRule = null;
		
		KB trainingDataset = new KB();
		KB targetDataset = new KB();		
		TSVFile tsvFile = new TSVFile(inputFile);
		
		trainingDataset.load(new File(args[1]));
		targetDataset.load(new File(args[2]));
		FileWriter manualEvalOut = new FileWriter(new File(args[3]));
		FileWriter automaticEvalOut = new FileWriter(new File(args[4]));
		PrintWriter manualEvalPw = new PrintWriter(manualEvalOut);
		PrintWriter automaticEvalPw = new PrintWriter(automaticEvalOut);
		int lineNumber = 0;
		for(List<String> record: tsvFile){
			lineNumber += 1;

			if(record.size() < 4){
				System.err.println("Warning: record at line " + lineNumber + " has wrong format, ignoring it");
				continue;
			}
			
			int[] triple = new int[3];
			
			String ruleStr = record.get(0);
			if(ruleStr.equals(lastRuleStr)){
				currentRule = lastRule;
			}else{
				Pair<List<int[]>, int[]> rulePair = targetDataset.rule(ruleStr);
				currentRule = new Rule(targetDataset);
				currentRule.getTriples().add(rulePair.second);
				currentRule.getTriples().addAll(rulePair.first);
			}
			
			for(int i = 0; i < 3; ++i){
				triple[i] = targetDataset.map(record.get(i + 1));
			}
			
			int evalCode = evaluate(triple, trainingDataset, targetDataset);
			if(evalCode == 3){
				//Output it in the manual evaluation file
				manualEvalPw.println(currentRule.getRuleString() + "\t" + targetDataset.unmap(triple[0]) + "\t" +
						targetDataset.unmap(triple[1]) + "\t" + targetDataset.unmap(triple[2]) + "\tManualEvaluation");
			}else{
				EvalResult evalResult = EvalResult.Unknown;
				EvalSource evalSource = EvalSource.Undefined;
				switch(evalCode){
				case 0:
					evalResult = EvalResult.True;
					evalSource = EvalSource.TargetSource;
					break;
				case 1:
					evalResult = EvalResult.False;
					evalSource = EvalSource.TrainingSource;
					break;
				case 2:
					evalResult = EvalResult.False;
					evalSource = EvalSource.TargetSource;
					break;
				}
				automaticEvalPw.println(currentRule.getRuleString() + "\t" + targetDataset.unmap(triple[0]) + "\t" +
						targetDataset.unmap(triple[1]) + "\t" + targetDataset.unmap(triple[2]) + "\t" + evalSource +
						"\t" + evalResult);
			}
			
			lastRule = currentRule;
			lastRuleStr = ruleStr;
		}
		
		tsvFile.close();
		manualEvalOut.close();
		automaticEvalOut.close();
		manualEvalPw.close();
		automaticEvalPw.close();
	}
}