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
            assertTrue(Schema.COMPOSE_SIZE >= 15);
        }
        
        public void testMapEntity() {
            int j = kb.map("<Jonathan>");
            int l = kb.map("<Luis>");
            assertTrue(j != l);
            assertTrue(j > 0);
            assertTrue(l > 0);
            assertEquals("<Jonathan>", kb.unmap(j));
            assertEquals("<Luis>", kb.unmap(l));
            assertTrue(j == kb.map("<Jonathan>"));
            assertTrue(l == kb.map("<Luis>"));
        }
        
        public void testMapVariable() {
            int x = kb.map("?x");
            System.err.println("?x -mapTo-> " + x);
            System.err.println("?x -unmapTo-> " + kb.unmap(x));
            int l = kb.map("?a86");
            System.err.println("?a86 -mapTo-> " + l);
            System.err.println("?a86 -unmapTo-> " + kb.unmap(l));
            int nox = kb.map("?_x1");
            System.err.println("?_x1 -mapTo-> " + nox);
            System.err.println("?_x1 -unmapTo-> " + kb.unmap(nox));
            assertTrue(x != nox);
            assertTrue(x != l);
            assertTrue(l != nox);
            assertTrue(KB.isVariable(x));
            assertTrue(KB.isVariable(l));
            assertTrue(KB.isVariable(nox));
            assertTrue(Schema.isOpenableVariable(x));
            assertTrue(Schema.isOpenableVariable(l));
            assertFalse(Schema.isOpenableVariable(nox));
            assertEquals("?x", kb.unmap(x));
            assertEquals("?a86", kb.unmap(l));
            assertEquals("?_x1", kb.unmap(nox));
            assertTrue(x == kb.map("?x"));
            assertTrue(l == kb.map("?a86"));
            assertTrue(nox == kb.map("?_x1"));
        }
        
        public void testMapComposite() {
            int j = kb.schema.mapComposite("Jonathan");
            int j1 = kb.schema.mapComposite("Jonathan", 1);
            int j1b = Schema.compose(j, 1);
            int l2 = kb.schema.mapComposite("Luis", 2);
            int l3 = kb.schema.mapComposite("Luis", 3);
            int l = kb.map("Luis");
            assertTrue(Schema.isComposite(j));
            assertTrue(Schema.isComposite(j1));
            assertTrue(j1 == j1b);
            assertTrue(Schema.isComposite(l2));
            assertTrue(Schema.isComposite(l3));
            assertTrue(Schema.isComposite(l));
            assertTrue(Schema.uncompose(j).first == j);
            assertTrue(Schema.uncompose(j).second == 0);
            assertTrue(Schema.uncompose(l).first == l);
            assertTrue(Schema.uncompose(l).second == 0);
            assertTrue(Schema.uncompose(j1).first == j);
            assertTrue(Schema.uncompose(j1).second == 1);
            assertTrue(Schema.uncompose(l2).first == l);
            assertTrue(Schema.uncompose(l2).second == 2);
            assertTrue(Schema.uncompose(l3).first == l);
            assertTrue(Schema.uncompose(l3).second == 3);
        }
        
                
        public void testNullEntity() {
            assertEquals("null", kb.unmap(0));
        }
}
