package amie.mining.miniAmie.output.comparisonToGroundTruth;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.mining.miniAmie.miniAMIE;
import amie.mining.miniAmie.utils;
import amie.rules.QueryEquivalenceChecker;
import amie.rules.Rule;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static amie.mining.miniAmie.output.OutputRules.OutputCSVLine;
import static amie.mining.miniAmie.output.comparisonToGroundTruth.ComparedMiniAmieClosedRule.RuleStateComparison.*;
import static amie.mining.miniAmie.output.OutputRules.OUTPUT_CSV_HEADER;
import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;
import static amie.mining.miniAmie.utils.ComputeRuleListMetrics;

// TODO fix this (broken atm)
public class CompareToGT {

    static final String COMPARE_CSV_HEADER = "isFalse" + commaSep // FALSE
            + "isCorrect" + commaSep // CORRECT
            + "isMissingFailure" + commaSep // MISSING_FAILURE
            + "isMissingOK" + commaSep
            + OUTPUT_CSV_HEADER ;
    /**
     * CompareRules will return true if two rules are equivalent (considering atom positions and variable naming)
     * (ex: ?a  < worksAt >  ?c ?c  < isLocatedIn >  ?b => ?a  < isCitizenOf >  ?b
     *  and ?d  < isLocatedIn >  ?b ?a  < worksAt >  ?d => ?a  < isCitizenOf >  ?b are equivalent)
     * @param groundTruthRule
     * @param rule
     * @return
     */
    public static boolean CompareRules(Rule groundTruthRule, Rule rule) {
        return QueryEquivalenceChecker.areEquivalent(groundTruthRule.getTriples(), rule.getTriples());
    }

    public static List<Rule> LoadGroundTruthRules() {
        List<Rule> groundTruthRules = new ArrayList<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(miniAMIE.PathToGroundTruthRules));
            String line = reader.readLine();
            String regexSpace = "[(\\ )|(\t)]+";
            String regexAtom = "(\\?[a-z]" + regexSpace + "<[^>]+>" + regexSpace + "\\?[a-z]" + regexSpace + ")";
            String regexBody = "(" + regexAtom + "+)";
            String regexRule = "(" + regexBody + "=> [(\\ )|(\t)]*" + regexAtom + ")";
            Pattern pat = Pattern.compile(regexRule);
            while (line != null) {
                List<int[]> bodyAtoms = new ArrayList<>();
                int[] headAtom;
                Matcher matcher = pat.matcher(line);
                if (matcher.find()) {
                    String bodyString = matcher.group(2);
                    String[] bodyParts = bodyString.split(regexSpace);
                    for (int i = 0; i < bodyParts.length; i += 3) {
                        String subjectString = bodyParts[i];
                        String relationString = bodyParts[i + 1];
                        String objectString = bodyParts[i + 2];

                        int subject = miniAMIE.Kb.map(subjectString);
                        int relation = miniAMIE.Kb.map(relationString);
                        int object = miniAMIE.Kb.map(objectString);

                        bodyAtoms.add(new int[]{subject, relation, object});
                    }
                    String headString = matcher.group(4);
                    String[] headParts = headString.split(regexSpace);
                    String subjectString = headParts[utils.SUBJECT_POSITION];
                    String relationString = headParts[utils.RELATION_POSITION];
                    String objectString = headParts[utils.OBJECT_POSITION];

                    if(miniAMIE.RestrainedHead != null &&
                            !miniAMIE.RestrainedHead.isEmpty() &&
                            !Objects.equals(relationString, miniAMIE.RestrainedHead))
                        break ;
                    int subject = miniAMIE.Kb.map(subjectString);
                    int relation = miniAMIE.Kb.map(relationString);
                    int object = miniAMIE.Kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};
                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.Kb);
                    groundTruthRules.add(groundTruthRule);
                } else {
                    System.err.println("Could not find ground truth rule in "+line);
                }
                line = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return groundTruthRules;
    }


    static public void PrintComparisonCSV(List<MiniAmieClosedRule> finalRules, List<Rule> groundTruthRules) {
        List<ComparedMiniAmieClosedRule> comparedRuleList = new ArrayList<>();
        List<MiniAmieClosedRule> mAmieStyleRuleList = new ArrayList<>();
        // Generating comparison map
        ConcurrentHashMap<Rule, ComparedMiniAmieClosedRule.RuleStateComparison> comparisonMap = new ConcurrentHashMap<>();
        for (MiniAmieClosedRule rule : finalRules) {
            ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(rule) ;
            comparedRule.setComparisonState(FALSE);
            comparedRuleList.add(comparedRule) ;
            mAmieStyleRuleList.add(comparedRule);
        }
        for (Rule groundTruthRule : groundTruthRules) {
            boolean found = false;
            for (MiniAmieClosedRule rule : finalRules) {
                if (CompareToGT.CompareRules(rule, groundTruthRule)) {
                    found = true;
                    ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(rule) ;
                    comparedRule.setComparisonState(CORRECT);
                    comparedRuleList.add(comparedRule) ;
                    break;
                }
            }
            if (!found) {
                if (MiniAmieClosedRule.RespectsLanguageBias(groundTruthRule)) {
                    ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(groundTruthRule) ;
                    comparedRule.setComparisonState(MISSING_FAILURE);
                    comparedRuleList.add(comparedRule) ;
                    mAmieStyleRuleList.add(comparedRule);
                }
                else {
                    ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(groundTruthRule) ;
                    comparedRule.setComparisonState(MISSING_OK);
                    comparedRuleList.add(comparedRule) ;
                }
            }
        }

        // Displaying comparison map
        System.out.println(" Comparison to ground truth: ");
        try {
            File outputComparisonCsvFile = new File(OutputComparisonCsvPath);
            if (outputComparisonCsvFile.createNewFile()) {
                System.out.println("Created CSV comparison to ground rules output: " + OutputComparisonCsvPath);
            } else {
                System.err.println("Could not create CSV output: " + OutputComparisonCsvPath +
                        ". Maybe name already exists?");
            }

            FileWriter outputComparisonCsvWriter = new FileWriter(OutputComparisonCsvPath);

            outputComparisonCsvWriter.write(COMPARE_CSV_HEADER);
            System.out.print(COMPARE_CSV_HEADER);

            // Computing real support using available cores
            if (!miniAMIE.OutputRules) {
                ComputeRuleListMetrics(mAmieStyleRuleList) ;
            }

            for (ComparedMiniAmieClosedRule rule : comparedRuleList) {

                // Printing to csv file
                String csvLine =
                                 boolToBit(rule.IsFalse()) + commaSep // FALSE
                                + boolToBit(rule.IsCorrect()) + commaSep // CORRECT
                                + boolToBit(rule.IsMissingFailure()) + commaSep // MISSING_FAILURE
                                + boolToBit(rule.IsMissingOK()) + commaSep // MISSING_OK
                                + OutputCSVLine(rule) + "\n" ;
                outputComparisonCsvWriter.write(csvLine);
                // Printing comparison to console
                System.out.print(rule.getComparisonCharacter() + csvLine + ComparedMiniAmieClosedRule.ANSI_RESET);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    static private int boolToBit(boolean bool) {
        return bool ? 1 : 0;
    }
}
