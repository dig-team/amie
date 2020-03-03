/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.rules.Rule;
import junit.framework.TestCase;

/**
 * GRank test on two toys examples.
 * 
 * We will create two KB and rules in order to test GRank such as:
 * * Q sets will be of size 1, as dMap is only the average of dAP over the 
 * different elements of the Q set, we only really need to test dAP.
 * * In order to test dAP we will make sure the rule generates 5 
 * predictions.
 * Case 1: Distinct rank for each predictions, 1st, 3rd and 4th predictions in the KB:
 * Predictions  Score   Inside KB
 *  y_1         5       Yes
 *  y_2         4       No
 *  y_3         3       Yes
 *  y_4         2       Yes
 *  y_5         1       No
 * Expected dPreK: 1/1; 1/2; 2/3; 3/4; 3/5.
 * Expected dAP: (1 + 2/3 + 3/4) / 3 (to be verified)
 * 
 * Case 2: Predictions with same rank, distribution 2-2-1 such that:
 * Predictions  Score   Inside KB
 *  y_1         3       Yes
 *  y_2         3       No
 *  y_3         2       No
 *  y_4         2       No
 *  y_5         1       Yes
 * Expected dPreK: 0.5/1; 1/2; 1/3; 1/4; 2/5.
 * Expected dAP: (0.5*0.5 + 1/2*0.5 + 2/5) / 2 (to be verified)
 * 
 * @author jlajus
 */
public class GRankTest extends TestCase {
    	
    KB kb1 = new KB();
    KB kb2 = new KB();
    GRank gr1;
    GRank gr2;
    Rule r1, r2;
	
    protected int entityCount = 0;
    protected int newEntity() {
        return KB.map("e" + Integer.toString(entityCount++));
    }
   
    protected void populateKB(KB kb, int x, int y, int score, boolean inKB) {
        int ne;
        for(int i = 0; i < score; i++) {
            ne = newEntity();
            kb.add(KB.triple(x, KB.map("r1"), ne));
            kb.add(KB.triple(ne, KB.map("r2"), y));
        }
        if (inKB) {
            kb.add(KB.triple(x, KB.map("h"), y));
            kb.add(KB.triple(y, KB.map("hm"), x));
        }
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        int x = KB.map("x");
        populateKB(kb1, x, KB.map("y1"), 5, true);
        populateKB(kb1, x, KB.map("y2"), 4, false);
        populateKB(kb1, x, KB.map("y3"), 3, true);
        populateKB(kb1, x, KB.map("y4"), 2, true);
        populateKB(kb1, x, KB.map("y5"), 1, false);
        gr1 = new GRank(kb1);
        
        populateKB(kb2, x, KB.map("y1"), 3, true);
        populateKB(kb2, x, KB.map("y2"), 3, false);
        populateKB(kb2, x, KB.map("y3"), 2, false);
        populateKB(kb2, x, KB.map("y4"), 2, false);
        populateKB(kb2, x, KB.map("y5"), 1, true);
        gr2 = new GRank(kb2);
        
        r1 = new Rule(KB.triple(KB.map("?x"), KB.map("h"), KB.map("?y")),
                        KB.triples(
                            KB.triple(KB.map("?x"), KB.map("r1"), KB.map("?e")),
                            KB.triple(KB.map("?e"), KB.map("r2"), KB.map("?y"))), 0);
        
        r2 = new Rule(KB.triple(KB.map("?y"), KB.map("hm"), KB.map("?x")),
                        KB.triples(
                            KB.triple(KB.map("?x"), KB.map("r1"), KB.map("?e")),
                            KB.triple(KB.map("?e"), KB.map("r2"), KB.map("?y"))), 0);
    }
    
    public void testDifferentRanks() {
        gr1.computeDMaps(r1);
        gr1.computeDMaps(r2);
        assertEquals((1.0 + 2.0/3 + 3.0/4) / 3, r1.getMeasure("GRank_map_tail"), 0.0001);
        assertEquals((1.0 + 2.0/3 + 3.0/4) / 3, r2.getMeasure("GRank_map_head"), 0.0001);
    }
    
    public void testDistributedRanks() {
        gr2.computeDMaps(r1);
        gr2.computeDMaps(r2);
        assertEquals((0.5*0.5 + 0.5*0.5 + 2.0/5) / 2, r1.getMeasure("GRank_map_tail"), 0.0001);
        assertEquals((0.5*0.5 + 0.5*0.5 + 2.0/5) / 2, r2.getMeasure("GRank_map_head"), 0.0001);
    }
}
