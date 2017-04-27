package amie.rules.eval;

import javatools.datatypes.ByteString;
import javatools.datatypes.Triple;
import amie.rules.Rule;

public class Evaluation {

	public Rule rule;
	
	public Triple<ByteString, ByteString, ByteString> fact;
		
	public EvalResult result;
	
	public EvalSource source;
	
	public Evaluation(Rule rule, Triple<ByteString, ByteString, ByteString> fact, EvalResult result){
		this.rule = rule;
		this.fact = fact;
		this.result = result;
		source = EvalSource.Undefined;
	}
	
	public Evaluation(Rule rule, Triple<ByteString, ByteString, ByteString> fact, EvalResult result, EvalSource source){
		this.rule = rule;
		this.fact = fact;
		this.result = result;
		this.source = source;
	}

	public ByteString[] toTriplePattern() {
		return new ByteString[]{fact.first, fact.second, fact.third};
	}
}
