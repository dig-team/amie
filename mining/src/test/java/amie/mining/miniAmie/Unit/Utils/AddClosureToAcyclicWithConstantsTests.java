package amie.mining.miniAmie.Unit.Utils;

import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.mining.miniAmie.MiniAmieRule;
import amie.mining.miniAmie.Unit.UnitTest;
import amie.mining.miniAmie.utils;
import amie.rules.PruningMetric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.GetInitRulesWithInstantiatedParameter;
import static amie.mining.miniAmie.utils.SelectRelations;

public class AddClosureToAcyclicWithConstantsTests extends UnitTest {
    static Collection<MiniAmieRule> initRulesInstantiatedParameter = new ArrayList<>();

    protected void setUp() throws Exception {
        System.out.println("Setting up AddClosure test.");
        super.setUp();
        utils.miningAssistant = new DefaultMiningAssistant(kb) ;
        SelectedRelations = SelectRelations();
        initRulesInstantiatedParameter = GetInitRulesWithInstantiatedParameter(MinSup) ;

        System.out.println("Using " + PM + " as pruning metric with minimum threshold " +
                (PM == PruningMetric.ApproximateSupport || PM == PruningMetric.Support ? MinSup : MinHC));

    }

    public void testClosureSingle() {
        for (MiniAmieRule rule : initRulesInstantiatedParameter) {
            System.out.println(rule.toString());
            List<MiniAmieClosedRule> instantiatedAtomsBody = rule.AddClosureToAcyclicWithConstants() ;

            for (MiniAmieClosedRule rule1 : instantiatedAtomsBody) {
                    System.out.println(rule1.toString());
                    assert rule1.IsMiniAmieStylePerfectPath() ;
            }
            break ;
        }
    }


    public void testClosure() {
        for (MiniAmieRule rule : initRulesInstantiatedParameter) {
            System.out.println(rule.toString());
            List<MiniAmieClosedRule> instantiatedAtomsBody = rule.AddClosureToAcyclicWithConstants() ;

            for (MiniAmieClosedRule rule1 : instantiatedAtomsBody) {
                System.out.println(rule1.toString());
                assert rule1.IsMiniAmieStylePerfectPath() ;
            }
            break ;
        }
    }

}
