/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.linkprediction;

import junit.framework.TestCase;

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
        assertTrue(r.rank(5).intValue() == 2);
        assertTrue(r.rank(2).intValue() == 2);
        assertTrue(r.rank(1).intValue() == 2);
    }
}
