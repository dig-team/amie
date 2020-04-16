package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


import amie.data.KB;
import amie.data.tuple.IntTriple;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;

/**
 * This class defines objects that take rules extracted from a training 
 * KB and produce a sample of the predictions made by the rules, i.e., triples 
 * that are not in the training source.
 * 
 * @author lgalarra
 *
 */
public class Predictor {
	/**
	 * Sample size
	 */
	private int sampleSize;
	
	/**
	 * Input dataset
	 */
	private KB source;
	
	public Predictor(KB dataset) {
		super();
		this.source = dataset;
		sampleSize = 30;
	}
	
	public Predictor(KB dataset, int sampleSize) {
		super();
		this.source = dataset;
		this.sampleSize = sampleSize;
	}
	
	/**
	 * @return the sampleSize
	 */
	public int getSampleSize() {
		return sampleSize;
	}

	/**
	 * @param sampleSize the sampleSize to set
	 */
	public void setNumberOfPredictions(int numberOfPredictions) {
		this.sampleSize = numberOfPredictions;
	}
		
	
	public Set<IntTriple> generateBodyTriples(Rule rule, boolean PCAMode) {
		Object bindings = null;
		if (PCAMode) {
			bindings = generateBodyPCABindings(rule);
		} else {
			bindings = generateBodyBindings(rule);
		}
		
		Set<IntTriple> triples = new LinkedHashSet<>();
		int[] head = rule.getHead();
		int relation = rule.getHead()[1];
		
		if (KB.numVariables(rule.getHead()) == 1) {
			IntSet constants = (IntSet) bindings;
			int variablePosition = rule.getFunctionalVariablePosition();
			for (int constant : constants) {
				if (variablePosition == 0) {
					triples.add(new IntTriple(constant, relation, head[2]));
				} else {
					triples.add(new IntTriple(head[0], relation, constant));					
				}
			}
		} else {
			Int2ObjectMap<Int2IntMap> pairs = (Int2ObjectMap<Int2IntMap>) bindings; 
			int functionalPosition = rule.getFunctionalVariablePosition();
			for (int subject : pairs.keySet()) {
				for (int object : pairs.get(subject).keySet()) {
					if (functionalPosition == 0) {
						triples.add(new IntTriple(subject, relation, object));
					} else {
						triples.add(new IntTriple(object, relation, subject));						
					}
				}
			}
		}
		
		return triples;
	}
	
	/**
	 * Given a rule, it produces a sample from the body bindings of the rule.
	 * 
	 * @param rule
	 * @return
	 */
	public Object generateBodyBindings(Rule rule){
		if(KB.numVariables(rule.getHead()) == 1)
			return generateBindingsForSingleVariable(rule);
		else if (KB.numVariables(rule.getHead()) == 2)
			return generateBindingsForTwoVariables(rule);
		else 
			return generateBindingsForThreeVariables(rule);
	}
	
	private Object generateBindingsForThreeVariables(Rule rule) {
		int[] head = rule.getHead();
		return source.selectDistinct(rule.getFunctionalVariable(), 
				head[1], rule.getNonFunctionalVariable(), rule.getAntecedent());
	}

	private Object generateBindingsForTwoVariables(Rule rule) {
		return source.selectDistinct(rule.getFunctionalVariable(), 
				rule.getNonFunctionalVariable(), rule.getAntecedent());
	}

	private Object generateBindingsForSingleVariable(Rule rule) {
		return source.selectDistinct(rule.getFunctionalVariable(), rule.getAntecedent());
	}
	
	/**
	 * Given a rule, it produces a sample from the body* bindings
	 * of the rule (the bindings that match the denominator of the
	 * PCA confidence expression)
	 * @param rule
	 * @return
	 */
	public Object generateBodyPCABindings(Rule rule) {
		if(KB.numVariables(rule.getHead()) == 1)
			return generatePCABindingsForSingleVariable(rule);
		else
			return generatePCABindingsForTwoVariables(rule);		
		
	}
	
	private Object generatePCABindingsForSingleVariable(Rule rule) {
		return source.selectDistinct(rule.getFunctionalVariable(), rule.getPCAQuery());
	}

	private Object generatePCABindingsForTwoVariables(Rule rule) {
		// TODO Auto-generated method stub
		return source.selectDistinct(rule.getFunctionalVariable(),
				rule.getNonFunctionalVariable(), rule.getPCAQuery());
	}

	/**
	 * Given a rule, it produces sample of predictions (triples that are beyond the database)
	 * 
	 * @param rule
	 * @return
	 */
	public Object generatePredictions(Rule rule){		
		if(KB.numVariables(rule.getHead()) == 1)
			return predictBindingsForSingleVariable(rule);
		else
			return predictBindingsForTwoVariables(rule);
	}
	
	private IntSet predictBindingsForSingleVariable(Rule rule) {
		//First get the bindings for the projection variable in the antecedent
		return source.difference(rule.getFunctionalVariable(), rule.getAntecedent(), rule.getTriples());
	}
	
	private Int2ObjectMap<IntSet> predictBindingsForTwoVariables(Rule rule) {
		return source.difference(rule.getFunctionalVariable(), 
				rule.getNonFunctionalVariable(), rule.getAntecedent(), rule.getTriples());
	}
	
	/**
	 * It takes a sample of unique predictions for the given rules by taking care of duplicated predictions.
	 * If a prediction was generated before by a rule, then it is not considered in the sampling.
	 * 
	 * @param rules
	 */
	public void runMode1(Collection<Rule> rules) {
		Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions = 
				new Int2ObjectOpenHashMap<Int2ObjectMap<IntSet>>();
		
		for(Rule rule: rules){
			Object predictions = generatePredictions(rule);
			Collection<IntTriple> newPredictions = 
					samplePredictions(predictions, rule, allPredictions);
			printPredictions(rule, newPredictions);
		}
	}		
		
	/**
	 * It takes a sample of the predictions of every rule in the argument. Unlike the method runMode1, the result can contain duplicates
	 * if two rules predict the same fact and this fact is considered in both samples.
	 * 
	 * @param rules
	 */
	public void runMode2(Collection<Rule> rules){
		for(Rule rule: rules){
			Object predictions = generatePredictions(rule);
			Collection<IntTriple> newPredictions = 
					samplePredictions(predictions, rule);
			printPredictions(rule, newPredictions);
		}		
	}

	private Collection<IntTriple> samplePredictions(Object predictions, Rule rule) {
		// TODO Auto-generated method stub
		int nVars = KB.numVariables(rule.getHead());
		if(nVars == 2){
			return samplePredictionsTwoVariables((Int2ObjectMap<IntSet>)predictions, rule);
		}else if(nVars == 1){
			return samplePredictionsOneVariable((IntSet)predictions, rule);			
		}
		
		return null;
	}

	private Collection<IntTriple> samplePredictionsOneVariable(IntSet predictions, Rule rule) {
		// TODO Auto-generated method stub
		return null;
	}

	private Collection<IntTriple> 
	samplePredictionsTwoVariables(Int2ObjectMap<IntSet> predictions, Rule rule) {
		IntSet keySet = predictions.keySet();
		int relation = rule.getHead()[1];
		//Depending on the counting variable the order is different
		int countingVarPos = rule.getFunctionalVariablePosition();
		Set<IntTriple> samplingCandidates = 
				new HashSet<IntTriple>();
		
		for(int value1: keySet){
			for(int value2: predictions.get(value1)){
				IntTriple triple = 
						new IntTriple(0, 0, 0);
				
				if(value1 == value2) continue;
				
				if(countingVarPos == 0){
					triple.first = value1;
					triple.third = value2;
				}else{
					triple.first = value2;
					triple.third = value1;					
				}
				
				triple.second = relation;
				samplingCandidates.add(triple);
			}			
		}
		
		return Predictor.sample(samplingCandidates, this.sampleSize);
	}
	

	private void printPredictions(Rule rule, Collection<IntTriple> newPredictions) {
		for(IntTriple triple: newPredictions){
			System.out.println(rule.getRuleString() + "\t" + KB.unmap(triple.first) + "\t" + KB.unmap(triple.second) + "\t" + KB.unmap(triple.third));
		}
	}

	/**
	 * 
	 * @param predictions
	 * @param rule
	 */
	private Collection<IntTriple> samplePredictions(
			Object predictions, Rule rule, Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions) {
		// TODO Auto-generated method stub
		int nVars = KB.numVariables(rule.getHead());
		if(nVars == 2){
			return samplePredictionsTwoVariables((Int2ObjectMap<IntSet>)predictions, rule, allPredictions);
		}else if(nVars == 1){
			return samplePredictionsOneVariable((IntSet)predictions, rule, allPredictions);			
		}
		
		return null;
	}

	private Collection<IntTriple> samplePredictionsOneVariable(IntSet predictions,
			Rule rule,
			Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions) {
		int[] head = rule.getHead();
		//Depending on the counting variable the order is different
		int countingVarPos = rule.getFunctionalVariablePosition();
		Set<IntTriple> samplingCandidates =
				new LinkedHashSet<IntTriple>();

		for(int binding: predictions){
			IntTriple triple =
					new IntTriple(head[0], head[1], head[2]);
			if (countingVarPos == 0) {
				triple.first = binding;
			} else {
				triple.third = binding;
			}

			if(!containsPrediction(allPredictions, triple)){
				samplingCandidates.add(triple);
			}

			addPrediction(allPredictions, triple);
		}

		return Predictor.sample(samplingCandidates, sampleSize);
	}

	private Collection<IntTriple> samplePredictionsTwoVariables(
			Int2ObjectMap<IntSet> predictions,
			Rule rule, Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions){
		IntSet keySet = predictions.keySet();
		int relation = rule.getHead()[1];
		//Depending on the counting variable the order is different
		int countingVarPos = rule.getFunctionalVariablePosition();
		Set<IntTriple> samplingCandidates = 
				new LinkedHashSet<IntTriple>();
		
		for(int value1: keySet){
			for(int value2: predictions.get(value1)){
				IntTriple triple = 
						new IntTriple(0, 0, 0);
				
				if(value1 == value2) continue;
				
				if(countingVarPos == 0){
					triple.first = value1;
					triple.third = value2;
				}else{
					triple.first = value2;
					triple.third = value1;					
				}
				
				triple.second = relation;
				
				if(!containsPrediction(allPredictions, triple)){
					samplingCandidates.add(triple);
				}
				
				addPrediction(allPredictions, triple);
			}			
		}
		
		return Predictor.sample(samplingCandidates, sampleSize);
	}
	
	/**
	 * Adds a prediction to the index of already generated predictions. It does not check whether the triple
	 * already exists in the set.
	 * @param allPredictions
	 * @param triple
	 */
	private void addPrediction(Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions, 
			IntTriple triple) {
		if(allPredictions.containsKey(triple.second)){
			Int2ObjectMap<IntSet> subjects = allPredictions.get(triple.second);
			if(subjects.containsKey(triple.first)){
				subjects.get(triple.first).add(triple.third);
			}else{
				IntSet objects = new IntOpenHashSet();
				objects.add(triple.third);
				subjects.put(triple.first, objects);
			}
		}else{
			IntSet objects = new IntOpenHashSet();
			objects.add(triple.third);
			Int2ObjectMap<IntSet> subjects = new Int2ObjectOpenHashMap<IntSet>();
			subjects.put(triple.first, objects);
			allPredictions.put(triple.second, subjects);
		}
	}

	private boolean containsPrediction(Int2ObjectMap<Int2ObjectMap<IntSet>> allPredictions, 
			IntTriple triple) {
		// TODO Auto-generated method stub
		Int2ObjectMap<IntSet> subjects2objects = allPredictions.get(triple.second);
		if(subjects2objects != null){
			IntSet objects = subjects2objects.get(triple.first);
			if(objects != null){
				return objects.contains(triple.third);
			}
			return false;
		}
		
		return false;
	}

	/**
	 * Given a collection of predictions, it extracts a random sample from it.
	 * 
	 * @param samplingCandidates
	 * @return
	 */
	public static Collection<IntTriple> sample(Collection<IntTriple> samplingCandidates,
			int sampleSize){
		//Now sample them
		List<IntTriple> result = new ArrayList<>(sampleSize);		
		if(samplingCandidates.size() <= sampleSize){
			return samplingCandidates;
		}else{
			Object[] candidates = samplingCandidates.toArray();
			int i;
			Random r = new Random();
			for(i = 0; i < sampleSize; ++i){				
				result.add((IntTriple)candidates[i]);
			}
			
			while(i < candidates.length){
			    int rand = r.nextInt(i);
			    if(rand < sampleSize){
			    	//Pick a random number in the reserviour
			    	result.set(r.nextInt(sampleSize), (IntTriple)candidates[i]);
			    }
			    ++i;
			}
		}
		
		return result;
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		KB trainingSource = new KB();
		
		if(args.length < 4){
			System.err.println("PredictionsSampler <db> <samplesPerRule> <unique> <rules>");
			System.err.println("db\tAn RDF knowledge base");
			System.err.println("samplesPerRule\tSample size per rule. It defines the number of facts that will be randomly taken from the entire set of predictions made a each rule");
			System.err.println("unique (0|1)\tIf 1, predictions that were generated by other rules before, are not output");
			System.err.println("rules\tFile containing each rule per line, as they are output by AMIE.");
			System.exit(1);
		}
		
		trainingSource.load(new File(args[0]));
		
		int sampleSize = Integer.parseInt(args[1]);
		int mode = Integer.parseInt(args[2]);
	
		List<Rule> rules = new ArrayList<Rule>();
		for(int i = 3; i < args.length; ++i){		
			rules.addAll(AMIEParser.rules(new File(args[i])));
		}
		
		for(Rule rule: rules){
			if(trainingSource.functionality(rule.getHead()[1]) >= trainingSource.inverseFunctionality(rule.getHead()[1]))
				rule.setFunctionalVariablePosition(0);
			else
				rule.setFunctionalVariablePosition(2);
		}
		
		Predictor pp = new Predictor(trainingSource, sampleSize);
		if(mode == 1)
			pp.runMode1(rules); //Considered previous samples
		else
			pp.runMode2(rules); //Independent for each rule
	}



}