package amie.rules.format;

public class NoStdDefaultRuleFormatter extends DefaultRuleFormatter {

	protected NoStdDefaultRuleFormatter(boolean verbose) {
		super(verbose);
	}

	@Override
	public OutputColumn[] columns() {
		OutputColumn[] columns = super.columns();
		OutputColumn[] allMetrics = new OutputColumn[columns.length - 2];
		int i = 0;
		while (i < columns.length - 2) {
			if (columns[i] != OutputColumn.StandardConfidence && columns[i] != OutputColumn.BodySize) {
				allMetrics[i] = columns[i];
				i++;
			}
		}

		return allMetrics;
	}
}
