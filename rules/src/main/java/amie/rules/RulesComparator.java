package amie.rules;

import java.util.Comparator;

public class RulesComparator implements Comparator<Rule> {

	@Override
	public int compare(Rule o1, Rule o2) {
		if (Double.compare(o1.getPcaConfidence(), o2.getPcaConfidence()) == 0) {
			if (Double.compare(o1.getSupport(), o2.getSupport()) == 0) {
				return Integer.compare(o1.hashCode(), o2.hashCode());
			} else {
				return Double.compare(o1.getSupport(), o2.getSupport());
			}
		} else {
			return Double.compare(o1.getPcaConfidence(), o2.getPcaConfidence());
		}
	}

}
