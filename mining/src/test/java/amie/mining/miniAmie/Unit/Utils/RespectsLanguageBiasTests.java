package amie.mining.miniAmie.Unit.Utils;

import amie.data.KB;
import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.mining.miniAmie.Unit.UnitTest;
import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

import static org.locationtech.jts.util.Memory.KB;

public class RespectsLanguageBiasTests extends UnitTest {

    protected void setUp() throws Exception {
        System.out.println("Setting up RespectsLanguageBiasTests test.");
        super.setUp () ;
    }

    public void testEmptyBody() {
        System.out.println("Empty Rules Test");
        int[] headAtom = new int[]{1,2,3} ;

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, kb) ;

        assert rule.IsMiniAmieStylePerfectPath() ;
    }

    public void testPerfectPath() {
        System.out.println("PerfectPath Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;

        assert rule.IsMiniAmieStylePerfectPath() ;
    }


    public void testPerfectPath2() {
        System.out.println("PerfectPath2 Test (allow switching parameters)");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,-1} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;

        assert rule.IsMiniAmieStylePerfectPath() ;
    }

    public void testPerfectPath3() {
        System.out.println("PerfectPath3 Test (allow acyclic instantiated parameters" +
                " in head and last body atom)");
        int[] headAtom = new int[]{10,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,20} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;

        assert rule.IsMiniAmieStylePerfectPath() ;
    }

    public void testNonPerfectPath() {
        System.out.println("NonPerfectPath Test (argument redundancy)");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-1} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        try {
            new MiniAmieClosedRule(headAtom, body, kb) ;
            assert false ;
        } catch (IllegalArgumentException e) {
            assert true ;
        } catch (Exception e) {
            assert false ;
        }
    }


    public void testNonPerfectPath3() {
        System.out.println("NonPerfectPath Test (relation redundancy)");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-4,3,-3} ;
        int[] bodyAtom3 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);
        body.add(bodyAtom3);

        try {
            new MiniAmieClosedRule(headAtom, body, kb) ;
            assert false ;
        } catch (IllegalArgumentException e) {
            assert true ;
        } catch (Exception e) {
            assert false ;
        }
    }

    public void testNonPerfectPath4() {
        System.out.println("NonPerfectPath Test (forbid isolated constant in head)");
        int[] headAtom = new int[]{10,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,-2} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;
        assert !rule.IsMiniAmieStylePerfectPath() ;
    }

    public void testNonPerfectPath5() {
        System.out.println("NonPerfectPath Test (forbid isolated constant in last body atom)");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,20} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;
        assert !rule.IsMiniAmieStylePerfectPath() ;
    }

    public void testNonPerfectPath6() {
        System.out.println("NonPerfectPath Test (forbid third constant in last body atom)");
        int[] headAtom = new int[]{10,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{30,3,20} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        try {
            new MiniAmieClosedRule(headAtom, body, kb) ;
            assert false ;
        } catch (IllegalArgumentException e) {
            assert true ;
        } catch (Exception e) {
            assert false ;
        }
    }

    public void testNonPerfectPath7() {
        System.out.println("NonPerfectPath Test (forbid third constant in body atom)");
        int[] headAtom = new int[]{10,1,-2} ;
        int[] bodyAtom1 = new int[]{30,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,20} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        try {
            new MiniAmieClosedRule(headAtom, body, kb) ;
            assert false ;
        } catch (IllegalArgumentException e) {
            assert true ;
        } catch (Exception e) {
            assert false ;
        }
    }

    public void testNonPerfectPath8() {
        System.out.println("NonPerfectPath Test (forbid isolated constant in body atom)");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{30,2,-2} ;
        int[] bodyAtom2 = new int[]{-3,3,-1} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        try {
            new MiniAmieClosedRule(headAtom, body, kb) ;
            assert false ;
        } catch (IllegalArgumentException e) {
            assert true ;
        } catch (Exception e) {
            assert false ;
        }
    }

    public void testRedundant() {
        System.out.println("Redundant Test");
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,1,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-3} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        MiniAmieClosedRule rule = new MiniAmieClosedRule(headAtom, body, kb) ;

        assert !rule.RespectsLanguageBias() ;
    }
}
