package amie.linkprediction;

import amie.data.Dataset;
import amie.data.KB;
import amie.rules.Rule;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestEvaluator extends TestCase {
    public void testToyEvaluation() {
        KB kb = new KB();
        kb.add("Luis", "livesIn", "Rennes");
        kb.add("Luis", "worksAt", "Inria");
        kb.add("Inria", "locatedIn", "Rennes");
        kb.add("Luis", "marriedTo", "Goulven");
        kb.add("Goulven", "livesIn", "Rennes");
        kb.add("Goulven", "bornIn", "Scaer");
        kb.add("Tassadit", "livesIn", "Montpellier");
        kb.add("Christophe", "worksAt", "SomeCompany");
        kb.add("SomeCompany", "locatedIn", "Scaer");
        kb.add("Christophe", "marriedTo", "Tassadit");

        Rule r1 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("worksAt"), -3},
                        new int[]{-3, kb.map("locatedIn"), -2}), 2, kb
        );

        Rule r2 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("marriedTo"), -3},
                        new int[]{-3, kb.map("locatedIn"), -2}), 2, kb
        );
        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        testTriples.put(kb.map("livesIn"), List.of(new int[]{kb.map("Christophe"), kb.map("livesIn"), kb.map("Rennes")}));
        Evaluator ev = new Evaluator(new Dataset(kb, testTriples), List.of(r1, r2));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Scaer"), new Query(kb, kb.map("Christophe"), kb.map("livesIn"), -1)));
    }
}
