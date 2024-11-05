package amie.mining.miniAmie.output;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.rules.Rule;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.miniAMIE.Kb;
import static amie.mining.miniAmie.utils.*;

public abstract class OutputRules {

    public static final String OUTPUT_CSV_HEADER = "rule" + commaSep // RULE
            + "headRelation" + commaSep
            + "size" + commaSep
            + "appSupport" + commaSep // APP SUPPORT
            + "altAppSupport" + commaSep // APP SUPPORT
            + "realSupport" + commaSep
            + "appHeadCoverage" + commaSep
            + "realHeadCoverage" + commaSep
            + "appSupportNano" + commaSep
            + "realSupportNano" + commaSep
            + Attributes.GetCSVHeader() + "\n";

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

            outputComparisonCsvWriter.write(OUTPUT_CSV_HEADER);
            System.out.print(OUTPUT_CSV_HEADER);

            // Computing real support using available cores
            List<MiniAmieClosedRule> miniAmieRules = ComputeRuleListMetrics(finalRules);

            for (MiniAmieClosedRule rule : miniAmieRules) {
                String csvLine = OutputCSVLine(rule)
                                + "\n" ; 
                outputComparisonCsvWriter.write(csvLine);
                // Printing comparison to console
                System.out.print(csvLine);
            }

        } catch (Exception e) {
//                System.err.println("Couldn't create output file: "+ outputComparisonCsvPath+ ". Maybe file already exists.");
            e.printStackTrace();
        }


    }

    static public String OutputCSVLine(MiniAmieClosedRule rule) {
        return rule + commaSep + // RULE
                Kb.unmap(rule.getHead()[RELATION_POSITION]) + commaSep // HEAD RELATION
                + (rule.getBody().size() + 1) + commaSep // RULE SIZE
                + rule.getApproximateSupport() + commaSep // APP SUPPORT
                + rule.getAlternativeApproximateSupport() + commaSep // APP SUPPORT
                + rule.getSupport() + commaSep
                + rule.getApproximateHC() + commaSep
                + rule.getHeadCoverage() + commaSep
                + rule.getAppSupportNano() + commaSep
                + rule.getSupportNano() + commaSep
                + rule.getFactorsOfApproximateSupport() ;
    }
}
