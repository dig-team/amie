package amie.mining.miniAmie.Unit.Utils;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.mining.miniAmie.MiniAmieRule;
import amie.mining.miniAmie.Unit.UnitTest;

import java.util.ArrayList;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.MinSup;
import static amie.mining.miniAmie.utils.GetInitRules;
import static amie.mining.miniAmie.utils.GetInitRulesWithInstantiatedParameter;

public class AddClosureTests extends UnitTest {
    static List<MiniAmieRule> openRules = new ArrayList<>();
    protected void setUp() throws Exception {
        super.setUp();
        System.out.println("Setting up AddClosure test.");
        openRules.addAll(GetInitRules(MinSup)) ;
        openRules.addAll(GetInitRulesWithInstantiatedParameter(MinSup)) ;
    }

    public void testClosureDifferentFromHead() {
        List<MiniAmieClosedRule> closedRules = new ArrayList<>();
        for(MiniAmieRule rule : openRules) {
            closedRules.addAll(rule.AddClosure()) ;
        }

        // Testing
        for(MiniAmieClosedRule rule : closedRules) {
//            IsPerfectPathTests
        }
    }
}
