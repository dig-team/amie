package amie.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.List;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;
import junit.framework.TestCase;

public class KBTest extends TestCase {
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
		assert(kb.count(KB.triple("<hasChild>", KB.NOTEXISTSINVbs, "?x")) == 6);
	}
	
	public void testContains() {
		assertFalse(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Antoine>")));
		assertTrue(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSstr, "<Ambar>")));
		assertTrue(kb.contains(KB.triple("<wasBornIn>", KB.NOTEXISTSINVstr, "<Paris>")));
		assertTrue(kb.contains(KB.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Munich>")));
		assertTrue(kb.contains(KB.triple("<locatedIn>", KB.NOTEXISTSINVstr, "<Cuenca>")));
	}
	
	public void testValuesOneVar1() {
		IntSet values = kb.resultsOneVariable(KB.triple("<worksAt>", KB.NOTEXISTSbs, "?x"));
		assertEquals(2, values.size());
		assertTrue(values.contains(ByteString.of("<Oana>")));
		assertTrue(values.contains(ByteString.of("<Telecom>")));
	}
	
	public void testValuesOneVar2() {
		IntSet values = kb.resultsOneVariable(KB.triple("?x", KB.NOTEXISTSbs, "<Luis>"));
		assertEquals(1, values.size());
		assertTrue(values.contains(ByteString.of("<isLocatedIn>")));
	}
	
	public void testValuesOneVar3() {
		kb.add(KB.triple("<Guayaquil>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Munich>", "<rdf:type>", "<City>"));
		kb.add(KB.triple("<Colmar>", "<rdf:type>", "<City>"));		
		kb.add(KB.triple("<Paris>", "<rdf:type>", "<City>"));		
		IntSet values = kb.selectDistinct(ByteString.of("?x"), 
				KB.triples(KB.triple("<wasBornIn>", KB.NOTEXISTSINVbs, "?x"),
						KB.triple("?x", "<rdf:type>", "<City>")));
		assertEquals(1, values.size());
		assertTrue(values.contains(ByteString.of("<Paris>")));
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
		Int2ObjectMap<IntSet> values = kb.selectDistinct(ByteString.of("?x"), ByteString.of("?y"),
				KB.triples(KB.triple("?x", KB.NOTEXISTSbs, "?y"),
						KB.triple("?y", "<rdf:type>", "<Person>")));
		assertTrue(values.containsKey(ByteString.of("<wasBornIn>")));
		assertTrue(values.get(ByteString.of("<wasBornIn>")).contains(ByteString.of("<Ambar>")));
		
		values = kb.selectDistinct(ByteString.of("?x"), ByteString.of("?y"),
				KB.triples(KB.triple("?x", KB.NOTEXISTSINVbs, "?y"),
						KB.triple("?y", "<rdf:type>", "<City>")));
		assertTrue(values.containsKey(ByteString.of("<wasBornIn>")));
		assertTrue(values.get(ByteString.of("<wasBornIn>")).contains(ByteString.of("<Paris>")));
	}
        
        public void testSelectIterator() {
            assertEquals(12, kb.size());
            kb.add(KB.triple("<Jonathan>", "<livesIn>", "<Villejuif>"));
            kb.add(KB.triple("<Jonathan>", "<worksAt>", "<Telecom>"));
            kb.add(KB.triple("<Villejuif>", "<isLocatedIn>", "<France>"));
            kb.add(KB.triple("<Luis>", "<worksAt>", "<INRIA>"));
            kb.add(KB.triple("<INRIA>", "<isLocatedIn>", "<Paris>"));
            List<ByteString[]> query = KB.triples(
                    KB.triple("?x", "<worksAt>", "?t"),
                    KB.triple("?t", "<isLocatedIn>", "?c"),
                    KB.triple("?x", "<livesIn>", "?c"));
            IntSet values = kb.selectDistinct(ByteString.of("?x"), query);
            assertEquals(3, values.size());
            assertTrue(values.contains(ByteString.of("<Thomas>")));
            assertTrue(values.contains(ByteString.of("<Antoine>")));
            assertTrue(values.contains(ByteString.of("<Luis>")));
            IntSet result = new IntOpenHashSet();
            IntSet resultIterator = new IntOpenHashSet();
            for (Iterator<ByteString> it = kb.selectDistinctIterator(result, ByteString.of("?x"), query); it.hasNext(); ) {
                ByteString e = it.next();
                assertFalse(resultIterator.contains(e));
                resultIterator.add(e);
            }
            assertEquals(result, values);
            assertEquals(result, resultIterator);
        }
        
        public void testConnectedComponent() {
            ByteString[] atom1, atom2, atom3;
            atom1 = KB.triple("?x", "a", "?y");
            atom2 = KB.triple("?x", "b", "?z");
            atom3 = KB.triple("?y", "c", "E");
            
            List<ByteString[]> query = KB.triples(atom1, atom2, atom3);
            assertEquals(KB.connectedComponent(query, ByteString.of("?y"), ByteString.of("?x")),
                    KB.triples(atom1, atom3));
            assertEquals(KB.connectedComponent(query, ByteString.of("?y"), ByteString.of("?z")),
                    KB.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, ByteString.of("?x"), ByteString.of("?y")),
                    KB.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, ByteString.of("?x"), ByteString.of("?z")),
                    KB.triples(atom1, atom2, atom3));
            assertEquals(KB.connectedComponent(query, ByteString.of("?z"), ByteString.of("?x")),
                    KB.triples(atom2));
            assertEquals(KB.connectedComponent(query, ByteString.of("?z"), ByteString.of("?y")),
                    KB.triples(atom1, atom2));
            assertEquals(KB.connectedComponent(query, ByteString.of("?w"), ByteString.of("a")),
                    KB.triples());
            assertEquals(KB.connectedComponent(query, ByteString.of("?x"), ByteString.of("?w")),
                    KB.triples(atom1, atom2, atom3));
        }

}
