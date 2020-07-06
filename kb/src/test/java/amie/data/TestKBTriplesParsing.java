package amie.data;

import javatools.datatypes.Pair;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

// TODO test datalog representation
public class TestKBTriplesParsing extends TestCase {

    List<Pair<List<int[]>, List<int[]>>> cases;

    public void setUp() throws Exception {
        super.setUp();
        cases = new ArrayList<>();

        // 2 triples pattern
        List<int[]> q01 = KB.triples(KB.triple("bob", "loves", "?x2"),
                KB.triple("?x2", "livesIn", "?x3"));
        List<int[]> q02 = KB.triples("bob loves ?x2  ?x2 livesIn ?x3");
        Pair<List<int[]>, List<int[]>> p0 = new Pair<>(q01, q02);
        cases.add(p0);

        // Multiple triples pattern with < >
        List<int[]> q11 = KB.triples(
                KB.triple("?a", "<hasZ>", "?b"),
                KB.triple("?g", "<connectsTo>", "?a"), KB.triple("?n", "<hasZ>", "?b"),
                KB.triple("?g", "<relatesTo>", "?a"), KB.triple("?g", "<relatesTo>", "?n")
        );
        List<int[]> q12 = KB.triples("?a  <hasZ>  ?b  ?g  <connectsTo>  ?a  ?n  <hasZ>  ?b  ?g  <relatesTo>  ?a  ?g  <relatesTo>  ?n");
        Pair<List<int[]>, List<int[]>> p1 = new Pair<>(q11, q12);
        cases.add(p1);

        // 2-triple pattern with / and number
        List<int[]> q21 = KB.triples(
                KB.triple("?a", "/film/actor/film./film/performance/film", "/m/0340hj"),
                KB.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/02x8n1n")
        );
        List<int[]> q22 = KB.triples("?a /film/actor/film./film/performance/film /m/0340hj " +
                "?a neg_/award/award_nominee/award_nominations./award/award_nomination/award /m/02x8n1n");
        Pair<List<int[]>, List<int[]>> p2 = new Pair<>(q21, q22);
        cases.add(p2);

        // 2-triple pattern with _
        List<int[]> q31 = KB.triples(KB.triple("bob", "really_loves", "?x2"),
                KB.triple("?x2", "lives_in", "?x3"));
        List<int[]> q32 = KB.triples("bob really_loves ?x2  ?x2 lives_in ?x3");
        Pair<List<int[]>, List<int[]>> p3 = new Pair<>(q31, q32);
        cases.add(p3);

        // 2-triple pattern with multiple _
        List<int[]> q41 = KB.triples(KB.triple("bob", "loves_very_much", "?x2"),
                KB.triple("?x2", "currently_lives_in", "?x3"));
        List<int[]> q42 = KB.triples("bob loves_very_much ?x2  ?x2 currently_lives_in ?x3");
        Pair<List<int[]>, List<int[]>> p4 = new Pair<>(q41, q42);
        cases.add(p4);

        // 2-triple pattern with /
        List<int[]> q51 = KB.triples(
                KB.triple("?a", "/film/actor/film./film/performance/film", "/m/abc"),
                KB.triple("?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/dfg")
        );
        List<int[]> q52 = KB.triples("?a /film/actor/film./film/performance/film /m/abc " +
                "?a neg_/award/award_nominee/award_nominations./award/award_nomination/award /m/dfg");
        Pair<List<int[]>, List<int[]>> p5 = new Pair<>(q51, q52);
        cases.add(p5);

        // 2-triple pattern with number
        List<int[]> q61 = KB.triples(KB.triple("bob", "s2", "?x2"),
                KB.triple("?x2", "lives_in", "?x3"));
        List<int[]> q62 = KB.triples("bob s2 ?x2  ?x2 lives_in ?x3");
        Pair<List<int[]>, List<int[]>> p6 = new Pair<>(q61, q62);
        cases.add(p6);
    }

    private void runTestCaseId(int case_id) {
        String q1 = KB.toString(cases.get(case_id).first);
        List<int[]> q2 = cases.get(case_id).second;
        assertNotNull("expected:<" + q1 + ">", q2);
        assertEquals(q1, KB.toString(q2));
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
