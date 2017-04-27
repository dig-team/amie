package amie.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amie.data.KB;

import javatools.datatypes.ByteString;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;

/** 
 * Parses a file of AMIE rules
 * 
 * @author Fabian
 *
 */
public class AMIEParser {
	
	/**
	 * Parsers an AMIE rule from a string.
	 * @param s
	 * @return
	 */
	public static Rule rule(String s) {	
		Pair<List<ByteString[]>, ByteString[]> rulePair = KB.rule(s);
		if(rulePair == null) return null;
		Rule resultRule = new Rule(rulePair.second, rulePair.first, 0);
		return resultRule;
	}	
  
	public static void normalizeRule(Rule q){
		char c = 'a';
		Map<ByteString, Character> charmap = new HashMap<ByteString, Character>();
		for(ByteString[] triple: q.getTriples()){
			for(int i = 0;  i < triple.length; ++i){
				if(KB.isVariable(triple[i])){
					Character replace = charmap.get(triple[i]);
					if(replace == null){
						replace = new Character(c);
						charmap.put(triple[i], replace);
						c = (char) (c + 1);
					}
					triple[i] = ByteString.of("?" + replace);				
				}
			}
		}
	}	

	public static List<Rule> rules(File f) throws IOException {
	    List<Rule> result=new ArrayList<>();
	    for(String line : new FileLines(f)) {
	    	ArrayList<ByteString[]> triples=KB.triples(line);
	    	if(triples==null || triples.size()<2) continue;      
	    	ByteString[] last=triples.get(triples.size()-1);
	    	triples.remove(triples.size()-1);
	    	triples.add(0, last);
	    	Rule query=new Rule();
	 
	    	ArrayList<ByteString> variables = new ArrayList<ByteString>();
	    	for(ByteString[] triple: triples){
	    		if(!variables.contains(triple[0]))
	    			variables.add(triple[0]);
	    		if(!variables.contains(triple[2]))
	    			variables.add(triple[2]);
	    	}
	      
	    	query.setSupport(0);
	    	query.setTriples(triples);
	    	query.setFunctionalVariablePosition(0);
	    	result.add(query);
	    }
	    return(result);
	}
	  
	public static void main(String[] args) throws Exception {
	    System.out.println(AMIEParser.rule("=> ?a <hasChild> ?b"));
	}
}
