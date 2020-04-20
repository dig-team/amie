package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;


import javatools.datatypes.IntHashMap;
import javatools.filehandlers.TSVFile;
import amie.data.KB;
import amie.data.tuple.IntTriple;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;


class TripleComparator implements Comparator<int[]> {

	@Override
	public int compare(int[] o1, int[] o2) {
		if (o1[0] == o2[0]) {
			if (o1[1] == o2[1]) {
				return Integer.compare(o1[2], o2[2]); 
			} else {
				return Integer.compare(o1[1], o2[1]);
			}
		} else {
			return Integer.compare(o1[0], o2[0]);
		}
	}
}
/**
 * This class implements a simple program that given a set of rules extracted from an old version of a KB, 
 * counts the number of right predictions (hits) in the newer version of the KB.
 * @author lgalarra
 *
 */
public class RuleHitsEvaluator {
	
	public static void main(String args[]) throws IOException{
		if(args.length < 3){
			System.err.println("RuleHitsEvaluator <inputfile> <trainingDb> <targetDb>");
			System.exit(1);
		}
		
		File inputFile = new File(args[0]);		
		KB trainingDataset = new KB();
		KB targetDataset = new KB();		
		TSVFile tsvFile = new TSVFile(inputFile);
		int rawHitsInTargetNotInTraining = 0;
		int hitsInTarget = 0;
		int rawHitsInTarget = 0;
		int rawHitsInTraining = 0;
		int hitsInTraining = 0;
		int hitsInTargetNotInTraining = 0;
		
		trainingDataset.load(new File(args[1]));
		targetDataset.load(new File(args[2]));
		Predictor predictor = new Predictor(trainingDataset);	
		IntHashMap<IntTriple> predictions = 
				new IntHashMap<>();
		// Collect all predictions made by the rules.
		for(List<String> record: tsvFile) {
			Rule q = AMIEParser.rule(record.get(0));
			if(q == null) {
				continue;
			}
			
			int[] head = q.getHead();
			q.setFunctionalVariablePosition(Rule.findFunctionalVariable(q, trainingDataset));
			Object bindings = null;
			try {
				bindings = predictor.generateBodyBindings(q);
			} catch (Exception e) {
				continue;
			}
			
			if(KB.numVariables(head) == 1){
				IntSet oneVarBindings = (IntSet)bindings;
				for(int binding: oneVarBindings){
					IntTriple t = 
							new IntTriple(head[0], head[1], head[2]);
					if (q.getFunctionalVariablePosition() == 0) {
						t.first = binding;
					} else {
						t.third = binding;
					}
					predictions.increase(t);
				}
			}else{
				Int2ObjectMap<IntSet> twoVarsBindings =
						(Int2ObjectMap<IntSet>)bindings;
				for(int value1: twoVarsBindings.keySet()){
					for(int value2: twoVarsBindings.get(value1)){
						IntTriple t = 
								new IntTriple(KB.map("?a"), head[1], KB.map("?b"));
						if(q.getFunctionalVariablePosition() == 0){
							t.first = value1;
							t.third = value2;
						}else{
							t.first = value2;
							t.third = value1;					
						}
						predictions.increase(t);
					}
				}
			}		
		}
		
		for (IntTriple t : predictions) {
			int[] triple = KB.triple2Array(t);
			int eval = Evaluator.evaluate(triple, trainingDataset, targetDataset);
			if(eval == 0) { 
				++hitsInTarget;
				rawHitsInTarget += predictions.get(t);
			}
			
			if(trainingDataset.count(triple) > 0) {
				++hitsInTraining;
				rawHitsInTraining += predictions.get(t);
			} else {
				if (eval == 0) {
					++hitsInTargetNotInTraining;
					rawHitsInTargetNotInTraining += predictions.get(t);
				}
			}
		}
		
		System.out.println("Total unique predictions\tTotal Hits in target"
				+ "\tTotal unique hits in target\tTotal hits on training"
				+ "\tTotal unique hits in training\tTotal hits in target not in training"
				+ "\tTotal unique hits in target not in training");
		System.out.println(predictions.size() + 
				"\t" + rawHitsInTarget + "\t" + hitsInTarget + 
				"\t" + rawHitsInTraining + "\t" + hitsInTraining + 
				"\t" + rawHitsInTargetNotInTraining + "\t" + hitsInTargetNotInTraining);
		tsvFile.close();
	}
}
