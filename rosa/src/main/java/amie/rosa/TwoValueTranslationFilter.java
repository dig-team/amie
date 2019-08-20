package amie.rosa;

import java.io.File;
import java.io.IOException;
import java.util.List;


import amie.data.KB;
import amie.rules.AMIEParser;
import amie.rules.Rule;

public class TwoValueTranslationFilter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		List<Rule> rules = AMIEParser.rules(new File(args[0]));
		
		for(Rule rule: rules){
			if(followsAttributeValueTranslation(rule))
				System.out.println(rule.getRuleString());
		}
	}

	private static boolean followsAttributeValueTranslation(Rule rule) {
		if(rule.getLength() != 3)
			return false;
		
		List<int[]> body = rule.getBody();
		int[] head = rule.getHead();
		boolean containsObject = !KB.isVariable(body.get(0)[2]) || !KB.isVariable(body.get(1)[2]);
		
		if(containsObject && (KB.unmap(body.get(0)[1]).trim().startsWith("<ns:") && KB.unmap(body.get(1)[1]).trim().startsWith("<ns:") && !KB.unmap(head[1]).trim().startsWith("<ns:"))
				|| 	(!KB.unmap(body.get(0)[1]).trim().startsWith("<ns:") && !KB.unmap(body.get(1)[1]).trim().startsWith("<ns:") && KB.unmap(head[1]).trim().startsWith("<ns:"))
			  )
				return true;
		
		return false;
	}

}
