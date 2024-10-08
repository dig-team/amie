package amie.mining.miniAmie.Unit.Utils;

import amie.mining.miniAmie.CompareToGT;
import amie.mining.miniAmie.Unit.UnitTest;
import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

public class ShouldHaveBeenFoundTests extends UnitTest {

    protected void setUp() throws Exception {
        System.out.println("Setting up CompareRulesTests test.");
        super.setUp () ;
    }

    public void testEmptyBody() {
        System.out.println("Empty Rules Test");
        int[] headAtom = new int[]{1,2,3} ;

        Rule rule = new Rule(headAtom, -1, kb) ;
        assert !CompareToGT.ShouldHaveBeenFound(rule) ;
    }

    public void testPerfectPath() {
        System.out.println("PerfectPath Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;

        assert CompareToGT.ShouldHaveBeenFound(rule) ;
    }


    public void testNonPerfectPath() {
        System.out.println("NonPerfectPath Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-1} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;

        assert !CompareToGT.ShouldHaveBeenFound(rule) ;
    }

    public void testNonPerfectPath2() {
        System.out.println("NonPerfectPath2 Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,-1} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;

        assert !CompareToGT.ShouldHaveBeenFound(rule) ;
    }

    public void testNonPerfectPath3() {
        System.out.println("NonPerfectPath2 Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-4,3,-3} ;
        int[] bodyAtom3 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);
        body.add(bodyAtom3);


        Rule rule = new Rule(headAtom, body, -1, kb) ;

        assert !CompareToGT.ShouldHaveBeenFound(rule) ;
    }

    public void testRedundant() {
        System.out.println("Redundant Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,1,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;

        assert !CompareToGT.ShouldHaveBeenFound(rule) ;
    }
}
