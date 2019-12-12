package amie.typing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;



import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.data.Schema;
import amie.data.SimpleTypingKB;
import amie.typing.heuristics.*;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javatools.datatypes.Triple;

public class Typing {

    public static class TypingMT extends Thread {

        private BlockingQueue<Triple<List<int[]>, Integer, TypingHeuristic>> queryQ;
        private IntSet classes;
        private double outputThreshold;

        public TypingMT(BlockingQueue<Triple<List<int[]>, Integer, TypingHeuristic>> queryQ,
                IntSet classes, double outputThreshold) {
            this.queryQ = queryQ;
            this.classes = classes;
            this.outputThreshold = outputThreshold;
        }

        public void run() {
            Triple<List<int[]>, Integer, TypingHeuristic> q;
            BufferedWriter out;
            while (true) {
                try {
                    q = queryQ.take();
                    if (q.second.equals(KB.map("STOP"))) {
                        break;
                    }

                    if (q.first.size() > 1) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    int[] singleton = q.first.get(0);
                    String fn = q.third.name + "_";
                    fn += KB.unmap(singleton[1]).substring(1, KB.unmap(singleton[1]).length() - 1);
                    fn += (singleton[2] == (q.second)) ? "-1" : "";

                    out = new BufferedWriter(new FileWriter(fn));
                    double s;
                    for (int c : classes) {
                        s = q.third.evaluate(c, q.first, q.second);
                        if (outputThreshold <= s) {
                            out.write(KB.unmap(c) + "\t" + Double.toString(s) + "\n");
                        }
                    }
                    out.close();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(Separation.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        CommandLine cli = null;
        String[] heuristics = {};
        Integer popularityThreshold = 50;
        Integer supportThreshold = -1;
        double outputThreshold = 0.95;
        String delimiter = "\t";

        //Schema.typeRelation = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
        //Schema.typeRelationBS = KB.map(Schema.typeRelation);
        HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();
        Option heuristicOpt = OptionBuilder.withArgName("heuristic")
                .hasArgs()
                .withDescription("Heuristic to use among: pop, *std*, rev")
                .create("heur");
        Option popularityOpt = OptionBuilder.withArgName("popularity-threshold")
                .hasArg()
                .withDescription("Number of links for an entity to be popular. Default: 50")
                .create("popt");
        Option outputThresholdOpt = OptionBuilder.withArgName("output-threshold")
                .hasArg()
                .withDescription("Heuristic score for being output. Default: 0.95")
                .create("outputt");
        Option snapshotOpt = OptionBuilder.withArgName("snapshotDB")
                .hasArg()
                .withDescription("KB with old facts")
                .create("snapshot");
        Option cacheOpt = OptionBuilder
                .withDescription("Enable count cache")
                .create("c");
        Option delimiterOpt = OptionBuilder
                .hasArg()
                .withDescription("Delimiter in KB files")
                .create("d");
        Option typesOpt = OptionBuilder.withArgName("snapshotTypes")
                .hasArg()
                .withDescription("TSV file with rdf:type links for the snapshot")
                .create("t");
        Option supportThresholdOpt = OptionBuilder.withArgName("st")
                .hasArg()
                .withDescription("Support threshold. Default: -1")
                .create("st");
        Option nThreadOpt = OptionBuilder.withArgName("nc")
                .hasArg()
                .withDescription("Number of core")
                .create("nc");
        Option wikidataOpt = OptionBuilder.withDescription("Assume wikidata setup")
                .create("w");

        options.addOption(delimiterOpt);
        options.addOption(heuristicOpt);
        options.addOption(popularityOpt);
        options.addOption(outputThresholdOpt);
        options.addOption(snapshotOpt);
        options.addOption(typesOpt);
        options.addOption(nThreadOpt);
        options.addOption(supportThresholdOpt);
        options.addOption(wikidataOpt);

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        if (cli.hasOption("popt")) {
            try {
                String popThresholdStr = cli.getOptionValue("popt");
                popularityThreshold = Integer.parseInt(popThresholdStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -popt (popularity threshold) requires an integer as argument");
                System.err.println("Typing [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("Typing", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("outputt")) {
            try {
                String outputThresholdStr = cli.getOptionValue("outputt");
                outputThreshold = Double.parseDouble(outputThresholdStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -outputt (output threshold) requires a double as argument");
                System.err.println("Typing [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("Typing", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("st")) {
            try {
                String stStr = cli.getOptionValue("st");
                supportThreshold = Integer.parseInt(stStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -st (support threshold) requires an integer as argument");
                System.err.println("*Classifier [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("*Classifier", options);
                System.exit(1);
            }
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

        if (cli.hasOption("heur")) {
            heuristics = cli.getOptionValues("heur");
        }

        String[] leftOverArgs = cli.getArgs();
        List<File> dataFiles = new ArrayList<>();

        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            System.err.println("Typing [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        //Load database
        for (int i = 0; i < leftOverArgs.length; ++i) {
            dataFiles.add(new File(leftOverArgs[i]));
        }

        KB dataSource = new SimpleTypingKB();


        if (cli.hasOption("d")) {
            delimiter = cli.getOptionValue("d");
        }
        
        if (cli.hasOption("w")) {
                Schema.typeRelation = "<P106>";
                Schema.typeRelationBS = KB.map(Schema.typeRelation);
                Schema.subClassRelation = "<P279>";
                Schema.subClassRelationBS = KB.map(Schema.subClassRelation);
                Schema.top = "<Q35120>";
                Schema.topBS = KB.map(Schema.top);
                delimiter = " ";
            }

        List<TypingHeuristic> typingHeuristics = new LinkedList<>();

        for (int i = 0; i < heuristics.length; i++) {
            switch (heuristics[i]) {
                case "std":
                    typingHeuristics.add(new StdConfHeuristic(dataSource, supportThreshold));
                    System.out.println("Standard Confidence heuristic selected");
                    break;
                case "rev":
                    typingHeuristics.add(new ReverseStdConfHeuristic(dataSource, supportThreshold));
                    System.out.println("Reverse Standard Confidence heuristic selected");
                    break;
                case "pop":
                    typingHeuristics.add(new PopularityHeuristic(dataSource, popularityThreshold, supportThreshold));
                    System.out.println("Popularity heuristic selected, parameter: " + popularityThreshold.toString());
                    break;
                case "cond":
                    typingHeuristics.add(new ConditionalProbability(dataSource));
                    System.out.println("Conditional Probability heuristic selected");
                    break;
                case "newf":
                    throw new UnsupportedOperationException("New semantic => broken code");
//        	if (!cli.hasOption("snapshot")) {
//        		System.err.println("New heuristic requires a snapshot file");
//                formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
//                System.exit(1);
//        	}
//        	try {
//        		KB diffDB = new KB();
//        		diffDB.loadDiff(new File(cli.getOptionValue("snapshot")), dataSource, Schema.typeRelationBS);
//        		typingHeuristic = new StdConfHeuristic(diffDB);
//        		System.out.println("New Facts heuristic selected");
//        	} 
//        	catch (Exception e) {
//        		System.err.println("Unable to load snapshot file");
//                e.printStackTrace();
//                System.exit(1);
//        	}
//        	break;
                case "all":
                    typingHeuristics.add(new StdConfHeuristic(dataSource, supportThreshold));
                    typingHeuristics.add(new ReverseStdConfHeuristic(dataSource, supportThreshold));
                    typingHeuristics.add(new PopularityHeuristic(dataSource, popularityThreshold, supportThreshold));
                    typingHeuristics.add(new ConditionalProbability(dataSource));
                    typingHeuristics.add(new Spread(dataSource));
                    typingHeuristics.add(new HSpread(dataSource));
                    typingHeuristics.add(new Amplification(dataSource));
                    typingHeuristics.add(new TrueType(dataSource));
                    System.out.println("All heuristics selected, parameter for popularity: " + popularityThreshold.toString());
                    if (!cli.hasOption("snapshot")) // Bit of hacking
                    {
                        break;
                    }
                case "newe":
                    if (!cli.hasOption("snapshot")) {
                        System.err.println("New heuristic requires a snapshot file");
                        formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
                        System.exit(1);
                    }
                    try {
                        KB diffDB = new KB();
                        diffDB.setDelimiter(delimiter);
                        diffDB.load(new File(cli.getOptionValue("snapshot")));
                        if (cli.hasOption("t")) {
                            diffDB.load(new File(cli.getOptionValue("t")));
                        }
                        IntSet oldEntities = diffDB.selectDistinct(KB.map("?x"), TypingHeuristic.typeL(KB.map("?y"), KB.map("?x")));
                        TypingHeuristic newEntities = new StdConfHeuristic(dataSource.newEntitiesKB(oldEntities));
                        newEntities.name = "newEntities";
                        typingHeuristics.add(newEntities);
                    } catch (Exception e) {
                        System.err.println("Unable to load snapshot file");
                        e.printStackTrace();
                        System.exit(1);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Custom heuristics support not yet implemented");
            }
        }
        
        dataSource.setDelimiter(delimiter);
        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();

        List<int[]> query = new ArrayList<>(1);
        query.add(KB.triple(KB.map("?x"), KB.map("?y"), KB.map("?z")));
        IntSet relations = dataSource.getRelationSet(); //dataSource.selectDistinct(KB.map("?y"), query);
        relations.remove(Schema.typeRelationBS);
        relations.remove(PopularityHeuristic.popularityRelationBS);
        relations.remove(TrueType.trueTypeBS);

        query.get(0)[1] = Schema.typeRelationBS;
        IntSet classes = dataSource.getClassSet(); //dataSource.selectDistinct(KB.map("?z"), query);
        List<int[]> clause = new LinkedList<>();

        if (typingHeuristics.isEmpty()) {
            System.out.println("Default heuristic: MRC");
            TypingHeuristic spread = new Spread(dataSource);
            TypingHeuristic amplification = new Amplification(dataSource);
            double mrc_score = 0;
            int mrc = KB.map("NONE");
            double mrc_score2 = 0;
            int mrc2 = KB.map("NONE");
            for (int r : relations) {
                clause.add(KB.triple(KB.map("?x"), r, KB.map("?y")));
                Double prob = 0.0;
                List<Double> probs = new LinkedList<>();
                for (int c : classes) {
                    if ((prob = spread.evaluate(c, clause, KB.map("?x")) * amplification.evaluate(c, clause, KB.map("?x"))) > mrc_score) {
                        mrc = c;
                        mrc_score = prob;
                    }
                    if ((prob = spread.evaluate(c, clause, KB.map("?y")) * amplification.evaluate(c, clause, KB.map("?y"))) > mrc_score2) {
                        mrc2 = c;
                        mrc_score2 = prob;
                    }
                }
                System.out.println("MRC(" + KB.unmap(r) + ")\t=" + KB.unmap(mrc));
                System.out.println("MRC(" + KB.unmap(r) + "-1)\t=" + KB.unmap(mrc2));
                mrc_score = mrc_score2 = 0;
                mrc = KB.map("NONE");
                mrc2 = KB.map("NONE");
                clause.clear();
            }
        } else {
            BlockingQueue queryQ = new LinkedBlockingQueue();
            for (TypingHeuristic th : typingHeuristics) {
                for (int r : relations) {
                    queryQ.add(new Triple<>(KB.triples(KB.triple(KB.map("?x"), r, KB.map("?y"))),
                            KB.map("?x"), th));
                    queryQ.add(new Triple<>(KB.triples(KB.triple(KB.map("?x"), r, KB.map("?y"))),
                            KB.map("?y"), th));
                }
            }
            for (int i = 0; i < nThreads; i++) {
                queryQ.add(new Triple<>(Collections.EMPTY_LIST, KB.map("STOP"), null));
            }

            // Let's thread !
            long timeStamp3 = System.currentTimeMillis();
            List<Thread> threadList = new ArrayList<>(nThreads);
            for (int i = 0; i < nThreads; i++) {
                threadList.add(new TypingMT(queryQ, classes, outputThreshold));
            }

            for (Thread thread : threadList) {
                thread.start();
            }

            for (Thread thread : threadList) {
                thread.join();
            }
            long timeStamp4 = System.currentTimeMillis();
            System.out.println("Processing done with " + Integer.toString(nThreads) + " threads in " + Long.toString(timeStamp4 - timeStamp3) + "ms.");
        }
    }
}
