package amie.rules;


import junit.framework.TestCase;
import amie.data.KB;

public class RedundantSubgraphsTest extends TestCase {

	Rule q1, q2, q3, q4, q5, q6, q7, q8;
	
	public RedundantSubgraphsTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		q1 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q1 = q1.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?b")), 0);
		q1 = q1.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?e")), 0, KB.map("?a"), KB.map("?e"));				
		q1 = q1.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?e")), 0);
		q1 = q1.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?f")), 0, KB.map("?a"), KB.map("?f"));				
		q1 = q1.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?f")), 0);				
		
		q2 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?x")), 0);
		q2 = q2.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?m")), 0, KB.map("?a"), KB.map("?m"));
		q2 = q2.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?x")), 0);				
		q2 = q2.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?m")), 0);

		q3 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?x")), 0);
		q3 = q3.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?m")), 0, KB.map("?a"), KB.map("?m"));
		q3 = q3.addAtom(KB.triple(KB.map("?x"), KB.map("r3"), KB.map("?m")), 0);
		
		q4 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q4 = q4.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?b")), 0);
		q4 = q4.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?e")), 0, KB.map("?a"), KB.map("?e"));				
		q4 = q4.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?e")), 0);
		q4 = q4.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?f")), 0, KB.map("?a"), KB.map("?f"));				
		q4 = q4.addAtom(KB.triple(KB.map("?a"), KB.map("r3"), KB.map("?f")), 0);		
		
		q5 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q5 = q5.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?c")), 0, KB.map("?a"), KB.map("?c"));				
		q5 = q5.addAtom(KB.triple(KB.map("?d"), KB.map("r1"), KB.map("?b")), 0, KB.map("?b"), KB.map("?d"));				
		q5 = q5.addAtom(KB.triple(KB.map("?d"), KB.map("r2"), KB.map("?c")), 0);
		
		q6 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q6 = q6.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?x")), 0, KB.map("?a"), KB.map("?x"));				
		q6 = q6.addAtom(KB.triple(KB.map("?b"), KB.map("r2"), KB.map("?a")), 0);				
		q6 = q6.addAtom(KB.triple(KB.map("?x"), KB.map("r2"), KB.map("?a")), 0);				
		
		q7 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q7 = q7.addAtom(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?x")), 0, KB.map("?a"), KB.map("?x"));				
		q7 = q7.addAtom(KB.triple(KB.map("?b"), KB.map("r2"), KB.map("?a")), 0);				
		q7 = q7.addAtom(KB.triple(KB.map("?a"), KB.map("r2"), KB.map("?x")), 0);				

		q8 = new Rule(KB.triple(KB.map("?a"), KB.map("r1"), KB.map("?b")), 0);
		q8 = q8.addAtom(KB.triple(KB.map("?b"), KB.map("r2"), KB.map("?x")), 0, KB.map("?b"), KB.map("?x"));				
		q8 = q8.addAtom(KB.triple(KB.map("?x"), KB.map("r1"), KB.map("?c")), 0, KB.map("?x"), KB.map("?c"));				
		q8 = q8.addAtom(KB.triple(KB.map("?c"), KB.map("r2"), KB.map("?a")), 0);	
		
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
