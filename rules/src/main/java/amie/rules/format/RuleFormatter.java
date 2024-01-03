package amie.rules.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import amie.rules.Metric;
import amie.rules.Rule;

/**
 * Abstract class designed to format rules in different ways
 */
public abstract class RuleFormatter {
	protected boolean verbose;

	/**
     * Column headers
     */
    public static final List<Metric> headers = Arrays.asList(Metric.None, Metric.HeadCoverage, Metric.StdConfidence,
            Metric.PcaConfidence, Metric.PositiveExamples, Metric.BodySize, Metric.PcaBodySize,
            Metric.FunctionalVariable, Metric.StdUpperBound, Metric.PcaUpperBound, Metric.PcaConfEstimation);


	public abstract String format(Rule rule);
	public abstract Metric[] columns();
	
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
		Metric[] columns = columns();
		List<String> strs = new ArrayList<>();
		for (Metric col: columns) {
			if (col == Metric.None) {
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
		for (Metric col: this.columns()) {
			if (col == Metric.None){
				selectedCols.add(format(rule));
			} else {
				selectedCols.add(""+rule.getMetric(col));
			}
		} 
		strBuilder.append(String.join(getSeparator(), selectedCols));
		return strBuilder.toString();
	}
}
