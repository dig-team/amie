package amie.mining.miniAmie;

import amie.rules.Rule;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.miniAMIE.kb;
import static amie.mining.miniAmie.utils.*;

public class OutputRules {

    static public void PrintOutputCSV(List<Rule> finalRules) {

        try {
            File outputCsvFile = new File(OutputRulesCsvPath);
            if (outputCsvFile.createNewFile()) {
                System.out.println("Created CSV rules output: " + OutputRulesCsvPath);
            } else {
                System.err.println("Could not create CSV output: " + OutputRulesCsvPath +
                        ". Maybe name already exists?");
            }

            FileWriter outputComparisonCsvWriter = new FileWriter(OutputRulesCsvPath);

            String csvColumnLine = String.format(
                    "rule" + commaSep // RULE
                            + "headRelation" + commaSep
                            + "size" + commaSep
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
            List<MiniAmieClosedRule> miniAmieRules = ComputeRuleListMetrics(finalRules);

            for (MiniAmieClosedRule rule : miniAmieRules) {
                String csvLine = String.format(
                        rule + commaSep + // RULE
                                kb.unmap(rule.getHead()[RELATION_POSITION]) + commaSep // HEAD RELATION
                                + (rule.getBody().size() + 1) + commaSep // RULE SIZE
                                + rule.getApproximateSupport() + commaSep // APP SUPPORT
                                + rule.getAlternativeApproximateSupport() + commaSep // APP SUPPORT
                                + rule.getSupport() + commaSep
                                + rule.getApproximateHC() + commaSep
                                + rule.getHeadCoverage() + commaSep
                                + rule.getAppSupportNano() + commaSep
                                + rule.getSupportNano() + commaSep
                                + rule.getFactorsOfApproximateSupport()
                                + "\n"
                );
                outputComparisonCsvWriter.write(csvLine);
                // Printing comparison to console
                System.out.print(csvLine);
            }

        } catch (Exception e) {
//                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
            e.printStackTrace();
        }


    }
}
