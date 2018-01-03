/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.testing;

import amie.data.KB;
import amie.data.SimpleTypingKB;
import amie.typing.classifier.SeparationClassifier;
import static amie.typing.classifier.SeparationClassifier.getOptions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javatools.datatypes.ByteString;
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
public class PrintStatistics {
    public static void main(String[] args) throws IOException {
		
		CommandLine cli = null;
		
		HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = getOptions();
        
        Option eliminationRatioOpt = OptionBuilder.withArgName("er")
                .hasArg()
                .withDescription("Elimination ratio. Default: 1.5")
                .create("er");
        options.addOption(eliminationRatioOpt);
        
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("SeparationClassifier [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }
        
        SeparationClassifier.ParsedArguments pa = new SeparationClassifier.ParsedArguments(cli, formatter, options);
        
        //Load database
        List<File> dataFiles = new ArrayList<>();
        for (int i = 0; i < pa.leftOverArgs.length; ++i) {
                dataFiles.add(new File(pa.leftOverArgs[i]));
        }
        KB dataSource = new SimpleTypingKB();
        dataSource.setDelimiter(pa.delimiter);
        dataSource.load(dataFiles);
        
        //Load classifier
        SeparationClassifier sc;
        if (pa.countFile != null) {
        	if(pa.countIntersectionFile != null) {
        		sc = new SeparationClassifier(dataSource, pa.countFile, pa.countIntersectionFile);
        	} else {
        		sc = new SeparationClassifier(dataSource, pa.countFile);
        	}
        } else {
            System.out.print("Computing classes overlap...");
        	sc = new SeparationClassifier(dataSource);
        	System.out.println(" Done.");
        }
        
        Map<ByteString, Map<ByteString, Double>> stats = sc.computeStatistics(pa.query, pa.variable, pa.classSizeThreshold, pa.supportThreshold);
        for(ByteString t1: stats.keySet()) {
            for(ByteString t2:stats.get(t1).keySet()) {
                System.out.println(t1.toString() + "\t" + t2.toString() + "\t" + Double.toString(stats.get(t1).get(t2)));
            }
        }
    }
}
