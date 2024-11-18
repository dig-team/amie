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
    KB first_kb;
    KB second_kb;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.first_kb = new KB();
        this.first_kb.add("Luis", "livesIn", "Rennes");
        this.first_kb.add("Luis", "worksAt", "Inria");
        this.first_kb.add("Inria", "locatedIn", "Rennes");
        this.first_kb.add("Luis", "marriedTo", "Goulven");
        this.first_kb.add("Goulven", "livesIn", "Rennes");
        this.first_kb.add("Goulven", "bornIn", "Scaer");
        this.first_kb.add("Tassadit", "livesIn", "Montpellier");
        this.first_kb.add("Christophe", "worksAt", "SomeCompany");
        this.first_kb.add("SomeCompany", "locatedIn", "Scaer");
        this.first_kb.add("SomeCompany", "locatedIn", "Montpellier");
        this.first_kb.add("Christophe", "marriedTo", "Tassadit");
        this.first_kb.add("Christophe", "bornIn", "Scaer");
        this.first_kb.add("Julianne", "bornIn", "Carcassone");
        this.first_kb.add("Technicolor", "locatedIn", "Paris");
        this.first_kb.add("Julianne", "worksAt", "SomeCompany");

        this.second_kb = new KB();
        this.second_kb.add("Pedro", "nationality", "Spain");
        this.second_kb.add("Gonzalo", "nationality", "Spain");
        this.second_kb.add("Pedro", "father", "Gonzalo");

        this.second_kb.add("Diana", "speaks", "English");
        this.second_kb.add("Gabriel", "speaks", "Spanish");
        this.second_kb.add("Spain", "officialLang", "Spanish");
        this.second_kb.add("Spain", "officialLang", "Catalan");

        this.second_kb.add("Alfonsina", "nationality", "Mexico");
        this.second_kb.add("Mexico", "officialLang", "Spanish");

        this.second_kb.add("Sabrina", "fof", "Alfonsina");
        this.second_kb.add("Alfonsina", "fof", "Sabrina");
        this.second_kb.add("Sabrina", "speaks", "Russian");
    }

    public void testToyRuleSupportsQueryEvaluation() {
        Rule r1 = new Rule(new int[]{-1, first_kb.map("livesIn"), -2},
                List.of(new int[]{-1, first_kb.map("worksAt"), -3},
                        new int[]{-3, first_kb.map("locatedIn"), -2}), 2, first_kb
        );

        Rule r2 = new Rule(new int[]{-1, first_kb.map("livesIn"), -2},
                List.of(new int[]{-1, first_kb.map("marriedTo"), -3},
                        new int[]{-3, first_kb.map("livesIn"), -2}), 2, first_kb
        );

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        Evaluator ev = new Evaluator(new Dataset(first_kb, testTriples), List.of(r1, r2));
        // Some trivial assertions
        assertFalse(ev.ruleSupportsEntityForQuery(r1, first_kb.map("SomeCompany"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, first_kb.map("SomeCompany"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Inria"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("worksAt"), -1)));

        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Scaer"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("livesIn"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, first_kb.map("Scaer"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("livesIn"), -1)));
        assertTrue(ev.ruleSupportsEntityForQuery(r2, first_kb.map("Rennes"), new Query(first_kb, first_kb.map("Luis"),
                first_kb.map("livesIn"), -1)));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Rennes"), new Query(first_kb, first_kb.map("Luis"),
                first_kb.map("livesIn"), -1)));

        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Christophe"), new Query(first_kb, -1, first_kb.map("livesIn"),
                first_kb.map("Scaer"))));
        assertFalse(ev.ruleSupportsEntityForQuery(r2, first_kb.map("Christophe"), new Query(first_kb, -1, first_kb.map("livesIn"),
                first_kb.map("Scaer"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r2, first_kb.map("Luis"), new Query(first_kb, -1, first_kb.map("livesIn"),
                first_kb.map("Rennes"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Luis"), new Query(first_kb, -1, first_kb.map("livesIn"),
                first_kb.map("Rennes"))));
    }

    public void testToyRuleWithConstantsSupportsQueryEvaluation() {
        Rule r1 = new Rule(new int[]{-1, first_kb.map("worksAt"), first_kb.map("SomeCompany")},
                List.of(new int[]{-1, first_kb.map("bornIn"), first_kb.map("Scaer")}), 1, first_kb
        );
        System.out.println(first_kb);

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        Evaluator ev = new Evaluator(new Dataset(first_kb, testTriples), List.of(r1));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("SomeCompany"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Inria"), new Query(first_kb, first_kb.map("Christophe"),
                first_kb.map("worksAt"), -1)));
        assertFalse(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Tassadit"), new Query(first_kb, -1, first_kb.map("livesIn"),
                first_kb.map("Tassadit"))));
        assertTrue(ev.ruleSupportsEntityForQuery(r1, first_kb.map("Christophe"), new Query(first_kb, -1, first_kb.map("worksAt"),
                first_kb.map("SomeCompany"))));
    }

    public void testSimpleRank() {
        Rule r1 = new Rule(new int[]{-1, first_kb.map("livesIn"), -2},
                List.of(new int[]{-1, first_kb.map("worksAt"), -3},
                        new int[]{-3, first_kb.map("locatedIn"), -2}), 60, first_kb
        );
        r1.setBodySize(100); // r1 has confidence and PCA confidence 0.6
        r1.setPcaBodySize(100);

        Rule r2 = new Rule(new int[]{-1, first_kb.map("livesIn"), -2},
                List.of(new int[]{-1, first_kb.map("marriedTo"), -3},
                        new int[]{-3, first_kb.map("livesIn"), -2}), 80, first_kb
        );
        r2.setBodySize(100); // r2 has confidence and PCA confidence 0.8
        r2.setPcaBodySize(100);

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        testTriples.put(first_kb.map("livesIn"), List.of(
                new int[]{first_kb.map("Christophe"), first_kb.map("livesIn"), first_kb.map("Montpellier")}, // r1 and r2
                new int[]{first_kb.map("Julianne"), first_kb.map("livesIn"), first_kb.map("Rennes")} // no rule
        ));
        Evaluator ev = new Evaluator(new Dataset(first_kb, testTriples), List.of(r1, r2));
        Set<Integer> candidates = ev.getQueryCandidatesStream(first_kb.map("livesIn"), 2).collect(Collectors.toSet());
        System.out.println(candidates.stream().map(x -> first_kb.unmap(x)).collect(Collectors.toList()));
        assertEquals(candidates.size(), 4);
        assertTrue(candidates.contains(first_kb.map("Rennes")));
        assertTrue(candidates.contains(first_kb.map("Scaer")));
        assertTrue(candidates.contains(first_kb.map("Montpellier")));
        assertTrue(candidates.contains(first_kb.map("Paris")));
        Query tailQuery = new Query(first_kb, first_kb.map("Christophe"), first_kb.map("livesIn"), -1);
        Ranking rankingTail = new Ranking(tailQuery);
        rankingTail.addSolution(ev.getEntityScoresForQuery(first_kb.map("Rennes"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(first_kb.map("Scaer"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(first_kb.map("Montpellier"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(first_kb.map("Paris"), tailQuery));
        rankingTail.build();
        assertTrue(rankingTail.rank(first_kb.map("Rennes")).intValue() > rankingTail.rank(first_kb.map("Scaer")).intValue());
        assertTrue(rankingTail.rank(first_kb.map("Paris")).intValue() > rankingTail.rank(first_kb.map("Scaer")).intValue());
        assertEquals(rankingTail.rank(first_kb.map("Montpellier")).intValue(), 1);
        assertEquals(rankingTail.rank(first_kb.map("Scaer")).intValue(), 2);
        assertEquals(rankingTail.filteredRank(first_kb.map("Montpellier")).intValue(), 1);
        assertEquals(rankingTail.filteredRank(first_kb.map("Scaer")).intValue(), 2);

        EvaluationResult eresult = ev.evaluate();
        System.out.println(eresult);
    }

    public void testSimpleRank2() {
        Rule r1 = new Rule(new int[]{-1, second_kb.map("speaks"), second_kb.map("Spanish")},
                List.of(new int[]{-1, second_kb.map("nationality"), second_kb.map("Spain")},
                        new int[]{-2, second_kb.map("nationality"), second_kb.map("Spain")},
                        new int[]{-1, second_kb.map("father"), -2}), 10, second_kb
        );
        r1.setBodySize(10); // r1 has confidence and PCA confidence 1.0
        r1.setPcaBodySize(10);

        Rule r2 = new Rule(new int[]{-1, second_kb.map("speaks"), -2},
                List.of(new int[]{-3, second_kb.map("officialLang"), -2},
                        new int[]{-1, second_kb.map("nationality"), -3}), 9, second_kb
        );
        r2.setBodySize(10); // r2 has confidence and PCA confidence 0.9
        r2.setPcaBodySize(10);

        Rule r3 = new Rule(new int[]{-1, second_kb.map("speaks"), -2},
                List.of(new int[]{-3, second_kb.map("speaks"), -2},
                        new int[]{-3, second_kb.map("fof"), -1}), 3, second_kb
        );
        r3.setBodySize(10); // r2 has confidence and PCA confidence 0.3
        r3.setPcaBodySize(10);

        Map<Integer, List<int[]>> testTriples = new HashMap<>();
        testTriples.put(second_kb.map("speaks"), List.of(
                new int[]{second_kb.map("Pedro"), second_kb.map("speaks"), second_kb.map("Spanish")}, // r1 and r2
                new int[]{second_kb.map("Alfonsina"), second_kb.map("speaks"), second_kb.map("Russian")}, // r3
                new int[]{second_kb.map("Pedro"), second_kb.map("speaks"), second_kb.map("Catalan")} // r2
        ));

        Evaluator ev = new Evaluator(new Dataset(second_kb, testTriples), List.of(r1, r2, r3));
        Set<Integer> candidates = ev.getQueryCandidatesStream(second_kb.map("speaks"), 2).collect(Collectors.toSet());
        System.out.println(candidates.stream().map(x -> second_kb.unmap(x)).collect(Collectors.toSet()));
        assertEquals(candidates.size(), 4);
        assertTrue(candidates.contains(second_kb.map("Catalan")));
        assertTrue(candidates.contains(second_kb.map("Spanish")));
        assertTrue(candidates.contains(second_kb.map("English")));
        assertTrue(candidates.contains(second_kb.map("Russian")));

        Query tailQuery = new Query(second_kb, second_kb.map("Pedro"), second_kb.map("speaks"), -1);
        Ranking rankingTail = new Ranking(tailQuery);
        rankingTail.addSolution(ev.getEntityScoresForQuery(second_kb.map("Catalan"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(second_kb.map("Spanish"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(second_kb.map("English"), tailQuery));
        rankingTail.addSolution(ev.getEntityScoresForQuery(second_kb.map("Russian"), tailQuery));
        rankingTail.build();
        assertTrue(rankingTail.rank(second_kb.map("Spanish")).intValue() < rankingTail.rank(second_kb.map("Catalan")).intValue());
        assertTrue(rankingTail.rank(second_kb.map("Catalan")).intValue() < rankingTail.rank(second_kb.map("Russian")).intValue());
        assertEquals(rankingTail.rank(second_kb.map("Spanish")).intValue(), 1);
        assertEquals(rankingTail.rank(second_kb.map("Catalan")).intValue(), 2);

        EvaluationResult eresult = ev.evaluate();
        System.out.println(eresult);

        Query headQuery = new Query(second_kb, -1, second_kb.map("speaks"), second_kb.map("Russian"));
        Set<Integer> candidates2 = ev.getQueryCandidatesStream(second_kb.map("speaks"), 0).collect(Collectors.toSet());
        System.out.println(candidates2.stream().map(x -> second_kb.unmap(x)).collect(Collectors.toSet()));
        Ranking rankingHead = new Ranking(headQuery);
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Diana"), headQuery));
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Pedro"), headQuery));
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Gonzalo"), headQuery));
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Alfonsina"), headQuery));
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Sabrina"), headQuery));
        rankingHead.addSolution(ev.getEntityScoresForQuery(second_kb.map("Gabriel"), headQuery));
        rankingHead.build();
        assertTrue(rankingHead.rank(second_kb.map("Alfonsina")).intValue() == 1);

    }
}
