package amie.rosa;

import amie.data.KB;
import java.io.File;
import java.io.IOException;
import java.util.List;


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
		
		List<int[]> body = rule.getBody();
		int[] head = rule.getHead();
		
		if((KB.unmap(body.get(0)[1]).trim().startsWith("<ns:") && KB.unmap(body.get(1)[1]).trim().startsWith("<ns:") && !KB.unmap(head[1]).trim().startsWith("<ns:"))
				|| 	(!KB.unmap(body.get(0)[1]).trim().startsWith("<ns:") && !KB.unmap(body.get(1)[1]).trim().startsWith("<ns:") && KB.unmap(head[1]).trim().startsWith("<ns:"))
			  )
				return true;
		
		return false;
	}

}
