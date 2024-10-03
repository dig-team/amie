package amie.rules.format;

import java.util.ArrayList;
import java.util.List;

public class NoStdDefaultRuleFormatter extends DefaultRuleFormatter {

	private boolean ommitStdConf;
	private boolean ommitPCAConf;

	protected NoStdDefaultRuleFormatter(boolean verbose, boolean ommitStdConf, boolean ommitPCAConf) {
		super(verbose);
		this.ommitStdConf = ommitStdConf;
		this.ommitPCAConf = ommitPCAConf;
	}

	@Override
	public OutputColumn[] columns() {
		OutputColumn[] columns = super.columns();
		int i = 0;
		List<OutputColumn> ommittedCols = new ArrayList<>();
		if (this.ommitStdConf) {
			ommittedCols.add(OutputColumn.BodySize);
			ommittedCols.add(OutputColumn.StandardConfidence);
		}
		if (this.ommitPCAConf) {
			ommittedCols.add(OutputColumn.PcaBodySize);
			ommittedCols.add(OutputColumn.PcaConfidence);
		}

		OutputColumn[] allMetrics = new OutputColumn[columns.length - ommittedCols.size()];
		int j = 0;
		for (i = 0; i < columns.length; ++i) {
			if (!ommittedCols.contains(columns[i])) {
				allMetrics[j] = columns[i];
				++j;
			}
		}

		return allMetrics;
	}
}
