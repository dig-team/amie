package amie.mining.miniAmie.Unit.Utils;

import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.MiniAmieRule;
import amie.mining.miniAmie.Unit.UnitTest;
import amie.mining.miniAmie.utils;
import amie.rules.Rule;

import java.util.Collection;
import java.util.List;

public class GetInitRulesTests extends UnitTest {
    private final double DEFAULT_MIN_SUPPORT = 100 ;
    Collection<MiniAmieRule> initRules ;

    protected void setUp() throws Exception {
        System.out.println("Setting up GetInitRulesTests test.");
        super.setUp () ;
        utils.miningAssistant = new DefaultMiningAssistant (kb);
        initRules = utils.GetInitRules (DEFAULT_MIN_SUPPORT);
    }

    /**
     * Checks that single atom rules produced by GetInitRules are above min support
     */
    public void testMinSupport() {
        for (Rule rule : initRules) {
            double sup = rule.getSupport() ;
            System.out.printf("%s : %f\n", rule, sup);
            assert sup > DEFAULT_MIN_SUPPORT : String.format("Found init rules with support (%s : %f) under min value %f",
                    rule, sup, DEFAULT_MIN_SUPPORT); ;
        }
    }

    /**
     * Checks that body is empty
     */
    public void testEmptyBody() {
        for (Rule rule : initRules) {
            System.out.println(rule);
            List<int[]> body = rule.getBody() ;
            assert body.isEmpty() : String.format("Found init rules with body (%s)",
                    rule); ;
        }
    }


}
