package amie.rules.format;

import amie.rules.Rule;

class DefaultRuleFormatter extends RuleFormatter {

	protected DefaultRuleFormatter(boolean verbose) {
		super(verbose);
	}

	@Override
	public String format(Rule rule) {
		return rule.getRuleString();
	}

	@Override
	public OutputColumn[] columns() {
		OutputColumn[] allMetrics = null;
		if (this.verbose) {
			allMetrics = new OutputColumn[RuleFormatter.headers.size()];
			RuleFormatter.headers.toArray(allMetrics);
		} else {
			allMetrics = new OutputColumn[RuleFormatter.headers.size() - 3];
			RuleFormatter.headers.subList(0, RuleFormatter.headers.size() - 3).toArray(allMetrics);
		}

		return allMetrics;
	}

}