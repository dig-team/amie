package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import javatools.datatypes.Triple;

/**
 * 
 * @author lgalarra
 *
 */
public class RuleBodySizeEvaluator {

	public static int aggregate(Int2ObjectMap<Int2IntMap> bindings){
		int count = 0;
		for(ByteString value1: bindings.keySet()){
			count += bindings.get(value1).size();
		}
		return count;
	}
	
	public static void main(String args[]) throws IOException{
		KB db = new KB();
		db.load(new File(args[1]));
		List<Rule> qList = AMIEParser.rules(new File(args[0]));
		Set<Triple<ByteString, ByteString, ByteString>> predictions = new HashSet<Triple<ByteString, ByteString, ByteString>>();	
		
		//Now calculate the body size for the rules
		for(Rule q: qList){
			ByteString[] head = q.getHead();
			q.setFunctionalVariablePosition(Rule.findFunctionalVariable(q, db));
			long[] result = conditionalBodySize(q, db, predictions);
			long nSubjects = db.countDistinct(ByteString.of(head[0]), KB.triples(head));
			long nObjects = db.countDistinct(ByteString.of(head[2]), KB.triples(head));
			ByteString tmp = head[0];
			head[0] = ByteString.of("?x");
			long a = db.countDistinctPairs(tmp, head[2], q.getTriples());
			head[0] = tmp;
			tmp = head[2];
			head[2] = ByteString.of("?x");
			long b = db.countDistinctPairs(head[0], tmp, q.getTriples());
			System.out.println(q.getRuleString() + "\t" + result[0] + "\t" + result[1] + "\t" + nSubjects + "\t" + nObjects + "\t" + a + "\t" + b);
		}
		
		
	}

	private static long[] conditionalBodySize(Rule q, KB db, Set<Triple<ByteString, ByteString, ByteString>> allPredictions) {
		Predictor pp = new Predictor(db);
		Object predictionsObj = pp.generatePredictions(q);
		Int2ObjectMap<Int2IntMap> predictions = (Int2ObjectMap<Int2IntMap>)predictionsObj;
		int countingVarPos = q.getFunctionalVariablePosition();
		long result[] = new long[2];
		
		for(ByteString value1: predictions.keySet()){
			for(ByteString value2: predictions.get(value1)){
				Triple<ByteString, ByteString, ByteString> triple = new Triple<ByteString, ByteString, ByteString>(null, null, null);
				
				if(value1.equals(value2)){ 
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
