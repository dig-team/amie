package amie.rules.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import amie.rules.Rule;

/**
 * Abstract class designed to format rules in different ways
 */
public abstract class RuleFormatter {
	protected boolean verbose;

	/**
	 * Column headers
	 */
	public static final List<OutputColumn> headers = Arrays.asList(OutputColumn.Rule, OutputColumn.HeadCoverage,
			OutputColumn.StandardConfidence,
			OutputColumn.PcaConfidence, OutputColumn.Support, OutputColumn.BodySize, OutputColumn.PcaBodySize,
			OutputColumn.FunctionalVariable, OutputColumn.StdConfUpperBound, OutputColumn.PcaConfUpperBound,
			OutputColumn.PcaConfEstimation);

	protected static final HashMap<OutputColumn, String> formatMappings = new HashMap<>();

	static {
		formatMappings.put(OutputColumn.Rule, "%s");
		formatMappings.put(OutputColumn.HeadCoverage, "%f");
		formatMappings.put(OutputColumn.StandardConfidence, "%f");
		formatMappings.put(OutputColumn.PcaConfidence, "%f");
		formatMappings.put(OutputColumn.Support, "%.0f");
		formatMappings.put(OutputColumn.BodySize, "%d");
		formatMappings.put(OutputColumn.PcaBodySize, "%.0f");
		formatMappings.put(OutputColumn.FunctionalVariable, "%d");
		formatMappings.put(OutputColumn.StdConfUpperBound, "%f");
		formatMappings.put(OutputColumn.PcaConfUpperBound, "%f");
		formatMappings.put(OutputColumn.PcaConfEstimation, "%f");
	}

	public abstract String format(Rule rule);

	public abstract OutputColumn[] columns();

	protected RuleFormatter(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String header() {
		OutputColumn[] columns = columns();
		List<String> strs = new ArrayList<>();
		for (OutputColumn col : columns) {
			if (col == OutputColumn.Rule) {
				strs.add("Rule");
			} else {
				strs.add(col.toString().replaceAll("(.)([A-Z])", "$1 $2"));
			}

		}
		return String.join(getSeparator(), strs) + "\n";
	}

	public String getSeparator() {
		return "\t";
	}

	public String fullFormat(Rule rule) {
		StringBuilder strBuilder = new StringBuilder();
		ArrayList<String> selectedCols = new ArrayList<>();
		for (OutputColumn col : this.columns()) {
			if (col == OutputColumn.Rule) {
				selectedCols.add(format(rule));
			} else {
				selectedCols.add(String.format(formatMappings.get(col),
						rule.getOutputColumn(col)));
			}
		}
		strBuilder.append(String.join(getSeparator(), selectedCols));
		return strBuilder.toString();
	}
}
