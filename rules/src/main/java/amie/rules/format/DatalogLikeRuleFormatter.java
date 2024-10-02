package amie.rules.format;

import amie.rules.Rule;

public class DatalogLikeRuleFormatter extends DefaultRuleFormatter {

	protected DatalogLikeRuleFormatter(boolean verbose) {
		super(verbose);
	}

	@Override
	public String format(Rule rule) {
		return rule.getDatalogString(true);
	}
}
