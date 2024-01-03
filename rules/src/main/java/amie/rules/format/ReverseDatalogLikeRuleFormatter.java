package amie.rules.format;

import amie.rules.Rule;

public class ReverseDatalogLikeRuleFormatter extends DatalogLikeRuleFormatter {

	protected ReverseDatalogLikeRuleFormatter(boolean verbose) {
		super(verbose);
	}

	@Override
	public String format(Rule rule) {
		return rule.getReverseDatalogRuleString();
	}

}
