package amie.rosa;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javatools.datatypes.ByteString;
import amie.rules.AMIEParser;
import amie.rules.Rule;

public class TwoHopsSubsumptionFilter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		List<Rule> rules = AMIEParser.rules(new File(args[0]));
		
		for(Rule rule: rules){
			if(followsTwoHopsTranslation(rule))
				System.out.println(rule.getRuleString());
		}
	}

	private static boolean followsTwoHopsTranslation(Rule rule) {
		if(rule.getLength() != 3)
			return false;
		
		List<ByteString[]> body = rule.getBody();
		ByteString[] head = rule.getHead();
		
		if((body.get(0)[1].toString().trim().startsWith("<ns:") && body.get(1)[1].toString().trim().startsWith("<ns:") && !head[1].toString().trim().startsWith("<ns:"))
				|| 	(!body.get(0)[1].toString().trim().startsWith("<ns:") && !body.get(1)[1].toString().trim().startsWith("<ns:") && head[1].toString().trim().startsWith("<ns:"))
			  )
				return true;
		
		return false;
	}

}
