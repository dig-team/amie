package amie.rules.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amie.rules.Metric;
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
		String ruleString = rule.getDatalogString();
		ruleString = ruleString.replace("?a", "X").replace("?b", "Y");
		Matcher m = Pattern.compile("(\\?[a-z])").matcher(ruleString);
		char start = 'A';
		while (m.find()) {
			String var = m.group();
			ruleString = ruleString.replace(var, "" + start);
			start++;
		}
		return ruleString;
	}

	@Override
	public Metric[] columns() {
		return new Metric[]{Metric.PcaBodySize, Metric.PositiveExamples, Metric.PcaConfidence, Metric.None};
	}
	
}
