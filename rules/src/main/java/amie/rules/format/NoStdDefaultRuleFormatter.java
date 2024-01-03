package amie.rules.format;

import amie.rules.Metric;

public class NoStdDefaultRuleFormatter extends DefaultRuleFormatter {

	protected NoStdDefaultRuleFormatter(boolean verbose) {
		super(verbose);
	}
	
	@Override
	public Metric[] columns() {
		Metric[] columns = super.columns();
		Metric[] allMetrics = new Metric[columns.length - 2];
		int i = 0;
		while(i < columns.length - 2) {
			if (columns[i] != Metric.StdConfidence && columns[i] != Metric.BodySize) {
				allMetrics[i] = columns[i];
				i++;
			}
		}

		return allMetrics;
	}
}
