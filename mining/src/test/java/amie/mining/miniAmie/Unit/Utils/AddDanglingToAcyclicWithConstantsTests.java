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
import static amie.mining.miniAmie.utils.*;

public class AddDanglingToAcyclicWithConstantsTests extends UnitTest {
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

    public void testDangling() {
        for (MiniAmieRule rule : initRulesInstantiatedParameter) {
            System.out.println(rule.toString());
            List<MiniAmieRule> instantiatedAtomsBody = rule.AddDanglingToAcyclicWithConstants() ;

            for (MiniAmieRule rule1 : instantiatedAtomsBody) {
                List<MiniAmieClosedRule> closures = rule1.AddClosure() ;
                for (MiniAmieClosedRule rule2 : closures) {
                    System.out.println(rule2.toString());
                    assert rule2.IsMiniAmieStylePerfectPath() ;
                }
                break ;
            }
            break ;
        }
    }

}
