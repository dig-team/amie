/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing;

import amie.data.CardinalitySimpleTypingKB;
import amie.data.KB;
import amie.data.Schema;
import amie.data.SimpleTypingKB;
import amie.data.WikidataSimpleTypingKB;
import amie.typing.classifier.simple.B2SimpleClassifier;
import amie.typing.classifier.simple.B3SimpleClassifier;
import amie.typing.classifier.simple.FisherSeparationSimpleClassifier;
import amie.typing.classifier.simple.RelaxedSeparationSimpleClassifier;
import amie.typing.classifier.simple.SimpleClassifier;
import amie.typing.classifier.simple.SimpleClassifier.SimpleClassifierOutput;
import amie.typing.classifier.simple.StrictSeparationSimpleClassifier;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
public class SimpleTyping extends Thread {
    private BlockingQueue<Pair<Pair<Integer, Integer>, SimpleClassifier>> queryQ;
    //public Iterable<Integer> classSizeThresholds; 
    public int supportThreshold;
    
    public SimpleTyping(BlockingQueue<Pair<Pair<Integer, Integer>, SimpleClassifier>> queryQ,
            //Iterable<Integer> classSizeThresholds, 
            int supportThreshold) {
        this.queryQ = queryQ;
        //this.classSizeThresholds = classSizeThresholds;
        this.supportThreshold = supportThreshold;
    }
    
    @Override
    public void run() {
        Pair<Pair<Integer, Integer>, SimpleClassifier> q;
        while(true) {
            try {
                q = queryQ.take();
                if (q.first == null) { break; }
                q.second.classify(q.first.second, q.first.first, supportThreshold);
            } catch (InterruptedException ex) {
                Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (IOException ex) {
                System.err.println("IOException");
                Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static class LogComparator implements Comparator<Double> {

        @Override
        public int compare(Double t, Double t1) {
            return (Math.abs(Math.log(t)) == Math.abs(Math.log(t1))) ? 0 : ((Math.abs(Math.log(t)) > Math.abs(Math.log(t1))) ? -1 : 1);
        }
        
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        ArrayList<Integer> classSizeThresholds = null;
        int supportThreshold = 50;
        String delimiter = "\t";
        String[] leftOverArgs;
        String[] queries = null; 
        Int2ObjectMap<Int2IntMap> countIntersectionMap = null;
        int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        PrintStream output = System.out;
        ArrayList<Double> t1 = null, t2 = null, t3 = null;
        List<Pair<String, String>> classifiers = null;
        Queue<SimpleClassifierOutput> outputQ = new LinkedList<>();
        Lock outputLock = new ReentrantLock();
        
        
        CommandLine cli = null;
	HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new PosixParser();
        
        Option classSizeThresholdOpt = OptionBuilder.withArgName("cst")
                .hasArgs()
                .withDescription("Class size Thresholds. Default: 50")
                .create("cst");
        Option supportThresholdOpt = OptionBuilder.withArgName("st")
                .hasArg()
                .withDescription("Support threshold. Default: 50")
                .create("st");
        Option coresOpt = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores - 1"
                		+ "in the system if a higher value is provided.")
                .create("nc");
        Option delimiterOpt = OptionBuilder
                .hasArg()
                .withDescription("Delimiter in KB files")
                .create("d");
        Option typeRelationOpt = OptionBuilder.withArgName("typeRelation")
                .hasArg()
                .withDescription("Type relation used in this KB. Default: rdf:type")
                .create("tr");
        Option subClassRelationOpt = OptionBuilder.withArgName("subClassRelation")
                .hasArg()
                .withDescription("Sub Class relation used in this KB. Default: rdfs:subClassOf")
                .create("scr");
        Option topOpt = OptionBuilder.withArgName("topClass")
                .hasArg()
                .withDescription("Top class used in this KB. Default: owl:Thing")
                .create("top");
        Option queryOpt = OptionBuilder.withArgName("attribute")
                .hasArgs()
                .withDescription("Only run on a specific attribute [-1]")
                .create("q");
        Option countIntersectionFile = OptionBuilder.withArgName("iCountF")
                .hasArg()
                .withDescription("Intersection count file (for speedup)")
                .create("icf");
        Option wikidataOpt = OptionBuilder.withDescription("Assume wikidata setup, overrides -tr, -scr, -top, -d")
                .create("w");
        Option thresholds1_Opt = OptionBuilder.withArgName("t")
                .hasArgs()
                .withDescription("Thresholds list 1")
                .create("t1");
        Option thresholds2_Opt = OptionBuilder.withArgName("t")
                .hasArgs()
                .withDescription("Thresholds list 2")
                .create("t2");
        Option thresholds3_Opt = OptionBuilder.withArgName("t")
                .hasArgs()
                .withDescription("Thresholds list 3")
                .create("t3");
        Option classifierOpt = OptionBuilder.withArgName("classifier:n")
                .hasArgs()
                .withDescription("Run classifier \"b2\", \"b3\", \"strict\", \"relaxed\" or \"fisher\" with thresholds list n")
                .create("c");
        Option outputOpt = OptionBuilder.withArgName("of")
                .hasArg()
                .withDescription("Output file. Default: stdout")
                .create("o");
        Option cardOpt = OptionBuilder
                .withDescription("Enable cardinality experiment")
                .create("card");
        
        Options options = new Options();
        // Cores
        options.addOption(coresOpt);
        // Parameters
        options.addOption(classSizeThresholdOpt);
        options.addOption(supportThresholdOpt);
        // Query
        options.addOption(queryOpt);
        options.addOption(cardOpt);
        // Count files options
        options.addOption(countIntersectionFile);
        // KB related options
        options.addOption(delimiterOpt);
        options.addOption(typeRelationOpt);
        options.addOption(topOpt);
        options.addOption(subClassRelationOpt);
        options.addOption(wikidataOpt);
        // Thresholds
        options.addOption(thresholds1_Opt);
        options.addOption(thresholds2_Opt);
        options.addOption(thresholds3_Opt);
        // Classifiers
        options.addOption(classifierOpt);
        // Output
        options.addOption(outputOpt);
        
        /*
        * Let parse this
        */
        
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("SimpleTyping [OPTIONS] <KB FILES>", options);
            System.exit(1);
        }
        
        if (cli.hasOption("cst")) {
            try {
                String[] cstStr = cli.getOptionValues("cst");
                classSizeThresholds = new ArrayList<>(cstStr.length);
                for (String cst : cstStr) {
                    classSizeThresholds.add(Integer.parseInt(cst));
                }
            } catch (NumberFormatException e) {
                System.err.println("The option -cst (class size thresholds) requires integers as argument");
                formatter.printHelp("SimpleTyping [OPTIONS] <KB FILES>", options);
                System.exit(1);
            }
        } else {
            classSizeThresholds = new ArrayList<>(1);
            classSizeThresholds.add(50);
        }

        if (cli.hasOption("st")) {
            try {
                String stStr = cli.getOptionValue("st");
                supportThreshold = Integer.parseInt(stStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -st (support threshold) requires an integer as argument");
                formatter.printHelp("SimpleTyping [OPTIONS] <KB FILES>", options);
                System.exit(1);
            }
        }

        // Schema related options
        if (cli.hasOption("tr")) {
            Schema.typeRelation = cli.getOptionValue("tr");
            Schema.typeRelationBS = KB.map(Schema.typeRelation);
        }
        if (cli.hasOption("scr")) {
            Schema.subClassRelation = cli.getOptionValue("scr");
            Schema.subClassRelationBS = KB.map(Schema.subClassRelation);
        }
        if (cli.hasOption("top")) {
            Schema.top = cli.getOptionValue("top");
            Schema.topBS = KB.map(Schema.top);
        }

        // Delimiter
        if (cli.hasOption("d")) {
            delimiter = cli.getOptionValue("d");
        }

        // Wikidata setup overrides Schema + delimiter
        if (cli.hasOption("w")) {
            Schema.typeRelation = "<P106>";
            Schema.typeRelationBS = KB.map(Schema.typeRelation);
            Schema.subClassRelation = "<P279>";
            Schema.subClassRelationBS = KB.map(Schema.subClassRelation);
            Schema.top = "<Q35120>";
            Schema.topBS = KB.map(Schema.top);
            delimiter = " ";
        }

        leftOverArgs = cli.getArgs();
        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            formatter.printHelp("SimpleTyping [OPTIONS] <KB FILES>", options);
            System.exit(1);
        }

        if (cli.hasOption("icf")) {
            try {
                countIntersectionMap = Schema.loadTypesIntersectionCount(new File(cli.getOptionValue("icf")));
            } catch (Exception e) {
                System.err.println("Error while creating intersection size map, ignoring.");
            }
        }

        if (cli.hasOption("q")) {
            queries = cli.getOptionValues("q");
        }
        
        if (cli.hasOption("nc")) {
            String nCoresStr = cli.getOptionValue("nc");
            try {
                nThreads = Math.max(Math.min(nThreads, Integer.parseInt(nCoresStr)), 1);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -nc (number of threads) must be an integer");
                formatter.printHelp("SimpleTyping [OPTIONS] <KB FILES>", options);
                System.exit(1);
            }
        }
        
        if (cli.hasOption("o")) {
            try {
                output = new PrintStream(new File(cli.getOptionValue("o")));
            } catch (Exception e) {
                System.out.println("Unexpected exception setting up output file: " + e.getMessage());
                System.exit(1);
            }
        }
        
        if (cli.hasOption("t1")) {
            String[] erStr = cli.getOptionValues("t1");
            t1 = new ArrayList<>(erStr.length);
            for (String s: erStr) {
        	try {
                    double d = Double.parseDouble(s);
                    if (d < 0) {
                        System.err.println("Warning: ignoring negative threshold in t1: "+s);
                        continue;
                    }
                    t1.add(d);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: ignoring invalid threshold in t1: "+s);
                    continue;
                }
            }
        }
        
        if (cli.hasOption("t2")) {
            String[] erStr = cli.getOptionValues("t2");
            t2 = new ArrayList<>(erStr.length);
            for (String s: erStr) {
        	try {
                    double d = Double.parseDouble(s);
                    if (d < 0) {
                        System.err.println("Warning: ignoring negative threshold in t2: "+s);
                        continue;
                    }
                    t2.add(d);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: ignoring invalid threshold in t2: "+s);
                    continue;
                }
            }
        }
        
        if (cli.hasOption("t3")) {
            String[] erStr = cli.getOptionValues("t3");
            t3 = new ArrayList<>(erStr.length);
            for (String s: erStr) {
        	try {
                    double d = Double.parseDouble(s);
                    if (d < 0) {
                        System.err.println("Warning: ignoring negative threshold in t3: "+s);
                        continue;
                    }
                    t3.add(d);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: ignoring invalid threshold in t3: "+s);
                    continue;
                }
            }
        }
        
        if (cli.hasOption("c")) {
            String[] cs = cli.getOptionValues("c");
            classifiers = new ArrayList<>(cs.length);
            for (String c : cs) {
                String[] cn = c.split(":");
                if (cn.length != 2) {
                    System.err.println("Warning: classifier parse error "+c+", ignoring.");
                } else {
                    switch(cn[0]) {
                        case "b2":
                        case "b3":
                        case "strict":
                        case "relaxed":
                        case "fisher": break;
                        default: 
                            System.err.println("Warning: invalid classifier name: "+cn[0]+", ignoring.");
                            continue;
                    }
                    switch(cn[1]) {
                        case "1": 
                            if (t1 == null || t1.isEmpty()) {
                                System.err.println("Warning: null or empty threshold list 1, ignoring.");
                                continue;
                            }
                            break;
                        case "2": 
                            if (t2 == null || t2.isEmpty()) {
                                System.err.println("Warning: null or empty threshold list 2, ignoring.");
                                continue;
                            }
                            break;
                        case "3":
                            if (t3 == null || t3.isEmpty()) {
                                System.err.println("Warning: null or empty threshold list 3, ignoring.");
                                continue;
                            }
                            break;
                        default:
                            System.err.println("Warning: invalid threshold list number: "+cn[1]+", ignoring.");
                            continue;
                    }
                    classifiers.add(new Pair<>(cn[0], cn[1]));
                }
            }
        }
        if (classifiers == null || classifiers.isEmpty()) {
            System.err.println("No valid classifier defined");
            System.exit(1);
        }
        
        // KB files
        List<File> dataFiles = new ArrayList<>();
        for (int i = 0; i < leftOverArgs.length; ++i) {
                dataFiles.add(new File(leftOverArgs[i]));
        }
        SimpleTypingKB dataSource;
        
        if (cli.hasOption("card")) {
            dataSource = new CardinalitySimpleTypingKB();
        } else if (cli.hasOption("w")) {
            dataSource = new WikidataSimpleTypingKB();
        } else {
            dataSource = new SimpleTypingKB();
        }
        dataSource.setDelimiter(delimiter);
        dataSource.load(dataFiles);
        
        if (dataSource instanceof CardinalitySimpleTypingKB) {
            ((CardinalitySimpleTypingKB) dataSource).computeCardinalities();
        }
        
        // Populate query queue
        BlockingQueue queryQ = new LinkedBlockingQueue();
        
        IntSet relations;
        if (queries != null && queries.length > 0) {
            relations = new IntOpenHashSet();
            for (int i = 0; i < queries.length; i++) {
                relations.add(KB.map(queries[i]));
            }
        } else {
            relations = dataSource.relations.keySet();
        }
        /*for (int r : relations) {
            System.err.println(r.toString());
        }*/
        
        // Create the SimpleClassifier objects
        List<SimpleClassifier> classifiersO = new ArrayList(classifiers.size());
        for (Pair<String, String> cn : classifiers) {
            SimpleClassifier c = null;
            ArrayList<Double> t = null;
            switch(cn.second) {
                case "1": t = t1; break;
                case "2": t = t2; break;
                case "3": t = t3; break;
            }
            switch(cn.first) {
                case "b2":
                case "b3": Collections.sort(t); break;
                default: Collections.sort(t, new LogComparator());
            }
            double[] tf = new double[t.size()];
            for (int i = 0; i < t.size(); i++) {
                tf[i] = t.get(i);
            }
            switch(cn.first) {
                case "b2":
                    for (int r : relations) {
                        for (Integer classSizeThreshold : classSizeThresholds) {
                            queryQ.add(new Pair<>(
                                new Pair<>(classSizeThreshold, r), 
                                new B2SimpleClassifier(dataSource, tf, outputQ, outputLock, countIntersectionMap)));
                        }
                    }
                    break;
                case "b3":
                    for (int r : relations) {
                        for (Integer classSizeThreshold : classSizeThresholds) {
                            queryQ.add(new Pair<>(
                                new Pair<>(classSizeThreshold, r), 
                                new B3SimpleClassifier(dataSource, tf, outputQ, outputLock, countIntersectionMap)));
                        }
                    }
                    break;
                case "strict":
                    for (int r : relations) {
                        for (Integer classSizeThreshold : classSizeThresholds) {
                            queryQ.add(new Pair<>(
                                new Pair<>(classSizeThreshold, r), 
                                new StrictSeparationSimpleClassifier(dataSource, tf, outputQ, outputLock, countIntersectionMap)));
                        }
                    }
                    break;
                case "relaxed":
                    for (int r : relations) {
                        for (Integer classSizeThreshold : classSizeThresholds) {
                            queryQ.add(new Pair<>(
                                new Pair<>(classSizeThreshold, r), 
                                new RelaxedSeparationSimpleClassifier(dataSource, tf, outputQ, outputLock, countIntersectionMap)));
                        }
                    }
                    break;
                case "fisher":
                    for (int r : relations) {
                        for (Integer classSizeThreshold : classSizeThresholds) {
                            queryQ.add(new Pair<>(
                                new Pair<>(classSizeThreshold, r), 
                                new FisherSeparationSimpleClassifier(dataSource, tf, outputQ, outputLock, countIntersectionMap)));
                        }
                    }
                    break;
            }
            classifiersO.add(c);
        }
        //System.err.println(queryQ.size());
        //System.exit(0);
        
        for (int i = 0; i < nThreads; i++) {
            queryQ.add(new Pair<>(null, null));
        }
        
        long timeStamp1 = System.currentTimeMillis();
        List<Thread> threadList = new ArrayList<>(nThreads);
        
        for (int i = 0; i < nThreads; i++) {
                threadList.add(new SimpleTyping(queryQ, supportThreshold));
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
        long timeStamp2 = System.currentTimeMillis();
        System.out.println("Processing done with "+Integer.toString(nThreads)+" threads in "+Long.toString(timeStamp2 - timeStamp1)+"ms.");
        
        for (SimpleClassifierOutput r : outputQ) {
            output.println(r.classSizeThreshold + "\t" + r.method + "\t" + Double.toString(r.threshold) + "\t" + KB.unmap(r.relation) + "\t" + KB.unmap(r.resultClass));
        }
        
        try {
            output.close();
        } catch (Exception e) {}
    }
}
