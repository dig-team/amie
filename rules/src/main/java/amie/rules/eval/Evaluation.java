package amie.rules.eval;


import amie.data.tuple.IntTriple;
import amie.rules.Rule;

public class Evaluation {

	public Rule rule;
	
	public IntTriple fact;
		
	public EvalResult result;
	
	public EvalSource source;
	
	public Evaluation(Rule rule, IntTriple fact, EvalResult result){
		this.rule = rule;
		this.fact = fact;
		this.result = result;
		source = EvalSource.Undefined;
	}
	
	public Evaluation(Rule rule, IntTriple fact, EvalResult result, EvalSource source){
		this.rule = rule;
		this.fact = fact;
		this.result = result;
		this.source = source;
	}

	public int[] toTriplePattern() {
		return new int[]{fact.first, fact.second, fact.third};
	}
}
