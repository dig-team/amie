package amie.data;

import javatools.datatypes.Pair;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// TODO test datalog representation
public class TestKBTripleParsing extends TestCase {

    List<Pair<int[], int[]>> cases;

    public void setUp() throws Exception {
        super.setUp();
        cases = new ArrayList<>();

        // 2 triples pattern
        List<String[]> triples = Arrays.asList(
                new String[]{"bob", "loves", "?x2"},
                new String[]{"bob", "loves_very_much", "?x2"},
                new String[]{"bob", "s2", "?x2"},
                new String[]{"?x2", "livesIn", "?x3"},
                new String[]{"?x2", "currently_lives_in", "?x3"},
                new String[]{"?g", "<connectsTo>", "?a"},
                new String[]{"?x2", "lives_in", "?x3"},
                new String[]{"?a", "/film/actor/film./film/performance/film", "/m/0340hj"},
                new String[]{"?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/02x8n1n"},
                new String[]{"?a", "/film/actor/film./film/performance/film", "/m/abc"},
                new String[]{"?a", "neg_/award/award_nominee/award_nominations./award/award_nomination/award", "/m/dfg"}
        );

        for (String[] triple : triples) {
            Pair<int[], int[]> p = new Pair<>(KB.triple(triple), KB.triple(String.join(" ", triple)));
            cases.add(p);
        }
    }

    private void runTestCaseId(int case_id) {
        String q1 = KB.toString(cases.get(case_id).first);
        int[] q2 = cases.get(case_id).second;
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

    public void test7() {
        runTestCaseId(7);
    }

    public void test8() {
        runTestCaseId(8);
    }

    public void test9() {
        runTestCaseId(9);
    }

    public void test10() {
        runTestCaseId(10);
    }
}
