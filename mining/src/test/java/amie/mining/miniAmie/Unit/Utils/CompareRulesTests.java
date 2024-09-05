package amie.mining.miniAmie.Unit.Utils;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.Unit.UnitTest;
import amie.mining.miniAmie.utils;
import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

public class CompareRulesTests extends UnitTest {


    protected void setUp() throws Exception {
        System.out.println("Setting up CompareRulesTests test.");
        super.setUp () ;
    }

    public void testCompareEmptyBody() {
        int[] headAtom = new int[]{1,2,3} ;

        Rule rule = new Rule(headAtom, -1, kb) ;
        assert utils.CompareRules(rule, rule) ;

        int[] headAtom2 = new int[]{1,4,8} ;
        Rule rule2 = new Rule(headAtom2, -1, kb) ;
        assert !utils.CompareRules(rule, rule2) ;
        assert !utils.CompareRules(rule2, rule) ;
    }

    public void testCompareLength2() {
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-2} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;
        assert utils.CompareRules(rule, rule) ;

        int[] headAtomOther = new int[]{-1,5,-2} ;
        int[] bodyAtomOther1 = new int[]{-3,8,-2} ;
        int[] bodyAtomOther2 = new int[]{-1,2,-2} ;
        List<int[]> bodyOther = new ArrayList<>();
        bodyOther.add(bodyAtomOther1);
        bodyOther.add(bodyAtomOther2);
        Rule ruleOther = new Rule(headAtomOther, bodyOther, -1, kb) ;
        assert !utils.CompareRules(ruleOther, rule) ;
        assert !utils.CompareRules(rule, ruleOther) ;
    }

    public void testCompareDifferentLength() {
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-2} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;
        assert utils.CompareRules(rule, rule) ;

        int[] headAtomOther = new int[]{-1,1,-2} ;
        int[] bodyAtomOther1 = new int[]{-3,2,-2} ;
        List<int[]> bodyOther = new ArrayList<>();
        bodyOther.add(bodyAtomOther1);
        Rule ruleOther = new Rule(headAtomOther, bodyOther, -1, kb) ;
        assert !utils.CompareRules(ruleOther, rule) ;
        assert !utils.CompareRules(rule, ruleOther) ;
    }

    public void testCompareDifferentLength2() {
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-10,10,-20} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);

        Rule rule = new Rule(headAtom, body, -1, kb) ;
        assert utils.CompareRules(rule, rule) ;

        int[] headAtomOther = new int[]{-1,1,-2} ;
        int[] bodyAtomOther1 = new int[]{-3,2,-2} ;
        List<int[]> bodyOther = new ArrayList<>();
        bodyOther.add(bodyAtomOther1);
        Rule ruleOther = new Rule(headAtomOther, bodyOther, -1, kb) ;
        assert !utils.CompareRules(ruleOther, rule) ;
        assert !utils.CompareRules(rule, ruleOther) ;
    }

    public void testCompareDifferentVariableNames() {
        int[] headAtom = new int[]{-1,1,-2} ;
        int[] bodyAtom1 = new int[]{-3,2,-2} ;
        int[] bodyAtom2 = new int[]{-1,3,-2} ;
        List<int[]> body = new ArrayList<>();
        body.add(bodyAtom1);
        body.add(bodyAtom2);
        Rule rule = new Rule(headAtom, body, -1, kb) ;

        int[] headAtomOther = new int[]{-10,1,-20} ;
        int[] bodyAtomOther1 = new int[]{-30,2,-20} ;
        int[] bodyAtomOther2 = new int[]{-10,3,-20} ;
        List<int[]> bodyOther = new ArrayList<>();
        bodyOther.add(bodyAtomOther1);
        bodyOther.add(bodyAtomOther2);
        Rule ruleOther = new Rule(headAtomOther, bodyOther, -1, kb) ;

        assert utils.CompareRules(ruleOther, rule) ;
        assert utils.CompareRules(rule, ruleOther) ;
    }

}
