package amie.mining.miniAmie.output.comparisonToGroundTruth;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.rules.Rule;

public class ComparedMiniAmieClosedRule extends MiniAmieClosedRule {

    public enum RuleStateComparison {
        /** Rule has been found by mini-Amie and is in ground truth rule set */
        CORRECT,
        /** Rule has been found by mini-Amie but is not in ground truth rule set */
        FALSE,
        /** Rule has not been found by mini-Amie and is in ground truth rule set
         * BUT should not be found by mini-Amie (ex: rule is not a perfect path, rule has redundant relations) */
        MISSING_OK,
        /** Rule has not been found by mini_Amie and is in ground truth rule set
         * BUT should have been found by mini-Amie */
        MISSING_FAILURE
    }

    protected static final String ANSI_PURPLE = "\u001B[35m";
    protected static final String ANSI_YELLOW = "\u001B[33m";
    protected static final String ANSI_GREEN = "\u001B[32m";
    protected static final String ANSI_RED = "\u001B[31m";
    protected static final String ANSI_RESET = "\u001B[0m";

    private String comparisonCharacter = "" ;
    private RuleStateComparison comparisonState = RuleStateComparison.FALSE ;

    /**
     * Will instantiate a ComparedMiniAmieClosedRule as MiniAmieClosedRule with default values
     * (empty strings and -1 for numeric values)
     * @param rule
     */
    ComparedMiniAmieClosedRule(Rule rule) {
        super(rule);
    }

    
    public String getComparisonCharacter() {
        return comparisonCharacter;
    }

    public RuleStateComparison getComparisonState() {
        return comparisonState;
    }

    public void setComparisonState(RuleStateComparison comparisonState) {
        switch (comparisonState) {
            case FALSE -> comparisonCharacter = ComparedMiniAmieClosedRule.ANSI_YELLOW;
            case CORRECT -> comparisonCharacter = ComparedMiniAmieClosedRule.ANSI_GREEN;
            case MISSING_FAILURE -> comparisonCharacter = ComparedMiniAmieClosedRule.ANSI_RED;
            case MISSING_OK -> comparisonCharacter = ComparedMiniAmieClosedRule.ANSI_PURPLE;
            default -> throw new RuntimeException("Unknown comparison state "
                    + comparisonState + " for rule " + this);
        }
        this.comparisonState = comparisonState;
    }

    public boolean IsFalse() {
        return comparisonState == RuleStateComparison.FALSE;
    }
    public boolean IsCorrect() {
        return comparisonState == RuleStateComparison.CORRECT;
    }
    public boolean IsMissingFailure() {
        return comparisonState == RuleStateComparison.MISSING_FAILURE;
    }
    public boolean IsMissingOK() {
        return comparisonState == RuleStateComparison.MISSING_OK;
    }




}
