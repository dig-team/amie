package amie.rules;

import javatools.datatypes.ByteString;
import junit.framework.TestCase;
import amie.data.KB;
import amie.rules.Rule;

public class RedundantSubgraphsTest extends TestCase {

	Rule q1, q2, q3, q4, q5, q6, q7, q8;
	
	public RedundantSubgraphsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		q1 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q1 = q1.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?b")), 0);
		q1 = q1.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?e")), 0, ByteString.of("?a"), ByteString.of("?e"));				
		q1 = q1.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?e")), 0);
		q1 = q1.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?f")), 0, ByteString.of("?a"), ByteString.of("?f"));				
		q1 = q1.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?f")), 0);				
		
		q2 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?x")), 0);
		q2 = q2.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?m")), 0, ByteString.of("?a"), ByteString.of("?m"));
		q2 = q2.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?x")), 0);				
		q2 = q2.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?m")), 0);

		q3 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?x")), 0);
		q3 = q3.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?m")), 0, ByteString.of("?a"), ByteString.of("?m"));
		q3 = q3.addAtom(KB.triple(ByteString.of("?x"), ByteString.of("r3"), ByteString.of("?m")), 0);
		
		q4 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q4 = q4.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?b")), 0);
		q4 = q4.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?e")), 0, ByteString.of("?a"), ByteString.of("?e"));				
		q4 = q4.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?e")), 0);
		q4 = q4.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?f")), 0, ByteString.of("?a"), ByteString.of("?f"));				
		q4 = q4.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r3"), ByteString.of("?f")), 0);		
		
		q5 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q5 = q5.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?c")), 0, ByteString.of("?a"), ByteString.of("?c"));				
		q5 = q5.addAtom(KB.triple(ByteString.of("?d"), ByteString.of("r1"), ByteString.of("?b")), 0, ByteString.of("?b"), ByteString.of("?d"));				
		q5 = q5.addAtom(KB.triple(ByteString.of("?d"), ByteString.of("r2"), ByteString.of("?c")), 0);
		
		q6 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q6 = q6.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?x")), 0, ByteString.of("?a"), ByteString.of("?x"));				
		q6 = q6.addAtom(KB.triple(ByteString.of("?b"), ByteString.of("r2"), ByteString.of("?a")), 0);				
		q6 = q6.addAtom(KB.triple(ByteString.of("?x"), ByteString.of("r2"), ByteString.of("?a")), 0);				
		
		q7 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q7 = q7.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?x")), 0, ByteString.of("?a"), ByteString.of("?x"));				
		q7 = q7.addAtom(KB.triple(ByteString.of("?b"), ByteString.of("r2"), ByteString.of("?a")), 0);				
		q7 = q7.addAtom(KB.triple(ByteString.of("?a"), ByteString.of("r2"), ByteString.of("?x")), 0);				

		q8 = new Rule(KB.triple(ByteString.of("?a"), ByteString.of("r1"), ByteString.of("?b")), 0);
		q8 = q8.addAtom(KB.triple(ByteString.of("?b"), ByteString.of("r2"), ByteString.of("?x")), 0, ByteString.of("?b"), ByteString.of("?x"));				
		q8 = q8.addAtom(KB.triple(ByteString.of("?x"), ByteString.of("r1"), ByteString.of("?c")), 0, ByteString.of("?x"), ByteString.of("?c"));				
		q8 = q8.addAtom(KB.triple(ByteString.of("?c"), ByteString.of("r2"), ByteString.of("?a")), 0);	
		
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void test1(){
		assertTrue(q1.containsDisallowedDiamond());
	}
	
	public void test2(){
		assertTrue(q2.containsDisallowedDiamond());
	}
	
	public void test3(){
		assertFalse(q3.containsDisallowedDiamond());
	}
	
	public void test4(){
		assertFalse(q4.containsDisallowedDiamond());
	}
	
	public void test5(){
		assertTrue(q5.containsDisallowedDiamond());
	}
	
	public void test6(){
		assertTrue(q6.containsDisallowedDiamond());
	}
	
	public void test7(){
		assertFalse(q7.containsDisallowedDiamond());
	}
	
	public void test8(){
		assertFalse(q8.containsDisallowedDiamond());
	}
}
