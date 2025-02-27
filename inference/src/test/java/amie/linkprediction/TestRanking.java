/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.linkprediction;

import amie.data.KB;
import junit.framework.TestCase;

import java.util.List;

/**
 *
 * @author Luis Galárraga
 */
public class TestRanking extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testRankingWithNoDuplicates() {
        Ranking r = new Ranking(null);
        r.addSolution(new Rank(1, 0.33, 0));
        r.addSolution(new Rank(2, 0.34, 0));
        r.addSolution(new Rank(5, 0.34, 0.12));
        r.build();
        assertTrue(r.rank(1).intValue() == 3);
        assertTrue(r.rank(2).intValue() == 2);
        assertTrue(r.rank(5).intValue() == 1);
    }

    public void testRankingWithSameFirstScore() {
        Ranking r = new Ranking(null);
        r.addSolution(new Rank(1, -1, 12));
        r.addSolution(new Rank(2, -1, 25));
        r.addSolution(new Rank(5, -1, 90));
        r.build();
        assertTrue(r.rank(1).intValue() == 3);
        assertTrue(r.rank(5).intValue() == 1);
        assertTrue(r.rank(2).intValue() == 2);
    }

    public void testRankingWithSameScores() {
        Ranking r = new Ranking(null);
        r.addSolution(new Rank(1, 0, 12));
        r.addSolution(new Rank(2, 0, 12));
        r.addSolution(new Rank(5, 0, 12));
        r.addSolution(new Rank(10, 0.2, 90));
        r.build();
        assertTrue(r.rank(10).intValue() == 1);
        assertTrue(r.rank(5).intValue() == 4);
        assertTrue(r.rank(2).intValue() == 3);
        assertTrue(r.rank(1).intValue() == 2);
    }

    public void testFilteredRanking() {
        KB kb = new KB();
        kb.add("Loane", "livesIn", "Rennes");
        kb.add("Paul", "livesIn", "Vitre");
        kb.add("Julianne", "livesIn", "Chateaugiron");
        kb.add("Gaelle", "livesIn", "Cesson-Sevigne");
        kb.add("Olivier", "livesIn", "Cesson-Sevigne");
        kb.add("Olivier", "bornIn", "Poitiers");
        Ranking r = new Ranking(new Query(kb, kb.map("Olivier"), kb.map("livesIn"), -1));
        r.addSolution(new Rank(kb.map("Rennes"), 0, 12));
        r.addSolution(new Rank(kb.map("Vitre"), 0, 12));
        r.addSolution(new Rank(kb.map("Chateaugiron"), 0, 12));
        r.addSolution(new Rank(kb.map("Poitiers"), 0.2, 90));
        r.addSolution(new Rank(kb.map("Cesson-Sevigne"), 0.3, 90));
        r.build();
        assertTrue(r.rank(kb.map("Cesson-Sevigne")).intValue() == 1);
        assertTrue(r.rank(kb.map("Poitiers")).intValue() == 2);
        assertTrue(r.filteredRank(kb.map("Poitiers")).intValue() == 1);
        List<Integer> ties = List.of(kb.map("Chateaugiron"), kb.map("Rennes"), kb.map("Vitre"));
        for (int i = 0; i < ties.size(); ++i) {
            assertTrue(r.filteredRank(ties.get(i)) < r.rank(ties.get(i)));
            for (int j = i + 1; j < ties.size(); ++j) {
                if (ties.get(i) > ties.get(j)) {
                    assertTrue(r.rank(ties.get(i)) > r.rank(ties.get(j)) );
                } else {
                    assertTrue(r.rank(ties.get(j)) > r.rank(ties.get(i)));
                }
            }
        }
    }
}
