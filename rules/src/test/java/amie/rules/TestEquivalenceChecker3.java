package amie.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;


import amie.data.javatools.datatypes.Pair;
import junit.framework.TestCase;
import amie.data.KB;

public class TestEquivalenceChecker3 extends TestCase {
	
	List<Pair<List<int[]>, List<int[]>>> cases;
	KB kb = new KB() ; 
	
	public TestEquivalenceChecker3(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		cases = new ArrayList<>();
		
		//The same query, 1 triple pattern
		List<int[]> q01 = kb.triples(kb.triple("bob","loves","?x2"));
		List<int[]> q02 = kb.triples(kb.triple("bob","loves","?x2"));
		Pair<List<int[]>, List<int[]>> p0 = new Pair<>(q01, q02);
		cases.add(p0);
		
		//The same query, 2 triple patterns
		List<int[]> q11 = kb.triples(kb.triple("bob","loves","?x2"), kb.triple("?x2","livesIn","?x3"));
		List<int[]> q12 = kb.triples(kb.triple("bob","loves","?x2"), kb.triple("?x2","livesIn","?x3"));
		Pair<List<int[]>, List<int[]>> p1 = new Pair<>(q11, q12);
		cases.add(p1);
		
		//Two different queries, one triple pattern
		List<int[]> q21 = kb.triples(kb.triple("bob","loves","?x2"));
		List<int[]> q22 = kb.triples(kb.triple("?z","loves","?x"));
		Pair<List<int[]>, List<int[]>> p2 = new Pair<>(q21, q22);
		cases.add(p2);
		
		//Two different queries, 2 triple patterns
		List<int[]> q31 = kb.triples(kb.triple("bob","loves","?x2"), kb.triple("?x2","livesIn","?x3"));
		List<int[]> q32 = kb.triples(kb.triple("bob","loves","?x2"), kb.triple("?x2","isCitizenOf","?x3"));
		Pair<List<int[]>, List<int[]>> p3 = new Pair<>(q31, q32);
		cases.add(p3);

		//The same path query with items shuffled, 2 triple patterns
		List<int[]> q41 = kb.triples(kb.triple("bob","loves","?x2"), kb.triple("?x2","livesIn","?x3"));
		List<int[]> q42 = kb.triples(kb.triple("?y2","livesIn","?y1"), kb.triple("bob","loves","?y2"));
		Pair<List<int[]>, List<int[]>> p4 = new Pair<>(q41, q42);
		cases.add(p4);
		
		//The same star query with items shuffled, 3 triple patterns
		List<int[]> q51 = kb.triples(kb.triple("?s","loves","?x"), kb.triple("?s","knows","?y"), kb.triple("?y","livesIn","?x"));
		List<int[]> q52 = kb.triples(kb.triple("?s","knows","?y"), kb.triple("?s","loves","?x"), kb.triple("?y","livesIn","?x"));
		Pair<List<int[]>, List<int[]>> p5 = new Pair<>(q51, q52);
		cases.add(p5);
		
		//Two different star queries, 3 triple patterns
		List<int[]> q61 = kb.triples(kb.triple("?s","loves","?y"), kb.triple("?s","knows","?y"), kb.triple("?s","livesIn","?m"));
		List<int[]> q62 = kb.triples( kb.triple("?k","livesIn","?m"), kb.triple("?k","loves","?o"), kb.triple("?k","knows","?y"));
		Pair<List<int[]>, List<int[]>> p6 = new Pair<>(q61, q62);
		cases.add(p6);
		
		//The same cycle query, 3 triple patterns
		List<int[]> q71 = kb.triples(kb.triple("?s","isConnected","?o"), kb.triple("?x","isConnected","?s"), kb.triple("?o","isConnected","?x"));
		List<int[]> q72 = kb.triples(kb.triple("?m","isConnected","?s"), kb.triple("?r","isConnected","?m"), kb.triple("?s","isConnected","?r"));
		Pair<List<int[]>, List<int[]>> p7 = new Pair<>(q71, q72);
		cases.add(p7);

		//Different cycle queries, 3 triple patterns
		List<int[]> q81 = kb.triples(kb.triple("?s","isConnected","?o"), kb.triple("?x","isConnected","?s"), kb.triple("?o","isConnected","?x"));
		List<int[]> q82 = kb.triples(kb.triple("?m","isConnected","?s"), kb.triple("?r","isConnected","?m"), kb.triple("?s","isConnected","?r"));
		Pair<List<int[]>, List<int[]>> p8 = new Pair<>(q81, q82);
		cases.add(p8);
		
		//Close star items shuffled, 3 triple patterns
		List<int[]> q91 = kb.triples(kb.triple("?s","created","?o"), kb.triple("?s","directed","?o"), kb.triple("?s","produced","?o"));
		List<int[]> q92 = kb.triples(kb.triple("?x","produced","?y"), kb.triple("?x","created","?y"), kb.triple("?x","directed","?y"));
		Pair<List<int[]>, List<int[]>> p9 = new Pair<>(q91, q92);
		cases.add(p9);
		
		//Different queries, close star items shuffled, 3 triple patterns
		List<int[]> q101 = kb.triples(kb.triple("?s","created","?o"), kb.triple("?s","directed","?o"), kb.triple("?s","produced","?o"));
		List<int[]> q102 = kb.triples(kb.triple("?x","produced","?z"), kb.triple("?x","created","?y"), kb.triple("?x","directed","?y"));
		Pair<List<int[]>, List<int[]>> p10 = new Pair<>(q101, q102);
		cases.add(p10);
		
		//Reflexive query
		List<int[]> q111 = kb.triples(kb.triple("?s","married","?u"), kb.triple("?u","married","?s"));
		List<int[]> q112 = kb.triples(kb.triple("?x","married","?z"), kb.triple("?z","married","?x"));
		Pair<List<int[]>, List<int[]>> p11 = new Pair<>(q111, q112);
		cases.add(p11);
		
		//Non Reflexive query
		List<int[]> q121 = kb.triples(kb.triple("?s","married","?u"), kb.triple("?u","married","?s"));
		List<int[]> q122 = kb.triples(kb.triple("?x","married","?z"), kb.triple("?z","married","?y"));
		Pair<List<int[]>, List<int[]>> p12 = new Pair<>(q121, q122);
		cases.add(p12);
		
		//Non Reflexive query
		List<int[]> q131 = kb.triples(kb.triple("?a16","connected","?b16"), kb.triple("?a16","connected","?o0"), kb.triple("?b16","connected","?o1"));
		List<int[]> q132 = kb.triples(kb.triple("?a16","connected","?b16"), kb.triple("?b16","connected","?o1"), kb.triple("?a16","connected","?o0"));
		Pair<List<int[]>, List<int[]>> p13 = new Pair<>(q131, q132);
		cases.add(p13);
		
		//Problematic case
		List<int[]> q141 = kb.triples(kb.triple("?c16","<isLocatedIn>","?d16"), kb.triple("?s","<livesIn>","?c16"), kb.triple("?s","<isPoliticianOf>","?d16"));
		List<int[]> q142 = kb.triples(kb.triple("?c16","<isLocatedIn>","?d16"), kb.triple("?s","<isPoliticianOf>","?d16"), kb.triple("?s","<livesIn>","?c16"));
		Pair<List<int[]>, List<int[]>> p14 = new Pair<>(q141, q142);
		cases.add(p14);
		
		//Rules with slight changes in topology
		List<int[]> q151 = kb.triples(kb.triple("?c","<hasChild>","?b"), kb.triple("?a","<hasChild>","?b"), kb.triple("?c","<isMarriedTo>","?a"));
		List<int[]> q152 = kb.triples(kb.triple("?c","<hasChild>","?a"), kb.triple("?b","<hasChild>","?a"), kb.triple("?b","<isMarriedTo>","?c"));
		Pair<List<int[]>, List<int[]>> p15 = new Pair<>(q151, q152);
		cases.add(p15);
		
		List<int[]> q161 = kb.triples(kb.triple("?c","<hasChild>","?b"), kb.triple("?a","<hasChild>","?b"), kb.triple("?c","<isMarriedTo>","?a"));
		List<int[]> q162 = kb.triples(kb.triple("?c","<hasChild>","?b"), kb.triple("?a","<hasChild>","?b"), kb.triple("?a","<isMarriedTo>","?c"));
		Pair<List<int[]>, List<int[]>> p16 = new Pair<>(q161, q162);
		cases.add(p16);

		Pair<List<int[]>, List<int[]>> p17 = new Pair<>(q152, q162);
		cases.add(p17);
		
		List<int[]> q181 = kb.triples(kb.triple("?s4","<hasWebsite>","?o4"), kb.triple("?s4","<isLocatedIn>","?o6"), kb.triple("?o6","<hasWebsite>","?o4"));
		List<int[]> q182 = kb.triples(kb.triple("?s4","<hasWebsite>","?o4"), kb.triple("?s6","<isLocatedIn>","?s4"), kb.triple("?s6","<hasWebsite>","?o4"));
		List<int[]> q183 = kb.triples(kb.triple("?s4","<hasWebsite>","?o4"), kb.triple("?s6","<hasWebsite>","?o4"), kb.triple("?s6","<isLocatedIn>","?s4"));
		List<int[]> q184 = kb.triples(kb.triple("?s4","<hasWebsite>","?o4"), kb.triple("?s6","<hasWebsite>","?o4"), kb.triple("?s4","<isLocatedIn>","?s6"));
		Pair<List<int[]>, List<int[]>> p18 = new Pair<>(q181, q182);
		Pair<List<int[]>, List<int[]>> p19 = new Pair<>(q181, q183);
		Pair<List<int[]>, List<int[]>> p20 = new Pair<>(q181, q184);
		Pair<List<int[]>, List<int[]>> p21 = new Pair<>(q182, q183);
		Pair<List<int[]>, List<int[]>> p22 = new Pair<>(q182, q184);		
		Pair<List<int[]>, List<int[]>> p23 = new Pair<>(q183, q184);
		
		cases.add(p18);
		cases.add(p19);
		cases.add(p20);
		cases.add(p21);
		cases.add(p22);
		cases.add(p23);

		List<int[]> q241 = kb.triples(kb.triple("?s2","<isMarriedTo>","?o2"), kb.triple("?s2","<hasChild>","?o1"), kb.triple("?o2","<hasChild>","?o1"));
		List<int[]> q242 = kb.triples(kb.triple("?s2","<isMarriedTo>","?o2"), kb.triple("?o2","<hasChild>","?o1"), kb.triple("?s2","<hasChild>","?o1"));
		Pair<List<int[]>, List<int[]>> p24 = new Pair<>(q241, q242);
		cases.add(p24);
		
		List<int[]> q251 = kb.triples(kb.triple("?s4","<hasOfficialLanguage>","?o4"), kb.triple("?s0","<hasOfficialLanguage>","?o2"), kb.triple("?s4","<dealsWith>","?s0"));
		List<int[]> q252 = kb.triples(kb.triple("?s4","<hasOfficialLanguage>","?o4"), kb.triple("?s0","<hasOfficialLanguage>","?o2"), kb.triple("?s4","<dealsWith>","?s0"));
		Pair<List<int[]>, List<int[]>> p25 = new Pair<>(q251, q252);
		cases.add(p25);
		
		List<int[]> q261 = kb.triples(kb.triple("?a","<livesIn>","?c"), kb.triple("?a","<livesIn>","?b"), kb.triple("?b","<hasCapital>","?c"));
		List<int[]> q262 = kb.triples(kb.triple("?a","<livesIn>","?b"), kb.triple("?a","<livesIn>","?c"), kb.triple("?b","<hasCapital>","?c"));
		Pair<List<int[]>, List<int[]>> p26 = new Pair<>(q261, q262);
		cases.add(p26);
		
		List<int[]> q271 = kb.triples(kb.triple("?f","<dealsWith>","?a"), kb.triple("?b","<dealsWith>","?f"), kb.triple("?a","<dealsWith>","?b"));
		List<int[]> q272 = kb.triples(kb.triple("?e","<dealsWith>","?a"), kb.triple("?b","<dealsWith>","?e"), kb.triple("?a","<dealsWith>","?b"));
		Pair<List<int[]>, List<int[]>> p27 = new Pair<>(q271, q272);
		cases.add(p27);
		
		List<int[]> q281 = kb.triples(kb.triple("<New_Zealand>","<participatedIn>","?b"), kb.triple("<United_Kingdom>","<participatedIn>","?b"));
		List<int[]> q282 = kb.triples(kb.triple("<United_Kingdom>","<participatedIn>","?b"), kb.triple("<New_Zealand>","<participatedIn>","?b"));
		Pair<List<int[]>, List<int[]>> p28 = new Pair<>(q281, q282);
		cases.add(p28);

		List<int[]> q291 = kb.triples(kb.triple("?a","<holdsPoliticalPosition>","?b"), 
				kb.triple("?k","<livesIn>","?b"), 
				kb.triple("?f", "<isLocatedIn>", "?j"), 
				kb.triple("?j", "<hasCapital>", "?n"));
		List<int[]> q292 = kb.triples(kb.triple("?a","<holdsPoliticalPosition>","?c"), 
				kb.triple("?f", "<isLocatedIn>", "?j"), 
				kb.triple("?j", "<hasCapital>", "?n"),
				kb.triple("?z","<livesIn>","?c"));
		Pair<List<int[]>, List<int[]>> p29 = new Pair<>(q291, q292);
		cases.add(p29);
		
		List<int[]> q301 = kb.triples(kb.triple("?a","<holdsPoliticalPosition>","?b"), 
				kb.triple("?f","<livesIn>","?b"), 
				kb.triple("?f", "<isLocatedIn>", "?j"), 
				kb.triple("?j", "<hasCapital>", "?n"));
		List<int[]> q302 = kb.triples(kb.triple("?a","<holdsPoliticalPosition>","?c"), 
				kb.triple("?f", "<isLocatedIn>", "?j"), 
				kb.triple("?j", "<hasCapital>", "?n"),
				kb.triple("?z","<livesIn>","?c"));
		Pair<List<int[]>, List<int[]>> p30 = new Pair<>(q301, q302);
		cases.add(p30);
		
		List<int[]> q311 = kb.triples(kb.triple("?a","<hasAcademicAdvisor>","?b"), 
				kb.triple("?x","<influences>","?f"), 
				kb.triple("?f", "<influences>", "?j"), 
				kb.triple("?j", "<hasAcademicAdvisor>", "?n"));
		List<int[]> q312 = kb.triples(kb.triple("?a","<hasAcademicAdvisor>","?b"), 
				kb.triple("?x","<influences>","?f"), 
				kb.triple("?j", "<hasAcademicAdvisor>", "?n"),
				kb.triple("?f", "<influences>", "?j"));
		Pair<List<int[]>, List<int[]>> p31 = new Pair<>(q311, q312);
		cases.add(p31);
		
		List<int[]> q321 = kb.triples(kb.triple("?a","<isPoliticianOf>","?b"), 
				kb.triple("?a","<livesIn>","?f"), 
				kb.triple("?f", "<isLocatedIn>", "?b"));
		List<int[]> q322 = kb.triples(kb.triple("?a","<isPoliticianOf>","?b"), 
				kb.triple("?e","<isLocatedIn>","?b"), 
				kb.triple("?a", "<livesIn>", "?e"));
		Pair<List<int[]>, List<int[]>> p32 = new Pair<>(q321, q322);
		cases.add(p32);
		
		List<int[]> q331 = kb.triples(kb.triple("?a","<isLocatedIn>","?b"), 
				kb.triple("?i", "<isLocatedIn>","?f"), 
				kb.triple("?a", "<isLocatedIn>", "?f"),
				kb.triple("?i", "<isCitizenOf>", "?b"));
		List<int[]> q332 = kb.triples(kb.triple("?a","<isLocatedIn>","?b"), 
				kb.triple("?e", "<isLocatedIn>", "?j"), 
				kb.triple("?a", "<isLocatedIn>", "?j"),
				kb.triple("?e", "<isCitizenOf>", "?b"));
		Pair<List<int[]>, List<int[]>> p33 = new Pair<>(q331, q332);
		cases.add(p33);
		
		//?a  db:birthyear  ?ob1  ?b  db:birthyear  ?ob1  ?a  db:description  british painter  ?b  db:description  british painter   => ?a  equals  ?b
		// ?a  db:birthyear  ?ob1  ?b  db:birthyear  ?ob1  ?a  db:description  british painter  ?b  db:description  british painter   => ?a  equals  ?b

		List<int[]> q341 = kb.triples(kb.triple("?a", "equals", "?b"),
				kb.triple("?b", "db:description", "british painter"),
				kb.triple("?a", "db:description", "british painter"),
				kb.triple("?b", "db:birthyear", "?o1"),
				kb.triple("?a", "db:birthyear", "?o1"));
		List<int[]> q342 = kb.triples(kb.triple("?a", "equals", "?b"),
				kb.triple("?b", "db:description", "british painter"),
				kb.triple("?a", "db:description", "british painter"),
				kb.triple("?b", "db:birthyear", "?o1"),
				kb.triple("?a", "db:birthyear", "?o1"));
		Pair<List<int[]>, List<int[]>> p34 = new Pair<>(q341, q342);
		cases.add(p34);
		
	}
	
	public void test0(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(0).first, cases.get(0).second));
	}
	
	public void test1(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(1).first, cases.get(1).second));
	}
	
	public void test2(){
		assertTrue(!QueryEquivalenceChecker3.areEquivalent(cases.get(2).first, cases.get(2).second));
	}
	
	public void test3(){
		assertTrue(!QueryEquivalenceChecker3.areEquivalent(cases.get(3).first, cases.get(3).second));
	}
	
	public void test4(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(4).first, cases.get(4).second));
	}
	
	public void test5(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(5).first, cases.get(5).second));
	}
	
	public void test6(){
		assertTrue(!QueryEquivalenceChecker3.areEquivalent(cases.get(6).first, cases.get(6).second));
	}
	
	public void test7(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(7).first, cases.get(7).second));
	}
	
	public void test8(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(8).first, cases.get(8).second));
	}
	
	public void test9(){
		assertTrue(!QueryEquivalenceChecker3.areEquivalent(cases.get(9).first, cases.get(9).second));
	}
	
	public void test10(){
		assertTrue(!QueryEquivalenceChecker3.areEquivalent(cases.get(10).first, cases.get(10).second));
	}
	
	public void test11(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(11).first, cases.get(11).second));
	}
	
	public void test12(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(12).first, cases.get(12).second));
	}
	
	public void test13(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(13).first, cases.get(13).second));
	}
	
	public void test14(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(14).first, cases.get(14).second));
	}
	
	public void test15(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(15).first, cases.get(15).second));
	}
	
	public void test16(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(16).first, cases.get(16).second));
	}
	
	public void test17(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(17).first, cases.get(17).second));
	}
	
	public void test18(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(18).first, cases.get(18).second));
	}
	
	public void test19(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(19).first, cases.get(19).second));
	}

	public void test20(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(20).first, cases.get(20).second));
	}
	
	public void test21(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(21).first, cases.get(21).second));
	}

	public void test22(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(22).first, cases.get(22).second));
	}
	
	public void test23(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(23).first, cases.get(23).second));
	}

	public void test24(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(24).first, cases.get(24).second));
	}
	
	public void test25(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(25).first, cases.get(25).second));
	}
	
	public void test26(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(26).first, cases.get(26).second));
	}
	
	public void test27(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(27).first, cases.get(27).second));
	}
	
	public void test28(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(28).first, cases.get(28).second));
	}
	
	public void test29(){
		List<int[]> q1 = kb.triples(kb.triple("?a","<hasAcademicAdvisor>","?b"), kb.triple("?f","<hasAcademicAdvisor>","?b"), kb.triple("?a","<hasAcademicAdvisor>","?f"));
		List<int[]> q2 = kb.triples(kb.triple("?a","<hasAcademicAdvisor>","?b"), kb.triple("?a","<hasAcademicAdvisor>","?e"), kb.triple("?e","<hasAcademicAdvisor>","?b"));
		LinkedHashSet<Rule> pool = new LinkedHashSet<>();
		Rule fq1 = new Rule(kb);
		fq1.getTriples().addAll(q1);
		Rule fq2 = new Rule(kb);
		fq1.setSupport(84);
		fq1.getHeadKey();
		fq2.getTriples().addAll(q2);
		fq2.setSupport(84);
		fq2.getHeadKey();
		pool.add(fq1);
		assertTrue(fq2.equals(fq1));
		//assertTrue(pool.contains(fq2));
		pool.add(fq2);
	}
	
	public void test30(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(29).first, cases.get(29).second));
	}
	
	public void test31(){
		assertFalse(QueryEquivalenceChecker3.areEquivalent(cases.get(30).first, cases.get(30).second));
	}
	
	public void test32(){
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(31).first, cases.get(31).second));
	}
	
	public void test33() {
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(32).first, cases.get(32).second));
	}
	
	public void test34() {
		assertTrue(QueryEquivalenceChecker3.areEquivalent(cases.get(33).first, cases.get(33).second));
	}
        
        public void test35() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "g", "?b"),
                                        kb.triple("?b", "h", "?z"),
                                        kb.triple("?z", "g", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "g", "?b"),
                                        kb.triple("?z", "h", "?a"),
                                        kb.triple("?b", "g", "?z"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test36() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"),
                                        kb.triple("?b", "g", "?z"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?b", "l", "?z"),
                                        kb.triple("?z", "g", "?a"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test37() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?b"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test38() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "g", "?b"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test39() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "g", "?a"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test40() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?z", "l", "?a"),
                                        kb.triple("?a", "h", "?b"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        
        public void test41() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?a", "l", "?z"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test42() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "l", "?a"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?b", "l", "?z"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test43() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?a", "h", "?z"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?b", "h", "?z"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test44() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?a", "h", "?z"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "h", "?a"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test45() {
            List<int[]> q1 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?a", "h", "?z"));
            List<int[]> q2 = kb.triples(
                                        kb.triple("?a", "h", "?b"),
                                        kb.triple("?z", "h", "?b"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test46() {
            // ?a  <created>  ?p  ?a  <directed>  ?b  ?a  <directed>  ?p   => ?a  <produced>  ?b
            // ?a  <created>  ?p  ?a  <directed>  ?p  ?a  <directed>  ?b   => ?a  <produced>  ?b
            List<int[]> q1 = kb.triples(
                    kb.triple("?a", "<produced>", "?b"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<directed>", "?p"),
                    kb.triple("?a", "<created>", "?p"));
            List<int[]> q2 = kb.triples(
                    kb.triple("?a", "<produced>", "?b"),
                    kb.triple("?a", "<directed>", "?p"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<created>", "?p"));
            assertTrue(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test47() {
            // ?a  <created>  ?p  ?a  <directed>  ?b  ?a  <directed>  ?p   => ?a  <produced>  ?b
            // ?a  <created>  ?p  ?a  <directed>  ?b  ?a  <directed>  ?b   => ?a  <produced>  ?b
            List<int[]> q1 = kb.triples(
                    kb.triple("?a", "<produced>", "?b"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<directed>", "?p"),
                    kb.triple("?a", "<created>", "?p"));
            List<int[]> q2 = kb.triples(
                    kb.triple("?a", "<produced>", "?b"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<created>", "?p"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
        public void test48() {
            // ?a  <created>  ?p  ?a  <directed>  ?b  ?a  <directed>  ?p   => ?a  <produced>  ?b
            // ?a  <created>  ?p  ?a  <directed>  ?b  ?a  <directed>  ?b   => ?a  <produced>  ?b
            List<int[]> q1 = kb.triples(
                    kb.triple("?a", "<produced>", "?b"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<directed>", "?p"),
                    kb.triple("?a", "<created>", "?p"));
            List<int[]> q2 = kb.triples(
                    kb.triple("?a", "<produced>", "?p"),
                    kb.triple("?a", "<directed>", "?b"),
                    kb.triple("?a", "<directed>", "?p"),
                    kb.triple("?a", "<created>", "?p"));
            assertFalse(QueryEquivalenceChecker3.areEquivalent(q1, q2));
        }
        
	protected void tearDown() throws Exception {
		super.tearDown();
	}
}
