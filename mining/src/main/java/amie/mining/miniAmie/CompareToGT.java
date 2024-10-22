package amie.mining.miniAmie;

import amie.rules.QueryEquivalenceChecker;
import amie.rules.Rule;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static amie.mining.miniAmie.ComparedMiniAmieClosedRule.RuleStateComparison.*;
import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.*;
import static amie.mining.miniAmie.utils.ComputeRuleListMetrics;

public class CompareToGT {


    protected static String bodySep = ";" ;

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

    protected static List<Rule> LoadGroundTruthRules() {
        List<Rule> groundTruthRules = new ArrayList<>();
        BufferedReader reader;

        try {
            reader = new BufferedReader(new FileReader(miniAMIE.pathToGroundTruthRules));
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

                        int subject = miniAMIE.kb.map(subjectString);
                        int relation = miniAMIE.kb.map(relationString);
                        int object = miniAMIE.kb.map(objectString);

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
                    int subject = miniAMIE.kb.map(subjectString);
                    int relation = miniAMIE.kb.map(relationString);
                    int object = miniAMIE.kb.map(objectString);
                    headAtom = new int[]{subject, relation, object};
                    Rule groundTruthRule = new Rule(headAtom, bodyAtoms, -1, miniAMIE.kb);
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
//        System.out.println("groundTruthRules "+ groundTruthRules);
        return groundTruthRules;
    }


    static public void PrintComparisonCSV(List<Rule> finalRules, List<Rule> groundTruthRules) {
        List<Rule> comparedRuleList = new ArrayList<>();
        List<Rule> mAmieStyleRuleList = new ArrayList<>();
        // Generating comparison map
        ConcurrentHashMap<Rule, ComparedMiniAmieClosedRule.RuleStateComparison> comparisonMap = new ConcurrentHashMap<>();
        for (Rule rule : finalRules) {
            ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(rule) ;
            comparedRule.setComparisonState(FALSE);
            comparedRuleList.add(comparedRule) ;
            mAmieStyleRuleList.add(comparedRule);
        }
        for (Rule groundTruthRule : groundTruthRules) {
            boolean found = false;
            for (Rule rule : finalRules) {
                if (CompareToGT.CompareRules(rule, groundTruthRule)) {
                    found = true;
                    ComparedMiniAmieClosedRule comparedRule = new ComparedMiniAmieClosedRule(rule) ;
                    comparedRule.setComparisonState(CORRECT);
                    comparedRuleList.add(comparedRule) ;
                    break;
                }
            }
            if (!found) {
                if (MiniAmieClosedRule.ShouldHaveBeenFound(groundTruthRule)) {
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

            String csvColumnLine = String.format(
                    "rule" + commaSep // RULE
                            + "headRelation" + commaSep
                            + "size" + commaSep
                            + "isFalse" + commaSep // FALSE
                            + "isCorrect" + commaSep // CORRECT
                            + "isMissingFailure" + commaSep // MISSING_FAILURE
                            + "isMissingOK" + commaSep // MISSING_OK
                            + "isPerfectPath" + commaSep
                            + "hasRedundancies" + commaSep
                            + "appSupport" + commaSep // APP SUPPORT
                            + "altAppSupport" + commaSep // APP SUPPORT
                            + "realSupport" + commaSep
                            + "appHeadCoverage" + commaSep
                            + "realHeadCoverage" + commaSep
                            + "appSupportNano" + commaSep
                            + "realSupportNano" + commaSep
                            + "relationHeadAtom" + commaSep
                            + "relationFirstBodyAtom" + commaSep
                            + "relationLastBodyAtom" + commaSep
                            + "headAtomObjectToFirstBodyAtomObjectOverlap" + commaSep
                            + "lastBodyAtomSubjectToHeadAtomSubjectOverlap" + commaSep
                            + "relationFirstBodyAtomSize" + commaSep
                            + "relationHeadSize" + commaSep
                            + "rangeFirstBodyAtom" + commaSep
                            + "domainHeadAtom" + commaSep
                            + "domainLastBodyAtom" + commaSep
                            + "ifunRelationFirstBodyAtom" + commaSep
                            + "funRelationHeadAtom" + commaSep
                            + "bodyEstimate" + commaSep
                            + "bodyProductElements"
                            + "\n"
            );
            outputComparisonCsvWriter.write(csvColumnLine);
            System.out.print(csvColumnLine);

            // Computing real support using available cores
            if (!miniAMIE.OutputRules) {
                ComputeRuleListMetrics(mAmieStyleRuleList) ;
            }

            for (Rule rule : comparedRuleList) {
                ComparedMiniAmieClosedRule comparedRule = (ComparedMiniAmieClosedRule) rule;
                ComparedMiniAmieClosedRule.RuleStateComparison compRule = comparisonMap.get(rule);

                // Printing to csv file
                String csvLine = String.format(
                        rule + commaSep + // RULE
                                kb.unmap(rule.getHead()[RELATION_POSITION]) + commaSep // HEAD RELATION
                                + (rule.getBody().size() + 1) + commaSep // RULE SIZE
                                + (compRule == FALSE ? 1 : 0) + commaSep // FALSE
                                + (compRule == CORRECT ? 1 : 0) + commaSep // CORRECT
                                + (compRule == ComparedMiniAmieClosedRule.RuleStateComparison.MISSING_FAILURE ? 1 : 0) + commaSep // MISSING_FAILURE
                                + (compRule == ComparedMiniAmieClosedRule.RuleStateComparison.MISSING_OK ? 1 : 0) + commaSep // MISSING_OK
                                + (MiniAmieClosedRule.IsRealPerfectPath(rule) ? 1 : 0) + commaSep
                                + (MiniAmieClosedRule.HasNoRedundancies(rule) ? 0 : 1) + commaSep
                                + comparedRule.getApproximateSupport() + commaSep // APP SUPPORT
                                + comparedRule.getAlternativeApproximateSupport() + commaSep // APP SUPPORT
                                + comparedRule.getSupport() + commaSep
                                + comparedRule.getApproximateHC() + commaSep
                                + comparedRule.getHeadCoverage() + commaSep
                                + comparedRule.getAppSupportNano() + commaSep
                                + comparedRule.getSupportNano() + commaSep
                                + comparedRule.getFactorsOfApproximateSupport()
                                + "\n"
                );
                outputComparisonCsvWriter.write(csvLine);
                // Printing comparison to console
                System.out.print(comparedRule.getComparisonCharacter() + csvLine + ComparedMiniAmieClosedRule.ANSI_RESET);
            }


        } catch (Exception e) {
//                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
            e.printStackTrace();
        }


    }
}
