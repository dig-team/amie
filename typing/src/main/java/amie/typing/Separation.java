/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing;

import amie.data.KB;
import amie.data.Schema;
import amie.data.SexismSimpleTypingKB;
import amie.data.SimpleTypingKB;
import amie.typing.classifier.SeparationClassifier;
import static amie.typing.classifier.SeparationClassifier.getOptions;
import amie.typing.classifier.SeparationPTreeClassifier;
import amie.typing.classifier.SeparationSTreeClassifier;
import amie.typing.classifier.SeparationTreeClassifier;
import amie.typing.classifier.SeparationVTreeClassifier;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    Int2IntMap cS;
    Int2ObjectMap<Int2IntMap> cIS;
    BlockingQueue<Pair<List<int[]>, Integer>> queryQ; 
    int classSizeThreshold; 
    int supportThreshold; 
    double[] thresholds;
    boolean supportForTarget;
    String classifier;
    
    public Separation(KB source, Int2IntMap cS, Int2ObjectMap<
            Int2IntMap> cIS, BlockingQueue<Pair<List<int[]>, 
            Integer>> queryQ, int classSizeThreshold, int supportThreshold, 
            double[] thresholds, boolean supportForTarget, String classifier) {
        this.source = source;
        this.cS = cS;
        this.cIS = cIS;
        this.queryQ = queryQ;
        this.classSizeThreshold = classSizeThreshold;
        this.supportThreshold = supportThreshold;
        this.thresholds = thresholds;
        this.supportForTarget = supportForTarget;
        this.classifier = classifier;
    }
    
    
    
        @Override
    public void run() {
        Pair<List<int[]>, Integer> q;
        while(true) {
            try {
                q = queryQ.take();
                if (q.second.equals(KB.map("STOP"))) { System.out.println("Thread terminating"); break; }
                System.out.println("Beginning "+ KB.unmap(q.first.get(0)[1]) + " (" + q.second + ") ...");
                SeparationTreeClassifier st;
                switch(classifier) {
                    case "P":
                        System.out.println("Using probabilistic classifier.");
                        st = new SeparationPTreeClassifier(source, cS, cIS, supportForTarget); 
                        break;
                    case "V": 
                        System.out.println("Using variance classifier.");
                        st = new SeparationVTreeClassifier(source, cS, cIS, supportForTarget); 
                        break;
                    case "S": 
                        System.out.println("Using strict CR classifier.");
                        st = new SeparationSTreeClassifier(source, cS, cIS, supportForTarget); 
                        break;
                    default: st = new SeparationTreeClassifier(source, cS, cIS, supportForTarget);
                }
                st.classify(q.first, q.second, classSizeThreshold, supportThreshold, thresholds);
                System.out.println("Finished: "+ KB.unmap(q.first.get(0)[1]) + " (" + q.second + ")");
            } catch (InterruptedException ex) {
                Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (IOException ex) {
                System.err.println("IOException");
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
        Option supportForTargetOpt = OptionBuilder
                .withDescription("Compare only with classes that meet the support threshold")
                .create("sft");
        Option coresOpt = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores - 1"
                		+ "in the system if a higher value is provided.")
                .create("nc");
        Option classifierOpt = OptionBuilder.withArgName("classifier")
                .hasArg()
                .withDescription("Default: PRatio, P: probabilistic, V: variance-based")
                .create("c");
        options.addOption(thresholdsOpt);
        options.addOption(coresOpt);
        options.addOption(supportForTargetOpt);
        options.addOption(classifierOpt);
        
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
            //System.out.println(t[i]);
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
        
        // supportForTargetOpt
        boolean supportForTarget = cli.hasOption("sft");
        
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
        KB dataSource;
        if (pa.query != null && pa.query.get(0)[1] == (KB.map("sexism"))) {
            dataSource = new SexismSimpleTypingKB();
        } else {
            dataSource = new SimpleTypingKB();
        }
        dataSource.setDelimiter(pa.delimiter);
        dataSource.load(dataFiles);
        
        // Load the counts
        Int2IntMap cS;
        Int2ObjectMap<Int2IntMap> cIS;
        
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
            IntSet relations = dataSource.getRelationSet();
            relations.remove(Schema.typeRelationBS);
            relations.remove(Schema.subClassRelationBS);

            int[] q;
            for (int r : relations) {
                q = KB.triple(KB.map("?x"), r, KB.map("?y"));
                queryQ.add(new Pair<>(KB.triples(q), KB.map("?x")));
                queryQ.add(new Pair<>(KB.triples(q), KB.map("?y")));
            }
        } else if (pa.query.get(0)[1] == (KB.map("sexism"))) {
            nThreads = Math.min(nProcessors, 2);
            // Note: KB instanceof SexismSimpleTypingKB
            queryQ.add(new Pair<>(KB.triples(KB.triple("?x", "<male>", "?y")), KB.map("?x")));
            queryQ.add(new Pair<>(KB.triples(KB.triple("?x", "<female>", "?y")), KB.map("?x")));
        } else {
            queryQ.add(new Pair<>(pa.query, pa.variable));
        }
        for (int i = 0; i < nThreads; i++) {
            queryQ.add(new Pair<>(Collections.EMPTY_LIST, KB.map("STOP")));
        }
        
        // Let's thread !
        long timeStamp1 = System.currentTimeMillis();
        List<Thread> threadList = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            if (cli.hasOption("c")) {
                threadList.add(new Separation(dataSource, cS, cIS, queryQ, 
                    pa.classSizeThreshold, pa.supportThreshold, t, 
                        supportForTarget, cli.getOptionValue("c")));
            } else {
                threadList.add(new Separation(dataSource, cS, cIS, queryQ, 
                    pa.classSizeThreshold, pa.supportThreshold, t, supportForTarget, "D"));
            }
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
        long timeStamp2 = System.currentTimeMillis();
        System.out.println("Processing done with "+Integer.toString(nThreads)+" threads in "+Long.toString(timeStamp2 - timeStamp1)+"ms.");
    }
    
}
