package amie.rules.format;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amie.rules.Rule;

public class AnyBurlFormatter extends RuleFormatter {

	public AnyBurlFormatter(boolean verbose) {
		super(verbose);
	}

	public String header() {
		return "";
	}

	@Override
	public String format(Rule rule) {
		String ruleString = rule.getDatalogPathString();
		ruleString = ruleString.replace("?a", "X").replace("?b", "Y");
		Matcher m = Pattern.compile("(\\?[a-z])").matcher(ruleString);
		char newVar = 'A';
		Map<String, String> replaceMap = new HashMap<>();
		while (m.find()) {
			String var = m.group();
			if (!replaceMap.containsKey(var)) {
				replaceMap.put(var, "" + newVar);
				newVar++;
			}
			ruleString = ruleString.replace(var, replaceMap.get(var));
		}
		return ruleString;
	}

	@Override
	public OutputColumn[] columns() {
		return new OutputColumn[] { OutputColumn.PcaBodySize, OutputColumn.Support,
				OutputColumn.PcaConfidence, OutputColumn.Rule };
	}

}
