package amie.data;

import javafx.util.Pair;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

// TODO test datalog representation
public class TestKBRuleParsing extends TestCase {

    List<Pair<List<int[]>, int[]>> cases;

    public void setUp() throws Exception {
        super.setUp();
        cases = new ArrayList<>();

        // 2 triples pattern
        Pair<List<int[]>, int[]> q0 = new Pair<>(
                KB.triples(KB.triple("bob", "loves", "?x2")),
                KB.triple("?x2", "livesIn", "?x3")
        );
        cases.add(q0);

        // Multiple triples pattern with < >
        Pair<List<int[]>, int[]> q1 = new Pair<>(
                KB.triples(
                        KB.triple("?a", "<hasZ>", "?b"), KB.triple("?g", "<connectsTo>", "?a"),
                        KB.triple("?n", "<hasZ>", "?b"), KB.triple("?g", "<relatesTo>", "?a")
                ),
                KB.triple("?g", "<relatesTo>", "?n")
        );
        cases.add(q1);

        // 2-triple pattern with / and number
        Pair<List<int[]>, int[]> q2 = new Pair<>(
                KB.triples(KB.triple("?a", "/film/actor/film./film/performance/film", "/m/0340hj")),
                KB.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/02x8n1n")
        );
        cases.add(q2);

        // 2-triple pattern with _
        Pair<List<int[]>, int[]> q3 = new Pair<>(
                KB.triples(KB.triple("bob", "really_loves", "?x2")),
                KB.triple("?x2", "lives_in", "?x3")
        );
        cases.add(q3);

        // 2-triple pattern with multiple _
        Pair<List<int[]>, int[]> q4 = new Pair<>(
                KB.triples(KB.triple("bob", "loves_very_much", "?x2")),
                KB.triple("?x2", "currently_lives_in", "?x3")
        );
        cases.add(q4);

        // 2-triple pattern with /
        Pair<List<int[]>, int[]> q5 = new Pair<>(
                KB.triples(KB.triple("?a", "/film/actor/film./film/performance/film", "/m/abc")),
                KB.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/dfg")
        );
        cases.add(q5);

        // 2-triple pattern with number
        Pair<List<int[]>, int[]> q6 = new Pair<>(
                KB.triples(KB.triple("bob", "s2", "?x2")),
                KB.triple("?x2", "lives_in", "?x3")
        );
        cases.add(q6);
    }

    private void runTestCaseId(int case_id) {
        Pair<List<int[]>, int[]> c = cases.get(case_id);
        String expected = KB.toString(c.getKey()) + " => " + KB.toString(c.getValue());
        Pair<List<int[]>, int[]> result = KB.rule(expected);

        assertNotNull("expected:<" + expected + ">", result);
        String actual = KB.toString(result.getKey()) + " => " + KB.toString(result.getValue());
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
