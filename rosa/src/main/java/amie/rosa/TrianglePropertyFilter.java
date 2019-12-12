package amie.rosa;

import amie.data.KB;
import java.io.File;
import java.io.IOException;
import java.util.List;


import amie.rules.AMIEParser;
import amie.rules.Rule;

public class TrianglePropertyFilter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		List<Rule> rules = AMIEParser.rules(new File(args[0]));
		
		for(Rule rule: rules){
			if(followsTriangleProperty(rule))
				System.out.println(rule.getRuleString());
		}

	}

	private static boolean followsTriangleProperty(Rule rule) {
		// TODO Auto-generated method stub
		if(rule.getLength() != 3)
			return false;
		
		List<int[]> body = rule.getBody();
		int[] head = rule.getHead();
		
		if(body.get(0)[1] != (body.get(1)[1]))
			return false;
		
		if(! (body.get(0)[0] == (body.get(1)[0]) || body.get(0)[2] == (body.get(1)[2]) ))
			return false;
		
		if(!(KB.unmap(body.get(0)[1]).startsWith("<ns:") && KB.unmap(body.get(1)[1]).startsWith("<ns:") && !KB.unmap(head[1]).startsWith("<ns:"))
			|| 	!(!KB.unmap(body.get(0)[1]).startsWith("<ns:") && !KB.unmap(body.get(1)[1]).startsWith("<ns:") && KB.unmap(head[1]).startsWith("<ns:"))
		  )
			return false;
			
		return true;
	}

}
