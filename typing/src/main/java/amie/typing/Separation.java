/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing;

import amie.data.KB;
import amie.data.Schema;
import amie.typing.classifier.SeparationClassifier;
import static amie.typing.classifier.SeparationClassifier.getOptions;
import amie.typing.classifier.SeparationTreeClassifier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;
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
public class Separation extends Thread {
    
    KB source; 
    IntHashMap<ByteString> cS;
    Map<ByteString, IntHashMap<ByteString>> cIS;
    BlockingQueue<Pair<List<ByteString[]>, ByteString>> queryQ; 
    int classSizeThreshold; 
    int supportThreshold; 
    double[] thresholds;
    
    public Separation(KB source, IntHashMap<ByteString> cS, Map<ByteString, IntHashMap<ByteString>> cIS,
            BlockingQueue<Pair<List<ByteString[]>, ByteString>> queryQ, int classSizeThreshold, int supportThreshold, double[] thresholds) {
        this.source = source;
        this.cS = cS;
        this.cIS = cIS;
        this.queryQ = queryQ;
        this.classSizeThreshold = classSizeThreshold;
        this.supportThreshold = supportThreshold;
        this.thresholds = thresholds;
    }
    
        @Override
    public void run() {
        Pair<List<ByteString[]>, ByteString> q;
        while(true) {
            try {
                q = queryQ.take();
                if (q.second.equals(ByteString.of("STOP"))) break;
                SeparationTreeClassifier st = new SeparationTreeClassifier(source, cS, cIS);
                st.classify(q.first, q.second, classSizeThreshold, supportThreshold, thresholds);
            } catch (InterruptedException ex) {
                Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (IOException ex) {
                Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        CommandLine cli = null;
		
	HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = getOptions();
        
        Option thresholdsOpt = OptionBuilder.withArgName("t")
                .hasArgs()
                .withDescription("thresholds")
                .create("t");
        Option coresOpt = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores - 1"
                		+ "in the system if a higher value is provided.")
                .create("nc");
        options.addOption(thresholdsOpt);
        options.addOption(coresOpt);
        
	try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("SeparationClassifier [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }
        
        SeparationClassifier.ParsedArguments pa = new SeparationClassifier.ParsedArguments(cli, formatter, options);
        
        // Generate thresholds array
        ArrayList<Double> thresholds = new ArrayList<>();
        if (cli.hasOption("t")) {
            String[] erStr = cli.getOptionValues("t");
            for (String s: erStr) {
        	try {
                    thresholds.add(-Math.abs(Math.log(Double.parseDouble(s))));
                } catch (NumberFormatException e) {
                    System.err.println("Warning: ignoring invalid threshold "+s);
                    continue;
                }
            }
        }
        Collections.sort(thresholds);
        double[] t = new double[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            t[i] = thresholds.get(i);
        }
        
        // Get number of threads
        int nProcessors = Runtime.getRuntime().availableProcessors();
        int nThreads = Math.max(1, nProcessors - 1);
        if (cli.hasOption("nc")) {
            String nCoresStr = cli.getOptionValue("nc");
            try {
                nThreads = Math.max(Math.min(nThreads, Integer.parseInt(nCoresStr)), 1);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -nc (number of threads) must be an integer");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }
        
        // KB files
        List<File> dataFiles = new ArrayList<>();
        
        if (pa.leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            System.err.println("Typing [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        for (int i = 0; i < pa.leftOverArgs.length; ++i) {
                dataFiles.add(new File(pa.leftOverArgs[i]));
        }
        
        // Load the KB
        KB dataSource = new KB();
        dataSource.setDelimiter(pa.delimiter);
        dataSource.load(dataFiles);
        
        // Load the counts
        IntHashMap<ByteString> cS;
        Map<ByteString, IntHashMap<ByteString>> cIS;
        
        if (pa.countFile == null) {
            cS = Schema.getTypesCount(dataSource);
            cIS = Schema.getTypesIntersectionCount(dataSource);
        } else {
            cS = Schema.loadTypesCount(pa.countFile);
            cIS = (pa.countIntersectionFile == null) ? Schema.loadTypesIntersectionCount(pa.countFile) : Schema.loadTypesIntersectionCount(pa.countIntersectionFile);
        }
        
        // Populate query queue
        BlockingQueue queryQ = new LinkedBlockingQueue();
        
        if (pa.query == null) {
            List<ByteString[]> query = new ArrayList<>(1);
            query.add(KB.triple(ByteString.of("?x"), ByteString.of("?y"), ByteString.of("?z")));
            Set<ByteString> relations = dataSource.selectDistinct(ByteString.of("?y"), query);
            relations.remove(Schema.typeRelationBS);

            ByteString[] q;
            for (ByteString r : relations) {
                q = KB.triple(ByteString.of("?x"), r, ByteString.of("?y"));
                queryQ.add(new Pair<>(KB.triples(q), ByteString.of("?x")));
                queryQ.add(new Pair<>(KB.triples(q), ByteString.of("?y")));
            }
        } else {
            queryQ.add(new Pair<>(pa.query, pa.variable));
        }
        for (int i = 0; i < nThreads; i++) {
            queryQ.add(new Pair<>(Collections.EMPTY_LIST, ByteString.of("STOP")));
        }
        
        // Let's thread !
        List<Thread> threadList = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            threadList.add(new Separation(dataSource, cS, cIS, queryQ, pa.classSizeThreshold, pa.supportThreshold, t));
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
    }
    
}
