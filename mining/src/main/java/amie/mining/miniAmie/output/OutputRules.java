package amie.mining.miniAmie.output;

import amie.mining.miniAmie.MiniAmieClosedRule;
import amie.rules.format.AnyBurlFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.miniAMIE.Kb;
import static amie.mining.miniAmie.utils.*;

public abstract class OutputRules {

    public static String RulesExtension = ".rules" ;

    public static final String OUTPUT_CSV_HEADER = "rule" + commaSep // RULE
            + "headRelation" + commaSep
            + "size" + commaSep
            + "appSupport" + commaSep
            + "appSurvivalRateSupport" + commaSep
            + "appAvgSupport" + commaSep
            + "appJacquardSupport" + commaSep // APP SUPPORT
            + "appPCAConfidence" + commaSep // APP SUPPORT
            + "realSupport" + commaSep
            + "appHeadCoverage" + commaSep
            + "realHeadCoverage" + commaSep
            + "appSupportNano" + commaSep
            + "realSupportNano" + commaSep
            + Attributes.GetCSVHeader() + "\n";

    static public void PrintOutputCSV(List<MiniAmieClosedRule> finalRules, String outputFilePath) {

        try {
            File outputCsvFile = new File(outputFilePath);
            if (outputCsvFile.createNewFile()) {
                System.out.println("Created CSV rules output: " + outputFilePath);
            } else {
                System.err.println("Could not create CSV output: " + outputFilePath +
                        ". Maybe name already exists?");
            }

            FileWriter outputCsvWriter = new FileWriter(outputFilePath);

            outputCsvWriter.write(OUTPUT_CSV_HEADER);
            System.out.print(OUTPUT_CSV_HEADER);

            // Computing real support using available cores
            // Attention: finalRules could be emptied
            List<MiniAmieClosedRule> miniAmieRules = ComputeRuleListMetrics(finalRules);

            for (MiniAmieClosedRule rule : miniAmieRules) {
                String csvLine = OutputCSVLine(rule)
                        + "\n";
                outputCsvWriter.write(csvLine);
                System.out.print(csvLine);
            }

            outputCsvWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    static public void PrintOutputAnyBurlFormat(List<MiniAmieClosedRule> finalRules,
                                                String outputFilePath) {

        try {
            File outputCsvFile = new File(outputFilePath);
            if (outputCsvFile.createNewFile()) {
                System.out.println("Created rules output: " + outputFilePath);
            } else {
                System.err.println("Could not create output: " + outputFilePath +
                        ". Maybe name already exists?. The file will be overwritten");
            }

            FileWriter outputWriter = new FileWriter(outputFilePath, false);

            // Computing all metrics using available cores
            List<MiniAmieClosedRule> rules = ComputeRuleListMetrics(finalRules);

            AnyBurlFormatter anyBurlFormatter = new AnyBurlFormatter(false);
            for (MiniAmieClosedRule rule : rules) {
                String line = anyBurlFormatter.fullFormat(rule) + "\n";
                outputWriter.write(line);
                System.out.print(line);
            }

            outputWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static public String OutputCSVLine(MiniAmieClosedRule rule) {
        return rule + commaSep + // RULE
                Kb.unmap(rule.getHead()[RELATION_POSITION]) + commaSep // HEAD RELATION
                + (rule.getBody().size() + 1) + commaSep // RULE SIZE
                + rule.getApproximateSupport() + commaSep // APP SUPPORT
                + rule.getSurvivalRateBasedAppSupport() + commaSep
                + rule.getAvgBasedAppSupport() + commaSep
                + rule.getJacquardBasedAppSupport() + commaSep
                + rule.getPcaEstimation() + commaSep
                + rule.getSupport() + commaSep
                + rule.getApproximateHC() + commaSep
                + rule.getHeadCoverage() + commaSep
                + rule.getAppSupportNano() + commaSep
                + rule.getSupportNano() + commaSep
                + rule.getFactorsOfApproximateSupport();
    }
}
