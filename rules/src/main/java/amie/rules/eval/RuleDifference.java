package amie.rules.eval;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import amie.rules.*;

public class RuleDifference {

	public static void main(String[] args) throws IOException {
		List<String> ruleStrings1 = Files.readAllLines(new File(args[0]).toPath(), Charset.defaultCharset());
		List<String> ruleStrings2 = Files.readAllLines(new File(args[1]).toPath(), Charset.defaultCharset());
		
		List<Rule> rules1 = fromString2Rules(ruleStrings1);
		List<Rule> rules2 = fromString2Rules(ruleStrings2);
		rules1.removeAll(rules2);
		
		for (Rule rule : rules1) {
			System.out.println(rule.getRuleString());
		}
	}

	private static List<Rule> fromString2Rules(List<String> ruleStrings) {
		List<Rule> result = new ArrayList<Rule>();
		for (String ruleString : ruleStrings) {
			result.add(AMIEParser.rule(ruleString));
		}
		
		return result;
	}

}
