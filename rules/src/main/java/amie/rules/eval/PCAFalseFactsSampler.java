package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Triple;
import amie.data.KB;
import amie.rules.AMIEParser;
import amie.rules.Rule;

/**
 * It computes a sample of the facts assumed false by the PCA for a given
 * set of rules mined from a KB.
 * 
 * @author galarrag
 *
 */
public class PCAFalseFactsSampler {

	private KB db;
	
	private int sampleSize;
	
	public PCAFalseFactsSampler(KB db, int sampleSize) {
		this.db = db;
		this.sampleSize = sampleSize;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		KB trainingSource = new KB();
		
		if(args.length < 3){
			System.err.println("PCAFalseFactsSampler <db> <samplesPerRule> <rules>");
			System.err.println("db\tAn RDF knowledge base");
			System.err.println("samplesPerRule\tSample size per rule. It defines the number of facts that will be randomly taken from the entire set of predictions made a each rule");
			System.err.println("rules\tFile containing each rule per line, as they are output by AMIE.");
			System.exit(1);
		}
		
		trainingSource.load(new File(args[0]));
		
		int sampleSize = Integer.parseInt(args[1]);
	
		List<Rule> rules = new ArrayList<Rule>();
		for(int i = 2; i < args.length; ++i){		
			rules.addAll(AMIEParser.rules(new File(args[i])));
		}
		
		for(Rule rule: rules){
			if(trainingSource.functionality(rule.getHead()[1]) >= trainingSource.inverseFunctionality(rule.getHead()[1]))
				rule.setFunctionalVariablePosition(0);
			else
				rule.setFunctionalVariablePosition(2);
		}
		
		PCAFalseFactsSampler pp = new PCAFalseFactsSampler(trainingSource, sampleSize);
		pp.run(rules);

	}

	private void run(Collection<Rule> rules){
		for(Rule rule: rules){	
			Collection<Triple<ByteString, ByteString, ByteString>> ruleAssumedFalse = generateAssumedFalseFacts(rule);			
			Collection<Triple<ByteString, ByteString, ByteString>> sample =  Predictor.sample(ruleAssumedFalse, sampleSize);
			for(Triple<ByteString, ByteString, ByteString> fact: sample){
				System.out.println(rule.getRuleString() + "\t" + fact.first + "\t" + fact.second + "\t" + fact.third);
			}
		}
	}
	
	/**
	 * It outputs a sample of the assumed-false facts
	 * @param rules
	 */
	private void runAndGroupByRelation(Collection<Rule> rules) {
		Map<ByteString, Collection<Rule>> headsToRules = new HashMap<ByteString, Collection<Rule>>();
		
		for(Rule rule: rules){
			Collection<Rule> rulesForRelation = headsToRules.get(rule.getHead()[1]);
			if(rulesForRelation == null){
				rulesForRelation = new ArrayList<Rule>();
				headsToRules.put(rule.getHead()[1], rulesForRelation);
			}
			rulesForRelation.add(rule);			
		}
		
		for(ByteString relation: headsToRules.keySet()){
			Map<Triple<ByteString, ByteString, ByteString>, Rule> factToRule = new HashMap<Triple<ByteString, ByteString, ByteString>, Rule>();
			Set<Triple<ByteString, ByteString, ByteString>> allAssumedFalse = new LinkedHashSet<Triple<ByteString, ByteString, ByteString>>();	
			for(Rule rule: headsToRules.get(relation)){		
				Collection<Triple<ByteString, ByteString, ByteString>> ruleAssumedFalse = generateAssumedFalseFacts(rule);
				for(Triple<ByteString, ByteString, ByteString> fact: ruleAssumedFalse){
					factToRule.put(fact, rule);
				}	
				allAssumedFalse.addAll(ruleAssumedFalse);				
			}
			Collection<Triple<ByteString, ByteString, ByteString>> sample =  Predictor.sample(allAssumedFalse, sampleSize);
			printSample(sample, factToRule);
		}
	}

	private void printSample(Collection<Triple<ByteString, ByteString, ByteString>> sample, Map<Triple<ByteString, ByteString, ByteString>, Rule> factToRule) {
		for(Triple<ByteString, ByteString, ByteString> fact: sample){
			System.out.println(factToRule.get(fact).getRuleString() + "\t" + fact.first + "\t" + fact.second + "\t" + fact.third);
		}
	}

	private Set<Triple<ByteString, ByteString, ByteString>> generateAssumedFalseFacts(Rule rule) {
		List<ByteString[]> query = new ArrayList<ByteString[]>();	
		Set<Triple<ByteString, ByteString, ByteString>> result = new LinkedHashSet<Triple<ByteString, ByteString, ByteString>>();
		ByteString[] head = rule.getHead();
		ByteString[] existential = head.clone();
		ByteString relation = head[1];
				
		if(rule.getFunctionalVariablePosition() == 0)
			existential[2] = ByteString.of("?x");
		else
			existential[0] = ByteString.of("?x");
		
		query.add(existential);
		for(ByteString[] triple: rule.getAntecedent())
			query.add(triple.clone());
		
		if(KB.numVariables(rule.getHead()) == 2){
			Map<ByteString, IntHashMap<ByteString>> bindingsTwoVars = db.difference(head[0], head[2], query, rule.getTriples());
			for(ByteString subject: bindingsTwoVars.keySet()){
				for(ByteString object: bindingsTwoVars.get(subject)){
					result.add(new Triple<ByteString, ByteString, ByteString>(subject, relation, object));
					//ByteString[] test = new ByteString[]{subject, relation, object};
					//assert(!db.contains(test));
					
				}
			}
			
		}
		
		return result;
	}

}
