package amie.mining.miniAmie;

import amie.data.AbstractKB;
import amie.rules.Rule;

import java.util.ArrayList;
import java.util.List;

public class miniAMIE {
    public static AbstractKB kb;
    public static int MaxRuleSize ;
    public static int MinSup ;
    private static class ReturnType {
        int sumExploredRules = 0 ;
        List<Rule> finalRules;

        public ReturnType(int sumExploredRules, List<Rule> finalRules) {
            this.sumExploredRules = sumExploredRules;
            this.finalRules = finalRules;
        }
    }

    // TODO
    private Rule AddClosure(Rule rule) {
        return rule ;
    }

    // TODO
    private Rule AddObjectSubjectDanglingAtom(Rule rule) {
        return rule ;
    }

    // TODO
    private Rule AddObjectObjectDanglingAtom(Rule rule) {
        return rule ;
    }

    // TODO
    private double ApproximateSupportClosedRule(Rule rule) {
        return 0 ;
    }

    // TODO
    private double ApproximateSupportOpenRule(Rule rule) {
        return 0 ;
    }

    // TODO
    private List<Rule> GetInitRules() {
        return null ;
    }

    private void Run() {
        List<Rule> initRules = GetInitRules();
        int totalSumExploredRules = 0;
        List<Rule> finalRules = new ArrayList<>();
        for (Rule rule : initRules) {
            double supp = rule.getSupport();
            if (supp >= MinSup) {
                ReturnType exploreChildrenResult = InitExploreChildren(rule);
                totalSumExploredRules += exploreChildrenResult.sumExploredRules;
                finalRules.addAll(exploreChildrenResult.finalRules);
            }
            totalSumExploredRules += 1;
        }

        // Displaying result
        System.out.println("Search space approximation: " + totalSumExploredRules + " explored rules.");
        System.out.println("Approximate mining: ") ;
        for (Rule rule : finalRules) {
            System.out.println(rule.toString()) ;
        }
        System.out.println("Thank you for using mini-Amie. See you next time");
    }

    private ReturnType InitExploreChildren(Rule rule) {
        int totalSumExploredRules = 0 ;
        List<Rule> finalRules = new ArrayList<>();
        Rule closedChild = AddClosure(rule) ;

        if (ApproximateSupportClosedRule(closedChild) < MinSup) {
            totalSumExploredRules = 1 ;
            finalRules.add(closedChild) ;
        }

        Rule openChild = AddObjectObjectDanglingAtom(rule) ;
        if (ApproximateSupportOpenRule(openChild) < MinSup) {
            return new ReturnType(totalSumExploredRules, finalRules);
        }

        ReturnType exploreOpenChildResult = ExploreChildren(openChild);
        finalRules.addAll(exploreOpenChildResult.finalRules) ;
        totalSumExploredRules += exploreOpenChildResult.sumExploredRules * 2  ;
        return new ReturnType(totalSumExploredRules, finalRules);
    }

    private ReturnType ExploreChildren(Rule rule)
    {
      if (rule.getBodySize() + 1 > MaxRuleSize) {
          return new ReturnType(0, new ArrayList<Rule>());
      }
      int totalSumExploredRules = 0 ;
      List<Rule> finalRules = new ArrayList<>();
      Rule closedChild = AddClosure(rule) ;

      if (ApproximateSupportClosedRule(closedChild) < MinSup) {
          totalSumExploredRules = 1 ;
          finalRules.add(closedChild) ;
      }

      if (rule.getBodySize() + 2 > MaxRuleSize) {
          return new ReturnType(totalSumExploredRules, finalRules);
      }

      Rule openChild = AddObjectSubjectDanglingAtom(rule) ;
      if (ApproximateSupportOpenRule(openChild) < MinSup) {
          return new ReturnType(totalSumExploredRules, finalRules);
      }

      ReturnType exploreOpenChildResult = ExploreChildren(openChild);
      finalRules.addAll(exploreOpenChildResult.finalRules) ;
      totalSumExploredRules += exploreOpenChildResult.sumExploredRules * 2 ;
      return new ReturnType(totalSumExploredRules, finalRules);
    }
}
