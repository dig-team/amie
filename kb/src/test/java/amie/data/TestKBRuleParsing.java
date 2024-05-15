package amie.data;

import amie.data.javatools.datatypes.Pair ; 
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

// TODO test datalog representation
public class TestKBRuleParsing extends TestCase {

    List<Pair<List<int[]>, int[]>> cases;
    KB kb = new KB() ; 

    public void setUp() throws Exception {
        super.setUp();
        cases = new ArrayList<>();

        // 2 triples pattern
        Pair<List<int[]>, int[]> q0 = new Pair<>(
                kb.triples(kb.triple("bob", "loves", "?x2")),
                kb.triple("?x2", "livesIn", "?x3")
        );
        cases.add(q0);

        // Multiple triples pattern with < >
        Pair<List<int[]>, int[]> q1 = new Pair<>(
                kb.triples(
                        kb.triple("?a", "<hasZ>", "?b"), kb.triple("?g", "<connectsTo>", "?a"),
                        kb.triple("?n", "<hasZ>", "?b"), kb.triple("?g", "<relatesTo>", "?a")
                ),
                kb.triple("?g", "<relatesTo>", "?n")
        );
        cases.add(q1);

        // 2-triple pattern with / and number
        Pair<List<int[]>, int[]> q2 = new Pair<>(
                kb.triples(kb.triple("?a", "/film/actor/film./film/performance/film", "/m/0340hj")),
                kb.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/02x8n1n")
        );
        cases.add(q2);

        // 2-triple pattern with _
        Pair<List<int[]>, int[]> q3 = new Pair<>(
                kb.triples(kb.triple("bob", "really_loves", "?x2")),
                kb.triple("?x2", "lives_in", "?x3")
        );
        cases.add(q3);

        // 2-triple pattern with multiple _
        Pair<List<int[]>, int[]> q4 = new Pair<>(
                kb.triples(kb.triple("bob", "loves_very_much", "?x2")),
                kb.triple("?x2", "currently_lives_in", "?x3")
        );
        cases.add(q4);

        // 2-triple pattern with /
        Pair<List<int[]>, int[]> q5 = new Pair<>(
                kb.triples(kb.triple("?a", "/film/actor/film./film/performance/film", "/m/abc")),
                kb.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/dfg")
        );
        cases.add(q5);

        // 2-triple pattern with number
        Pair<List<int[]>, int[]> q6 = new Pair<>(
                kb.triples(kb.triple("bob", "s2", "?x2")),
                kb.triple("?x2", "lives_in", "?x3")
        );
        cases.add(q6);
    }

    private void runTestCaseId(int case_id) {
        Pair<List<int[]>, int[]> c = cases.get(case_id);
        String expected = kb.toString(c.first) + " => " + kb.toString(c.second);
        Pair<List<int[]>, int[]> result = kb.rule(expected);

        assertNotNull("expected:<" + expected + ">", result);
        String actual = kb.toString(result.first) + " => " + kb.toString(result.second);
        System.out.println(expected + "\n" + actual);
        assertEquals(expected, actual);
    }

    public void test0() {
        runTestCaseId(0);
    }

    public void test1() {
        runTestCaseId(1);
    }

    public void test2() {
        runTestCaseId(2);
    }

    public void test3() {
        runTestCaseId(3);
    }

    public void test4() {
        runTestCaseId(4);
    }

    public void test5() {
        runTestCaseId(5);
    }

    public void test6() {
        runTestCaseId(6);
    }
}
