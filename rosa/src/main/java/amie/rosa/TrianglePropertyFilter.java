package amie.rosa;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javatools.datatypes.ByteString;
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
		
		List<ByteString[]> body = rule.getBody();
		ByteString[] head = rule.getHead();
		
		if(!body.get(0)[1].equals(body.get(1)[1]))
			return false;
		
		if(! (body.get(0)[0].equals(body.get(1)[0]) || body.get(0)[2].equals(body.get(1)[2]) ))
			return false;
		
		if(!(body.get(0)[1].toString().startsWith("<ns:") && body.get(1)[1].toString().startsWith("<ns:") && !head[1].toString().startsWith("<ns:"))
			|| 	!(!body.get(0)[1].toString().startsWith("<ns:") && !body.get(1)[1].toString().startsWith("<ns:") && head[1].toString().startsWith("<ns:"))
		  )
			return false;
			
		return true;
	}

}
