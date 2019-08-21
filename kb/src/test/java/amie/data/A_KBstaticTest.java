/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import junit.framework.TestCase;

/**
 *
 * @author jlajus
 */
public class A_KBstaticTest extends TestCase {
	KB kb = new KB();
	
	protected void setUp() throws Exception {
		super.setUp();
        }
        
        public void testComposeSize() {
            assertTrue(KB.COMPOSE_SIZE >= 15);
        }
        
        public void testMapEntity() {
            int j = KB.map("<Jonathan>");
            int l = KB.map("<Luis>");
            assertTrue(j != l);
            assertTrue(j > 0);
            assertTrue(l > 0);
            assertEquals("<Jonathan>", KB.unmap(j));
            assertEquals("<Luis>", KB.unmap(l));
            assertTrue(j == KB.map("<Jonathan>"));
            assertTrue(l == KB.map("<Luis>"));
        }
        
        public void testMapVariable() {
            int x = KB.map("?x");
            System.err.println("?x -mapTo-> " + x);
            System.err.println("?x -unmapTo-> " + KB.unmap(x));
            int l = KB.map("?a86");
            System.err.println("?a86 -mapTo-> " + l);
            System.err.println("?a86 -unmapTo-> " + KB.unmap(l));
            int nox = KB.map("?_x1");
            System.err.println("?_x1 -mapTo-> " + nox);
            System.err.println("?_x1 -unmapTo-> " + KB.unmap(nox));
            assertTrue(x != nox);
            assertTrue(x != l);
            assertTrue(l != nox);
            assertTrue(KB.isVariable(x));
            assertTrue(KB.isVariable(l));
            assertTrue(KB.isVariable(nox));
            assertTrue(KB.isOpenableVariable(x));
            assertTrue(KB.isOpenableVariable(l));
            assertFalse(KB.isOpenableVariable(nox));
            assertEquals("?x", KB.unmap(x));
            assertEquals("?a86", KB.unmap(l));
            assertEquals("?_x1", KB.unmap(nox));
            assertTrue(x == KB.map("?x"));
            assertTrue(l == KB.map("?a86"));
            assertTrue(nox == KB.map("?_x1"));
        }
        
        public void testMapComposite() {
            int j = KB.mapComposite("Jonathan");
            int j1 = KB.mapComposite("Jonathan", 1);
            int j1b = KB.compose(j, 1);
            int l2 = KB.mapComposite("Luis", 2);
            int l3 = KB.mapComposite("Luis", 3);
            int l = KB.map("Luis");
            assertTrue(KB.isComposite(j));
            assertTrue(KB.isComposite(j1));
            assertTrue(j1 == j1b);
            assertTrue(KB.isComposite(l2));
            assertTrue(KB.isComposite(l3));
            assertTrue(KB.isComposite(l));
            assertTrue(KB.uncompose(j).first == j);
            assertTrue(KB.uncompose(j).second == 0);
            assertTrue(KB.uncompose(l).first == l);
            assertTrue(KB.uncompose(l).second == 0);
            assertTrue(KB.uncompose(j1).first == j);
            assertTrue(KB.uncompose(j1).second == 1);
            assertTrue(KB.uncompose(l2).first == l);
            assertTrue(KB.uncompose(l2).second == 2);
            assertTrue(KB.uncompose(l3).first == l);
            assertTrue(KB.uncompose(l3).second == 3);
        }
        
                
        public void testNullEntity() {
            assertEquals("null", KB.unmap(0));
        }
}
