package amie.linkprediction;

import amie.data.Dataset;
import amie.data.KB;
import amie.rules.Rule;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestEvaluator extends TestCase {
    KB kb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.kb = new KB();
        this.kb.add("Luis", "livesIn", "Rennes");
        this.kb.add("Luis", "worksAt", "Inria");
        this.kb.add("Inria", "locatedIn", "Rennes");
        this.kb.add("Luis", "marriedTo", "Goulven");
        this.kb.add("Goulven", "livesIn", "Rennes");
        this.kb.add("Goulven", "bornIn", "Scaer");
        this.kb.add("Tassadit", "livesIn", "Montpellier");
        this.kb.add("Christophe", "worksAt", "SomeCompany");
        this.kb.add("SomeCompany", "locatedIn", "Scaer");
        this.kb.add("SomeCompany", "locatedIn", "Montpellier");
        this.kb.add("Christophe", "marriedTo", "Tassadit");
        this.kb.add("Christophe", "bornIn", "Scaer");
        this.kb.add("Julianne", "bornIn", "Carcassone");
        this.kb.add("Technicolor", "locatedIn", "Paris");
        this.kb.add("Julianne", "worksAt", "SomeCompany");
    }

    public void testToyRuleSupportsQueryEvaluation() {
        Rule r1 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("worksAt"), -3},
                        new int[]{-3, kb.map("locatedIn"), -2}), 2, kb
        );

        Rule r2 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("marriedTo"), -3},
                        new int[]{-3, kb.map("livesIn"), -2}), 2, kb
        );

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        Evaluator ev = new Evaluator(new Dataset(kb, testTriples), List.of(r1, r2));
        // Some trivial assertions
        assertFalse(ev.ruleSupportsEntityForQuery(r1, kb.map("SomeCompany"), new Query(kb, kb.map("Christophe"),
                kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, kb.map("SomeCompany"), new Query(kb, kb.map("Christophe"),
                kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, kb.map("Inria"), new Query(kb, kb.map("Christophe"),
                kb.map("worksAt"), -1)));

        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Scaer"), new Query(kb, kb.map("Christophe"),
                kb.map("livesIn"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, kb.map("Scaer"), new Query(kb, kb.map("Christophe"),
                kb.map("livesIn"), -1)));
        assertTrue(ev.ruleSupportsEntityForQuery(r2, kb.map("Rennes"), new Query(kb, kb.map("Luis"),
                kb.map("livesIn"), -1)));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Rennes"), new Query(kb, kb.map("Luis"),
                kb.map("livesIn"), -1)));

        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Christophe"), new Query(kb, -1, kb.map("livesIn"),
                kb.map("Scaer"))));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, kb.map("Christophe"), new Query(kb, -1, kb.map("livesIn"),
                kb.map("Scaer"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r2, kb.map("Luis"), new Query(kb, -1, kb.map("livesIn"),
                kb.map("Rennes"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Luis"), new Query(kb, -1, kb.map("livesIn"),
                kb.map("Rennes"))));
    }

    public void testToyRuleWithConstantsSupportsQueryEvaluation() {
        Rule r1 = new Rule(new int[]{-1, kb.map("worksAt"), kb.map("SomeCompany")},
                List.of(new int[]{-1, kb.map("bornIn"), kb.map("Scaer")}), 1, kb
        );
        System.out.println(kb);

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        Evaluator ev = new Evaluator(new Dataset(kb, testTriples), List.of(r1));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("SomeCompany"), new Query(kb, kb.map("Christophe"),
                kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, kb.map("Inria"), new Query(kb, kb.map("Christophe"),
                kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, kb.map("Tassadit"), new Query(kb, -1, kb.map("livesIn"),
                kb.map("Tassadit"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, kb.map("Christophe"), new Query(kb, -1, kb.map("worksAt"),
                kb.map("SomeCompany"))));
    }

    public void testSimpleRank() {
        Rule r1 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("worksAt"), -3},
                        new int[]{-3, kb.map("locatedIn"), -2}), 60, kb
        );
        r1.setBodySize(100); // r1 has confidence and PCA confidence 0.6
        r1.setPcaBodySize(100);

        Rule r2 = new Rule(new int[]{-1, kb.map("livesIn"), -2},
                List.of(new int[]{-1, kb.map("marriedTo"), -3},
                        new int[]{-3, kb.map("livesIn"), -2}), 80, kb
        );
        r2.setBodySize(100); // r2 has confidence and PCA confidence 0.8
        r2.setPcaBodySize(100);

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        testTriples.put(kb.map("livesIn"), List.of(
                new int[]{kb.map("Christophe"), kb.map("livesIn"), kb.map("Montpellier")}, // r1 and r2
                new int[]{kb.map("Julianne"), kb.map("livesIn"), kb.map("Rennes")} // no rule
        ));
        Evaluator ev = new Evaluator(new Dataset(kb, testTriples), List.of(r1, r2));
        Set<Integer> candidates = ev.getQueryCandidatesStream(kb.map("livesIn"), 2).collect(Collectors.toSet());
        System.out.println(candidates);
        assertEquals(candidates.size(), 4);
        assertTrue(candidates.contains(kb.map("Rennes")));
        assertTrue(candidates.contains(kb.map("Scaer")));
        assertTrue(candidates.contains(kb.map("Montpellier")));
        assertTrue(candidates.contains(kb.map("Paris")));
        Query tailQuery = new Query(kb, kb.map("Christophe"), kb.map("livesIn"), -1);
        Ranking rankingTail = new Ranking(tailQuery);
        rankingTail.addSolution(ev.getEntityScoresForQuery(kb.map("Rennes"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(kb.map("Scaer"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(kb.map("Montpellier"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(kb.map("Paris"), tailQuery));
        rankingTail.build();
        assertTrue(rankingTail.rank(kb.map("Rennes")).intValue() > rankingTail.rank(kb.map("Scaer")).intValue());
        assertTrue(rankingTail.rank(kb.map("Paris")).intValue() > rankingTail.rank(kb.map("Scaer")).intValue());
        assertEquals(rankingTail.rank(kb.map("Montpellier")).intValue(), 1);
        assertEquals(rankingTail.rank(kb.map("Scaer")).intValue(), 2);
        assertEquals(rankingTail.filteredRank(kb.map("Montpellier")).intValue(), 1);
        assertEquals(rankingTail.filteredRank(kb.map("Scaer")).intValue(), 2);

        EvaluationResult eresult = ev.evaluate();
        System.out.println(eresult);
    }

    public void testRankWn18RR() throws IOException {
        Evaluator ev = Evaluator.getEvaluator("/home/lgalarra/Documents/git/mm-kge/data/wn18rr/",
                "wn18rr.rules");
        EvaluationResult eresult = ev.evaluate();
        System.out.println(eresult);
    }
}
