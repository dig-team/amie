package amie.mining.utils;

import amie.mining.miniAmie.utils;
import amie.rules.PruningMetric;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

import static amie.mining.miniAmie.miniAMIE.*;

public abstract class GlobalSearchResult {
   public static final String HEADER = "Timestamp" + utils.commaSep
                + "MaxRuleSize" + utils.commaSep
                + "PruningMetric" + utils.commaSep
                + "MinSup" + utils.commaSep
                + "MinHC" + utils.commaSep
                + "NThreads" + utils.commaSep
                + "SearchRuntime" + utils.commaSep
                + "MemoryPeak_kB" + utils.commaSep
                + "SearchSpace" + "\n";
    public static String Timestamp = Instant.now().toString().replace(" ", "_") ;
    public static String CSVExtension = ".csv";
    public static String Suffix =   Timestamp + CSVExtension;
    public static String OutputConfigurationCsvPath = "./run-" + Suffix;
    public static boolean OutputConfigurationToAlreadyExistingCSV = false;

    public static void PrintGlobalSearchResultToCSV(
            int maxRuleSize,
            PruningMetric pruningMetric,
            double minSup,
            double minHC,
            int nThreads,
            long duration,
            int searchSpace) {

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
                    + Timestamp + utils.commaSep
                    + maxRuleSize + utils.commaSep
                    + pruningMetric + utils.commaSep
                    + minSup + utils.commaSep
                    + minHC + utils.commaSep
                    + nThreads + utils.commaSep
                    + duration + utils.commaSep
                    + Benchmarking.PeakMemory() + utils.commaSep
                    + searchSpace + "\n";
            outputConfigurationCsvWriter.write(runConfigCsvLine);
            System.out.print(runConfigCsvLine);
            outputConfigurationCsvWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
