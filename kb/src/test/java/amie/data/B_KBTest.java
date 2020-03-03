package amie.data;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
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
		kb.add(KB.triple("<Luis>", "<wasBornIn>", "<Guayaquil>"));
		kb.add(KB.triple("<Thomas>", "<wasBornIn>", "<Munich>"));
		kb.add(KB.triple("<Antoine>", "<wasBornIn>", "<Colmar>"));
		kb.add(KB.triple("<Oana>", "<livesIn>", "<Paris>"));
		kb.add(KB.triple("<Luis>", "<livesIn>", "<Paris>"));
		kb.add(KB.triple("<Thomas>", "<livesIn>", "<Paris>"));
		kb.add(KB.triple("<Antoine>", "<livesIn>", "<Paris>"));
		kb.add(KB.triple("<Ambar>", "<worksAt>", "<ESPOL>"));
		kb.add(KB.triple("<Luis>", "<worksAt>", "<Telecom>"));
		kb.add(KB.triple("<Thomas>", "<worksAt>", "<Telecom>"));
		kb.add(KB.triple("<Antoine>", "<worksAt>", "<Telecom>"));		
		kb.add(KB.triple("<Telecom>", "<isLocatedIn>", "<Paris>"));
	}
	
	public void testNegationCountOneVar() {
		assert(kb.count(KB.triple("<hasChild>", KB.NOTEXISTSstr, "?x")) == 6);
		assert(kb.count(KB.triple("<hasChild>", KB.NOTEXISTSINVstr, "?x")) == 6);
	}
	
	public void testContains() {
		assertFalse(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Antoine>")));
		assertTrue(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Ambar>")));
		assertTrue(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSINVstr, "<Paris>")));
		assertTrue(kb.contains(KB.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Munich>")));
		assertTrue(kb.contains(KB.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Cuenca>")));
	}
	
	public void testValuesOneVar1() {
		IntSet values = kb.resultsOneVariable(KB.triple("<worksAt>", KB.NOTEXISTSstr, "?x"));
		assertEquals(2, values.size());
		assertTrue(values.contains(KB.map("<Oana>")));
		assertTrue(values.contains(KB.map("<Telecom>")));
	}
	
	public void testValuesOneVar2() {
		IntSet values = kb.resultsOneVariable(KB.triple("?x", KB.NOTEXISTSstr, "<Luis>"));
		assertEquals(1, values.size());
		assertTrue(values.contains(KB.map("<isLocatedIn>")));
	}
	
	public void testValuesOneVar3() {
		kb.add(KB.triple("<Guayaquil>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Munich>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Colmar>", "<rdf:type>", "<City>"));		
		kb.add(KB.triple("<Paris>", "<rdf:type>", "<City>"));		
		IntSet values = kb.selectDistinct(KB.map("?x"), 
				KB.triples(KB.triple("<wasBornIn>", KB.NOTEXISTSINVstr, "?x"),
						KB.triple("?x", "<rdf:type>", "<City>")));
		assertEquals(1, values.size());
		assertTrue(values.contains(KB.map("<Paris>")));
	}
	
	public void testValuesTwoVars() {
		kb.add(KB.triple("<Guayaquil>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Munich>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Colmar>", "<rdf:type>", "<City>"));		
		kb.add(KB.triple("<Paris>", "<rdf:type>", "<City>"));	
		kb.add(KB.triple("<Luis>", "<rdf:type>", "<Person>"));
		kb.add(KB.triple("<Antoine>", "<rdf:type>", "<Person>"));
		kb.add(KB.triple("<Ambar>", "<rdf:type>", "<Person>"));		
		kb.add(KB.triple("<Oana>", "<rdf:type>", "<Person>"));	
		kb.add(KB.triple("<Thomas>", "<rdf:type>", "<Person>"));
		Int2ObjectMap<IntSet> values = kb.selectDistinct(KB.map("?x"), KB.map("?y"),
				KB.triples(KB.triple("?x", KB.NOTEXISTSstr, "?y"),
						KB.triple("?y", "<rdf:type>", "<Person>")));
		assertTrue(values.containsKey(KB.map("<wasBornIn>")));
		assertTrue(values.get(KB.map("<wasBornIn>")).contains(KB.map("<Ambar>")));
		
		values = kb.selectDistinct(KB.map("?x"), KB.map("?y"),
				KB.triples(KB.triple("?x", KB.NOTEXISTSINVstr, "?y"),
						KB.triple("?y", "<rdf:type>", "<City>")));
		assertTrue(values.containsKey(KB.map("<wasBornIn>")));
		assertTrue(values.get(KB.map("<wasBornIn>")).contains(KB.map("<Paris>")));
	}
        
        public void testSelectIterator() {
            assertEquals(12, kb.size());
            kb.add(KB.triple("<Jonathan>", "<livesIn>", "<Villejuif>"));
            kb.add(KB.triple("<Jonathan>", "<worksAt>", "<Telecom>"));
            kb.add(KB.triple("<Villejuif>", "<isLocatedIn>", "<France>"));
            kb.add(KB.triple("<Luis>", "<worksAt>", "<INRIA>"));
            kb.add(KB.triple("<INRIA>", "<isLocatedIn>", "<Paris>"));
            List<int[]> query = KB.triples(
                    KB.triple("?x", "<worksAt>", "?t"),
                    KB.triple("?t", "<isLocatedIn>", "?c"),
                    KB.triple("?x", "<livesIn>", "?c"));
            IntSet values = kb.selectDistinct(KB.map("?x"), query);
            assertEquals(3, values.size());
            assertTrue(values.contains(KB.map("<Thomas>")));
            assertTrue(values.contains(KB.map("<Antoine>")));
            assertTrue(values.contains(KB.map("<Luis>")));
            IntSet result = new IntOpenHashSet();
            IntSet resultIterator = new IntOpenHashSet();
            for (IntIterator it = kb.selectDistinctIterator(result, KB.map("?x"), query); it.hasNext(); ) {
                int e = it.nextInt();
                assertFalse(resultIterator.contains(e));
                resultIterator.add(e);
            }
            assertEquals(result, values);
            assertEquals(result, resultIterator);
        }
        
        public void testSelectMappings() {
            assertEquals(12, kb.size());
            kb.add(KB.triple("<Jonathan>", "<livesIn>", "<Villejuif>"));
            kb.add(KB.triple("<Jonathan>", "<worksAt>", "<Telecom>"));
            kb.add(KB.triple("<Villejuif>", "<isLocatedIn>", "<France>"));
            kb.add(KB.triple("<Luis>", "<worksAt>", "<INRIA>"));
            kb.add(KB.triple("<INRIA>", "<isLocatedIn>", "<Paris>"));
            assertEquals(4, kb.countMappings(KB.triples(KB.triple("?x", "<worksAt>", "<Telecom>"))));
            assertEquals(6, kb.countMappings(KB.triples(KB.triple("?x", "<worksAt>", "?y"))));
            assertEquals(5, kb.countMappings(
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z"))));
            assertEquals(4, kb.countMappings(
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?x", "<wasBornIn>", "?z"))));
            Int2LongMap r = kb.selectDistinctMappings(KB.map("?x"), KB.triples(KB.triple("?x", "<worksAt>", "<Telecom>")));
            assertEquals(4, r.size());
            assertTrue(r.containsKey(KB.map("<Luis>")));
            assertEquals(1, r.get(KB.map("<Luis>")));
            r = kb.selectDistinctMappings(KB.map("?y"), KB.triples(KB.triple("?x", "<worksAt>", "?y")));
            assertEquals(3, r.size());
            assertEquals(4, r.get(KB.map("<Telecom>")));
            assertEquals(1, r.get(KB.map("<ESPOL>")));
            assertEquals(1, r.get(KB.map("<INRIA>")));
            r = kb.selectDistinctMappings(KB.map("?y"), 
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z")));
            assertEquals(2, r.size());
            assertEquals(4, r.get(KB.map("<Telecom>")));
            assertEquals(1, r.get(KB.map("<INRIA>")));
            r = kb.selectDistinctMappings(KB.map("?z"), 
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z")));
            assertEquals(1, r.size());
            assertEquals(5, r.get(KB.map("<Paris>")));
            r = kb.selectDistinctMappings(KB.map("?x"), 
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z")));
            assertEquals(4, r.size());
            assertEquals(2, r.get(KB.map("<Luis>")));
            assertEquals(1, r.get(KB.map("<Jonathan>")));
            Int2ObjectMap<Int2LongMap> r2 = kb.selectDistinctMappings(KB.map("?x"), KB.map("?z"),
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z")));
            assertEquals(4, r2.size());
            assertEquals(1, r2.get(KB.map("<Luis>")).size());
            assertEquals(2, r2.get(KB.map("<Luis>")).get(KB.map("<Paris>")));
            assertEquals(1, r2.get(KB.map("<Jonathan>")).size());
            assertEquals(1, r2.get(KB.map("<Jonathan>")).get(KB.map("<Paris>")));
            r2 = kb.selectDistinctMappings(KB.map("?x"), KB.map("?y"),
                    KB.triples(KB.triple("?x", "<worksAt>", "?y"),
                            KB.triple("?y", "<isLocatedIn>", "?z")));
            assertEquals(4, r2.size());
            assertEquals(2, r2.get(KB.map("<Luis>")).size());
            assertEquals(1, r2.get(KB.map("<Luis>")).get(KB.map("<Telecom>")));
            assertEquals(1, r2.get(KB.map("<Luis>")).get(KB.map("<INRIA>")));
            assertEquals(1, r2.get(KB.map("<Jonathan>")).size());
            assertEquals(1, r2.get(KB.map("<Jonathan>")).get(KB.map("<Telecom>")));
        }
        
        public void testConnectedComponent() {
            int[] atom1, atom2, atom3;
            atom1 = KB.triple("?x", "a", "?y");
            atom2 = KB.triple("?x", "b", "?z");
            atom3 = KB.triple("?y", "c", "E");
            
            List<int[]> query = KB.triples(atom1, atom2, atom3);
            assertEquals(KB.connectedComponent(query, KB.map("?y"), KB.map("?x")),
                    KB.triples(atom1, atom3));
            assertEquals(KB.connectedComponent(query, KB.map("?y"), KB.map("?z")),
                    KB.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, KB.map("?x"), KB.map("?y")),
                    KB.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, KB.map("?x"), KB.map("?z")),
                    KB.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, KB.map("?z"), KB.map("?x")),
                    KB.triples(atom2));
            assertEquals(KB.connectedComponent(query, KB.map("?z"), KB.map("?y")),
                    KB.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, KB.map("?w"), KB.map("a")),
                    KB.triples());
            assertEquals(KB.connectedComponent(query, KB.map("?x"), KB.map("?w")),
                    KB.triples(atom1, atom2, atom3));
        }

}
