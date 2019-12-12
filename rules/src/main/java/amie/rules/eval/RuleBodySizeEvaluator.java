package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import amie.data.KB;
import amie.data.tuple.IntTriple;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * 
 * @author lgalarra
 *
 */
public class RuleBodySizeEvaluator {

	public static int aggregate(Int2ObjectMap<Int2IntMap> bindings){
		int count = 0;
		for(int value1: bindings.keySet()){
			count += bindings.get(value1).size();
		}
		return count;
	}
	
	public static void main(String args[]) throws IOException{
		KB db = new KB();
		db.load(new File(args[1]));
		List<Rule> qList = AMIEParser.rules(new File(args[0]));
		Set<IntTriple> predictions = new HashSet<IntTriple>();	
		
		//Now calculate the body size for the rules
		for(Rule q: qList){
			int[] head = q.getHead();
			q.setFunctionalVariablePosition(Rule.findFunctionalVariable(q, db));
			long[] result = conditionalBodySize(q, db, predictions);
			long nSubjects = db.countDistinct(head[0], KB.triples(head));
			long nObjects = db.countDistinct(head[2], KB.triples(head));
			int tmp = head[0];
			head[0] = KB.map("?x");
			long a = db.countDistinctPairs(tmp, head[2], q.getTriples());
			head[0] = tmp;
			tmp = head[2];
			head[2] = KB.map("?x");
			long b = db.countDistinctPairs(head[0], tmp, q.getTriples());
			System.out.println(q.getRuleString() + "\t" + result[0] + "\t" + result[1] + "\t" + nSubjects + "\t" + nObjects + "\t" + a + "\t" + b);
		}
		
		
	}

	private static long[] conditionalBodySize(Rule q, KB db, Set<IntTriple> allPredictions) {
		Predictor pp = new Predictor(db);
		Object predictionsObj = pp.generatePredictions(q);
		Int2ObjectMap<Int2IntMap> predictions = (Int2ObjectMap<Int2IntMap>)predictionsObj;
		int countingVarPos = q.getFunctionalVariablePosition();
		long result[] = new long[2];
		
		for(int value1: predictions.keySet()){
			for(int value2: predictions.get(value1).keySet()){
				IntTriple triple = new IntTriple(0, 0, 0);
				
				if(value1 == value2){ 
					continue;
				}
				
				if(countingVarPos == 0){
					triple.first = value1;
					triple.third = value2;
				}else{
					triple.first = value2;
					triple.third = value1;					
				}
				
				triple.second = q.getHead()[1];
				if(!allPredictions.contains(triple)){
					++result[0];
					allPredictions.add(triple);
				}
				
				++result[1];
			}			
		}
		
		return result;
	}
}
