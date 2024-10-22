package amie.mining.miniAmie;

import amie.mining.utils.Benchmarking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static amie.mining.miniAmie.miniAMIE.*;
import static amie.mining.miniAmie.utils.BidirectionalityJaccardThreshold;
import static amie.mining.miniAmie.utils.bidirectionalityMap;

public class GlobalSearchResult {
   public static final String HEADER = "Timestamp" + utils.commaSep
                + "MaxRuleSize" + utils.commaSep
                + "PruningMetric" + utils.commaSep
                + "MinSup" + utils.commaSep
                + "MinHC" + utils.commaSep
                + "NThreads" + utils.commaSep
                + "ShowRealSupport" + utils.commaSep
                + "ShowExplorationLayers" + utils.commaSep
                + "Verbose" + utils.commaSep
                + "ErrorRateThreshold" + utils.commaSep
                + "CompareToGroundTruth" + utils.commaSep
                + "RestrainedHead" + utils.commaSep
                + "PathToGroundTruthRules" + utils.commaSep
                + "CorrectionFactorClosure" + utils.commaSep
                + "CorrectionFactorOpening" + utils.commaSep
                + "SearchRuntime" + utils.commaSep
                + "MemoryPeak_kB" + utils.commaSep
                + "SearchSpaceSizeEstimate" + utils.commaSep
                + "FixedSearchSpaceSizeEstimate"
                + "\n";

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
            File outputRunConfigCsvFile = new File(OutputConfigurationCsvPath);
            FileWriter outputConfigurationCsvWriter ;
            if (!OutputConfigurationToAlreadyExistingCSV || !outputRunConfigCsvFile.isFile()) {
                if (outputRunConfigCsvFile.createNewFile()) {
                    System.out.println("Created CSV run config output: " + OutputConfigurationCsvPath);
                } else {
                    throw new IOException("Could not create CSV output: " + OutputConfigurationCsvPath +
                            ". Maybe name already exists?") ;
                }
                outputConfigurationCsvWriter = new FileWriter(outputRunConfigCsvFile, true) ;
                outputConfigurationCsvWriter.write(HEADER);
            } else {
                outputConfigurationCsvWriter = new FileWriter(outputRunConfigCsvFile, true) ;
            }

            System.out.println("Run configuration :");
            System.out.print(HEADER);
            String runConfigCsvLine = ""
                    + timestamp + utils.commaSep
                    + MaxRuleSize + utils.commaSep
                    + PM + utils.commaSep
                    + MinSup + utils.commaSep
                    + MinHC + utils.commaSep
                    + NThreads + utils.commaSep
                    + ShowRealSupport + utils.commaSep
                    + ShowExplorationLayers + utils.commaSep
                    + Verbose + utils.commaSep
                    + ErrorRateThreshold + utils.commaSep
                    + CompareToGroundTruth + utils.commaSep
                    + (RestrainedHead == null ? "" : RestrainedHead) + utils.commaSep
                    + (pathToGroundTruthRules == null ? "" : pathToGroundTruthRules) + utils.commaSep
                    + CORRECTION_FACTOR_CLOSURE + utils.commaSep
                    + CORRECTION_FACTOR_OPENING + utils.commaSep
                    + duration + utils.commaSep
                    + Benchmarking.PeakMemory() + utils.commaSep
                    + totalSumExploredRules + utils.commaSep
                    + totalSumExploredRulesAdjustedWithBidirectionality + "\n";

//            Files.write(
//                    Paths.get(OutputConfigurationCsvPath),
//                    runConfigCsvLine.getBytes(),
//                    StandardOpenOption.APPEND);
            outputConfigurationCsvWriter.write(runConfigCsvLine);
            System.out.print(runConfigCsvLine);
            outputConfigurationCsvWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
