package amie.mining.miniAmie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static amie.mining.miniAmie.CompareToGT.PrintComparisonCSV;
import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.BidirectionalityJaccardThreshold;
import static amie.mining.miniAmie.utils.bidirectionalityMap;

public class GlobalSearchResult {
    public static void PrintGlobalSearchResultCSV(long startTime, AtomicInteger totalSumExploredRules,
                                                  AtomicInteger totalSumExploredRulesAdjustedWithBidirectionality) {
        // Displaying result
        long duration = System.currentTimeMillis() - startTime;
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - 24 * days;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - 60 * hours;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - 60 * minutes;
        long milliseconds = duration - 1000 * seconds;

        System.out.println("Search duration: " +
                (days > 0 ? days + " days " : "") +
                (hours > 0 ? hours + " hours " : "") +
                (minutes > 0 ? minutes + " minutes " : "") +
                (seconds > 0 ? seconds + " seconds " : "") +
                milliseconds + " milliseconds" +
                ".");
        System.out.println("Search space approximation: " + totalSumExploredRules + " possibilities.");
        System.out.println("Search space approximation (adjusted with bidirectionality): " +
                totalSumExploredRulesAdjustedWithBidirectionality + " possibilities.");

        System.out.println("Bidirectional relations (range-dom Jaccard >= " + BidirectionalityJaccardThreshold + "):");
        for (int relation : bidirectionalityMap.keySet())
            if (bidirectionalityMap.get(relation)) {
                System.out.print(kb.unmap(relation) + " ");
            }

        // Outputing general information on run config
        try {
            File outputRunConfigCsvFile = new File(outputConfigurationCsvPath);
            if (outputRunConfigCsvFile.createNewFile()) {
                System.out.println("Created CSV run config output: " + outputConfigurationCsvPath);
            } else {
                System.err.println("Could not create CSV output: " + outputConfigurationCsvPath +
                        ". Maybe name already exists?");
            }

            FileWriter outputConfigurationCsvWriter = new FileWriter(outputConfigurationCsvPath);

            System.out.println("Run configuration :");
            String runConfigCsvHeader =
                    "MaxRuleSize" + CompareToGT.sep
                            + "MinSup" + CompareToGT.sep
                            + "NThreads" + CompareToGT.sep
                            + "ShowRealSupport" + CompareToGT.sep
                            + "ShowExplorationLayers" + CompareToGT.sep
                            + "Verbose" + CompareToGT.sep
                            + "ErrorRateThreshold" + CompareToGT.sep
                            + "CompareToGroundTruth" + CompareToGT.sep
                            + "RestrainedHead" + CompareToGT.sep
                            + "PathToGroundTruthRules" + CompareToGT.sep
                            + "CorrectionFactorClosure" + CompareToGT.sep
                            + "CorrectionFactorOpening" + CompareToGT.sep
                            + "SearchRuntime" + CompareToGT.sep
                            + "MemoryPeak" + CompareToGT.sep
                            + "SearchSpaceSizeEstimate" + CompareToGT.sep
                            + "FixedSearchSpaceSizeEstimate"
                            + "\n";
            System.out.print(runConfigCsvHeader);
            outputConfigurationCsvWriter.write(runConfigCsvHeader);
            String runConfigCsvLine = "" +
                    MaxRuleSize + CompareToGT.sep
                    + MinSup + CompareToGT.sep
                    + NThreads + CompareToGT.sep
                    + ShowRealSupport + CompareToGT.sep
                    + ShowExplorationLayers + CompareToGT.sep
                    + Verbose + CompareToGT.sep
                    + ErrorRateThreshold + CompareToGT.sep
                    + CompareToGroundTruth + CompareToGT.sep
                    + (RestrainedHead == null ? "" : RestrainedHead) + CompareToGT.sep
                    + (pathToGroundTruthRules == null ? "" : pathToGroundTruthRules) + CompareToGT.sep
                    + CORRECTION_FACTOR_CLOSURE + CompareToGT.sep
                    + CORRECTION_FACTOR_OPENING + CompareToGT.sep
                    + duration + CompareToGT.sep
                    + Runtime.getRuntime().totalMemory() / 1048576 + CompareToGT.sep
                    + totalSumExploredRules + CompareToGT.sep
                    + totalSumExploredRulesAdjustedWithBidirectionality + "\n";
            outputConfigurationCsvWriter.write(runConfigCsvLine);
            System.out.print(runConfigCsvLine);
            outputConfigurationCsvWriter.close();
        } catch (IOException e) {
            System.err.println("Couldn't create output file: " + outputComparisonCsvPath + ". Maybe file already exists.");
            e.printStackTrace();
        }
    }
}
