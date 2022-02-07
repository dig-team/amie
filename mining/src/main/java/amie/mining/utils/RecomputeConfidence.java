/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.utils;

import amie.data.KB;
import static amie.mining.AMIE.headers;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.experimental.DefaultMiningAssistantWithOrder;
import amie.mining.assistant.experimental.LazyMiningAssistant;
import amie.mining.assistant.variableorder.AppearanceOrder;
import amie.mining.assistant.variableorder.FunctionalOrder;
import amie.mining.assistant.variableorder.InverseOrder;
import amie.mining.assistant.variableorder.VariableOrder;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javatools.parsers.NumberFormatter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 *
 * @author jlajus
 */
public class RecomputeConfidence {
    public static void main(String[] args) throws IOException {

        List<Rule> rules = new ArrayList<Rule>();
        KB kb = new KB();

        List<File> dataFiles = new ArrayList<File>();
        List<File> schemaFiles = new ArrayList<File>();
        List<File> targetFiles = new ArrayList<File>();

        CommandLine cli = null;
        double minPCAConf = 0.1;
        boolean ommitStdConfidence = false;
        /**
         * System performance measure *
         */
        long sourcesLoadingTime = 0l;
        /**
         * ******************************
         */
        String bias = "default"; // Counting support on the two head variables.
        VariableOrder variableOrder = new FunctionalOrder();
        MiningAssistant mineAssistant = null;
        HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();

        Option datalogNotationOpt = OptionBuilder.withArgName("datalog-output")
                .withDescription("Print rules using the datalog notation "
                        + "Default: false")
                .create("datalog");

        Option assistantOp = OptionBuilder.withArgName("e-name")
                .hasArg()
                .withDescription("Syntatic/semantic bias: default|lazy"
                        + "Default: default (defines support and confidence in terms of 2 head variables given an order, cf -vo)")
                .create("bias");


        Option pcaConfThresholdOpt = OptionBuilder.withArgName("min-pca-confidence")
                .hasArg()
                .withDescription("Minimum PCA confidence threshold. "
                        + "This value is not used for pruning, only for filtering of the results. "
                        + "Default: 0.0")
                .create("minpca");

        Option variableOrderOp = OptionBuilder.withArgName("variableOrder")
                .withDescription("Define the order of the variable in counting query among: app, fun (default), ifun")
                .hasArg()
                .create("vo");

        Option calculateStdConfidenceOp = OptionBuilder.withArgName("ommit-std-conf")
                .withDescription("Do not calculate standard confidence")
                .create("ostd");

        Option delimOpt = OptionBuilder.withArgName("delimiter")
                .withDescription("Separator in input files (default: TAB)")
                .hasArg()
                .create("d");

        options.addOption(pcaConfThresholdOpt);
        options.addOption(assistantOp);
        options.addOption(variableOrderOp);
        options.addOption(datalogNotationOpt);
        options.addOption(calculateStdConfidenceOp);
        //options.addOption(enableCountCache);
        options.addOption(delimOpt);

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        if (cli.hasOption("minpca")) {
            String minicStr = cli.getOptionValue("minpca");
            try {
                minPCAConf = Double.parseDouble(minicStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -minpca (PCA confidence threshold) must be an integer greater than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("vo")) {
            switch (cli.getOptionValue("vo")) {
                case "app":
                    variableOrder = new AppearanceOrder();
                    break;
                case "fun":
                    variableOrder = new FunctionalOrder();
                    break;
                case "ifun":
                    variableOrder = InverseOrder.of(new FunctionalOrder());
                    break;
                default:
                    System.err.println("The argument for option -vo must be among \"app\", \"fun\" and \"ifun\".");
                    System.exit(1);
            }
        }

        String[] leftOverArgs = cli.getArgs();

        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            System.err.println("AMIE [OPTIONS] :t<AMIE RULE FILES> <.tsv INPUT FILES>");
            System.exit(1);
        }

        //Load database
        for (int i = 0; i < leftOverArgs.length; ++i) {
            if (leftOverArgs[i].startsWith(":t")) {
                targetFiles.add(new File(leftOverArgs[i].substring(2)));
            } else if (leftOverArgs[i].startsWith(":s")) {
                schemaFiles.add(new File(leftOverArgs[i].substring(2)));
            } else {
                dataFiles.add(new File(leftOverArgs[i]));
            }
        }

        for (File f : targetFiles) {
            rules.addAll(AMIEParser.rules(f));
        }

        KB dataSource = new KB();

        if (cli.hasOption("d")) {
            dataSource.setDelimiter(cli.getOptionValue("d"));
        }

        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();

        sourcesLoadingTime = timeStamp2 - timeStamp1;

        if (cli.hasOption("bias")) {
            bias = cli.getOptionValue("bias");
        }

        switch (bias) {
            case "default":
                mineAssistant = new DefaultMiningAssistantWithOrder(dataSource, variableOrder);
                break;
            case "lazy":
                mineAssistant = new LazyMiningAssistant(dataSource, variableOrder);
                break;
            default:
                System.err.println("The argument for option -bias must be among \"default\" and \"lazy\".");
                System.exit(1);
        }

        mineAssistant.setPcaConfidenceThreshold(minPCAConf);
        ommitStdConfidence = cli.hasOption("ostd");
        mineAssistant.setOmmitStdConfidence(ommitStdConfidence);

        System.out.println(mineAssistant.getDescription());

        long time = System.currentTimeMillis();
        printRuleHeaders(mineAssistant);
        for (Rule rule : rules) {
            if (dataSource.isFunctional(KB.map(rule.getHeadRelation()))) {
                rule.setFunctionalVariablePosition(0);
            } else {
                rule.setFunctionalVariablePosition(2);
            }
            rule.setSupport(mineAssistant.computeCardinality(rule));
            mineAssistant.calculateConfidenceMetrics(rule);
            System.out.println(mineAssistant.formatRule(rule));
        }

        long miningTime = System.currentTimeMillis() - time;
        System.out.println("Recomputation done in " + NumberFormatter.formatMS(miningTime));
        System.out.println(rules.size() + " rules mined.");
    }

    protected static void printRuleHeaders(MiningAssistant assistant) {
        List<String> finalHeaders = new ArrayList<>(headers);
        if (assistant.isOmmitStdConfidence()) {
            finalHeaders.removeAll(Arrays.asList("Std Confidence", "Body size"));
        }

        if (!assistant.isVerbose()) {
            finalHeaders.removeAll(Arrays.asList("Std. Lower Bound", "PCA Lower Bound", "PCA Conf estimation"));
        }

        System.out.println(String.join("\t", finalHeaders));
    }
}
