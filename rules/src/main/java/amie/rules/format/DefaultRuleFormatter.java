package amie.rules.format;

import amie.rules.Metric;
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
	public Metric[] columns() {
		Metric[] allMetrics = null;
		if (this.verbose) {
			allMetrics = new Metric[RuleFormatter.headers.size()];
			RuleFormatter.headers.toArray(allMetrics);			
		} else {
			allMetrics = new Metric[RuleFormatter.headers.size() - 3];
			RuleFormatter.headers.subList(0, RuleFormatter.headers.size() - 3).toArray(allMetrics);
		}

		return allMetrics;
	}

}