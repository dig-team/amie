package amie.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;

import junit.framework.TestCase;

public class B_KBTest extends TestCase {
	KB kb = new KB();
	
	protected void setUp() throws Exception {
		super.setUp();
		kb.add(kb.triple("<Luis>", "<wasBornIn>", "<Guayaquil>"));
		kb.add(kb.triple("<Thomas>", "<wasBornIn>", "<Munich>"));
		kb.add(kb.triple("<Antoine>", "<wasBornIn>", "<Colmar>"));
		kb.add(kb.triple("<Oana>", "<livesIn>", "<Paris>"));
		kb.add(kb.triple("<Luis>", "<livesIn>", "<Paris>"));
		kb.add(kb.triple("<Thomas>", "<livesIn>", "<Paris>"));
		kb.add(kb.triple("<Antoine>", "<livesIn>", "<Paris>"));
		kb.add(kb.triple("<Ambar>", "<worksAt>", "<ESPOL>"));
		kb.add(kb.triple("<Luis>", "<worksAt>", "<Telecom>"));
		kb.add(kb.triple("<Thomas>", "<worksAt>", "<Telecom>"));
		kb.add(kb.triple("<Antoine>", "<worksAt>", "<Telecom>"));		
		kb.add(kb.triple("<Telecom>", "<isLocatedIn>", "<Paris>"));
	}
	
	public void testNegationCountOneVar() {
		assert(kb.count(kb.triple("<hasChild>", KB.NOTEXISTSstr, "?x")) == 6);
		assert(kb.count(kb.triple("<hasChild>", KB.NOTEXISTSINVstr, "?x")) == 6);
	}
	
	public void testContains() {
		assertFalse(kb.contains(kb.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Antoine>")));
		assertTrue(kb.contains(kb.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Ambar>")));
		assertTrue(kb.contains(kb.triple("<wasBornIn>", KB.NOTEXISTSINVstr, "<Paris>")));
		assertTrue(kb.contains(kb.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Munich>")));
		assertTrue(kb.contains(kb.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Cuenca>")));
	}
	
	public void testValuesOneVar1() {
		IntSet values = kb.resultsOneVariable(kb.triple("<worksAt>", KB.NOTEXISTSstr, "?x"));
		assertEquals(2, values.size());
		assertTrue(values.contains(kb.map("<Oana>")));
		assertTrue(values.contains(kb.map("<Telecom>")));
	}
	
	public void testValuesOneVar2() {
		IntSet values = kb.resultsOneVariable(kb.triple("?x", KB.NOTEXISTSstr, "<Luis>"));
		assertEquals(1, values.size());
		assertTrue(values.contains(kb.map("<isLocatedIn>")));
	}
	
	public void testValuesOneVar3() {
		kb.add(kb.triple("<Guayaquil>", "<rdf:type>", "<City>"));
		kb.add(kb.triple("<Munich>", "<rdf:type>", "<City>"));
		kb.add(kb.triple("<Colmar>", "<rdf:type>", "<City>"));		
		kb.add(kb.triple("<Paris>", "<rdf:type>", "<City>"));		
		IntSet values = kb.selectDistinct(kb.map("?x"), 
				kb.triples(kb.triple("<wasBornIn>", KB.NOTEXISTSINVstr, "?x"),
						kb.triple("?x", "<rdf:type>", "<City>")));
		assertEquals(1, values.size());
		assertTrue(values.contains(kb.map("<Paris>")));
	}
	
	public void testValuesTwoVars() {
		kb.add(kb.triple("<Guayaquil>", "<rdf:type>", "<City>"));
		kb.add(kb.triple("<Munich>", "<rdf:type>", "<City>"));
		kb.add(kb.triple("<Colmar>", "<rdf:type>", "<City>"));		
		kb.add(kb.triple("<Paris>", "<rdf:type>", "<City>"));	
		kb.add(kb.triple("<Luis>", "<rdf:type>", "<Person>"));
		kb.add(kb.triple("<Antoine>", "<rdf:type>", "<Person>"));
		kb.add(kb.triple("<Ambar>", "<rdf:type>", "<Person>"));		
		kb.add(kb.triple("<Oana>", "<rdf:type>", "<Person>"));	
		kb.add(kb.triple("<Thomas>", "<rdf:type>", "<Person>"));
		Int2ObjectMap<IntSet> values = kb.selectDistinct(kb.map("?x"), kb.map("?y"),
				kb.triples(kb.triple("?x", KB.NOTEXISTSstr, "?y"),
						kb.triple("?y", "<rdf:type>", "<Person>")));
		assertTrue(values.containsKey(kb.map("<wasBornIn>")));
		assertTrue(values.get(kb.map("<wasBornIn>")).contains(kb.map("<Ambar>")));
		
		values = kb.selectDistinct(kb.map("?x"), kb.map("?y"),
				kb.triples(kb.triple("?x", KB.NOTEXISTSINVstr, "?y"),
						kb.triple("?y", "<rdf:type>", "<City>")));
		assertTrue(values.containsKey(kb.map("<wasBornIn>")));
		assertTrue(values.get(kb.map("<wasBornIn>")).contains(kb.map("<Paris>")));
	}
        
        public void testSelectIterator() {
            assertEquals(12, kb.size());
            kb.add(kb.triple("<Jonathan>", "<livesIn>", "<Villejuif>"));
            kb.add(kb.triple("<Jonathan>", "<worksAt>", "<Telecom>"));
            kb.add(kb.triple("<Villejuif>", "<isLocatedIn>", "<France>"));
            kb.add(kb.triple("<Luis>", "<worksAt>", "<INRIA>"));
            kb.add(kb.triple("<INRIA>", "<isLocatedIn>", "<Paris>"));
            List<int[]> query = kb.triples(
                    kb.triple("?x", "<worksAt>", "?t"),
                    kb.triple("?t", "<isLocatedIn>", "?c"),
                    kb.triple("?x", "<livesIn>", "?c"));
            IntSet values = kb.selectDistinct(kb.map("?x"), query);
            assertEquals(3, values.size());
            assertTrue(values.contains(kb.map("<Thomas>")));
            assertTrue(values.contains(kb.map("<Antoine>")));
            assertTrue(values.contains(kb.map("<Luis>")));
            IntSet result = new IntOpenHashSet();
            IntSet resultIterator = new IntOpenHashSet();
            for (IntIterator it = kb.selectDistinctIterator(result, kb.map("?x"), query); it.hasNext(); ) {
                int e = it.nextInt();
                assertFalse(resultIterator.contains(e));
                resultIterator.add(e);
            }
            assertEquals(result, values);
            assertEquals(result, resultIterator);
        }
        
        public void testConnectedComponent() {
            int[] atom1, atom2, atom3;
            atom1 = kb.triple("?x", "a", "?y");
            atom2 = kb.triple("?x", "b", "?z");
            atom3 = kb.triple("?y", "c", "E");
            
            List<int[]> query = kb.triples(atom1, atom2, atom3);
            assertEquals(KB.connectedComponent(query, kb.map("?y"), kb.map("?x")),
                    kb.triples(atom1, atom3));
            assertEquals(KB.connectedComponent(query, kb.map("?y"), kb.map("?z")),
                    kb.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, kb.map("?x"), kb.map("?y")),
                    kb.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, kb.map("?x"), kb.map("?z")),
                    kb.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, kb.map("?z"), kb.map("?x")),
                    kb.triples(atom2));
            assertEquals(KB.connectedComponent(query, kb.map("?z"), kb.map("?y")),
                    kb.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, kb.map("?w"), kb.map("a")),
                    kb.triples());
            assertEquals(KB.connectedComponent(query, kb.map("?x"), kb.map("?w")),
                    kb.triples(atom1, atom2, atom3));
        }

}
