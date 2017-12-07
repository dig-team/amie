/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.evaluation;

import amie.data.Schema;
import amie.data.SimpleTypingKB;
import amie.data.WikidataSimpleTypingKB;
import static amie.typing.classifier.SeparationClassifier.getOptions;
import static amie.typing.evaluation.ImpliedFactsEvaluator.extractQueryArgs;
import static amie.typing.evaluation.ImpliedFactsEvaluator.readClassFile;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javatools.datatypes.ByteString;
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
public class SimpleImpliedFactsEvaluator extends ImpliedFactsEvaluator {
    
    private SimpleTypingKB db;
    public Map<ByteString, Set<ByteString>> gs;
    public Map<ByteString, Set<ByteString>> gsClasses;
    public Map<ByteString, Map<String, Set<ByteString>>> query2classes;
    
    public SimpleImpliedFactsEvaluator(SimpleTypingKB db) {
        super(db);
        this.db = db;
        this.gs = new HashMap<>();
        this.query2classes = new HashMap<>();
    }
    
    @Override
    public void addGS(ByteString relation, Set<ByteString> classes) {
        //System.err.println(relation);
        queried.add(relation);
        Set<ByteString> gsr = gs.get(relation);
        if (gsr == null) {
            gs.put(relation, gsr = new HashSet<>());
        }
        for (ByteString rtClass : classes) {
            System.err.println(rtClass);
            gsr.addAll(db.classes.get(rtClass));
        }
    }
    
    @Override
    public void addGS(ByteString relation, ByteString gsClass) {
        //System.err.println(relation);
        queried.add(relation);
        Set<ByteString> gsr = gs.get(relation);
        if (gsr == null) {
            gs.put(relation, gsr = new HashSet<>());
        }
        gsr.addAll(db.classes.get(gsClass));
    }
    
    @Override
    public void addResult(ByteString relation, String method, ByteString rtClass) {
        querySet.add(method);
        Map<String, Set<ByteString>> method2classes = query2classes.get(relation);
        if (method2classes == null) {
            query2classes.put(relation, method2classes = new HashMap<>());
        }
        Set<ByteString> classes = method2classes.get(method);
        if (classes == null) {
            method2classes.put(method, classes = new HashSet<>());
        }
        classes.add(rtClass);
    }
    
    @Override
    public void addResult(ByteString relation, String method, Set<ByteString> classes) {
        querySet.add(method);
        Map<String, Set<ByteString>> method2classes = query2classes.get(relation);
        if (method2classes == null) {
            query2classes.put(relation, method2classes = new HashMap<>());
        }
        method2classes.put(method, classes);
    }
    
    @Override
    public ImpliedFacts computeImpliedFacts(ByteString query, String method) {
        System.err.println("Computing " + query.toString() + ":" + method);
        if (!queried.contains(query)) {
            return new ImpliedFacts(0, 0, 0, 0, 0, 0);
        }
        long gsSize = gs.get(query).size();
        long oldGSFacts = SimpleTypingKB.countIntersection(gs.get(query), db.relations.get(query));
        if (!query2classes.containsKey(query) || !query2classes.get(query).containsKey(method)) {
            return new ImpliedFacts(0, 0, gsSize, 0, 0, gsSize - oldGSFacts);
        }
        Set<ByteString> rtSet = new HashSet<>((int) gsSize);
        for (ByteString c : query2classes.get(query).get(method)) {
            rtSet.addAll(db.classes.get(c));
        }
        long rtSize = rtSet.size();
        Set<ByteString> tpSet = new HashSet<>((rtSize < gsSize) ? rtSet : gs.get(query));
        tpSet.retainAll((rtSize >= gsSize) ? rtSet : gs.get(query));
        long tp = tpSet.size();
        long oldTPFacts = SimpleTypingKB.countIntersection(tpSet, db.relations.get(query));
        long oldPSFacts = SimpleTypingKB.countIntersection(rtSet, db.relations.get(query));
        return new ImpliedFacts(tp, rtSize, gsSize, tp - oldTPFacts, rtSize - oldPSFacts, gsSize - oldGSFacts);
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        
        CommandLine cli = null;
	HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = getOptions();
        
        Option gsfOpt = OptionBuilder.withArgName("gsFiles")
                .hasArgs()
                .withDescription("Gold Standard files")
                .create("gs");
        Option rsfOpt = OptionBuilder.withArgName("rsFiles")
                .hasArgs()
                .withDescription("Result files")
                .create("r");
        Option coresOpt = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores - 1"
                		+ "in the system if a higher value is provided.")
                .create("nc");
        Option delimiterOpt = OptionBuilder
                .hasArg()
                .withDescription("Delimiter in KB files")
                .create("d");
        Option outputOpt = OptionBuilder.withArgName("of")
                .hasArg()
                .withDescription("Output file. Default: stdout")
                .create("o");
        options.addOption(gsfOpt);
        options.addOption(rsfOpt);
        options.addOption(coresOpt);
        options.addOption(delimiterOpt);
        options.addOption(outputOpt);
        
	try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("IFEvaluator -gs ... -rs ... -- <TSV FILES>", options);
            System.exit(1);
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
                formatter.printHelp("IFEvaluator -gs ... -rs ... -- <TSV FILES>", options);
                System.exit(1);
            }
        }
        
        String delimiter = "\t";
        if (cli.hasOption("d")) {
            delimiter = cli.getOptionValue("d");
            if (cli.getOptionValue("d").equals("w")) {
                System.err.println("Wikidata setup");
                Schema.typeRelation = "<P106>";
                Schema.typeRelationBS = ByteString.of(Schema.typeRelation);
                Schema.subClassRelation = "<P279>";
                Schema.subClassRelationBS = ByteString.of(Schema.subClassRelation);
                Schema.top = "<Q35120>";
                Schema.topBS = ByteString.of(Schema.top);
                delimiter = " ";
            }
        }
        
        PrintStream output = System.out;
        if (cli.hasOption("o")) {
            try {
                output = new PrintStream(new File(cli.getOptionValue("o")));
            } catch (Exception e) {
                System.out.println("Unexpected exception setting up output file: " + e.getMessage());
                System.exit(1);
            }
        }
        
        // KB files
        List<File> dataFiles = new ArrayList<>();
        
        String[] leftOverArgs = cli.getArgs();
        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            formatter.printHelp("IFEvaluator -gs ... -rs ... -- <TSV FILES>", options);
            System.exit(1);
        }

        for (int i = 0; i < leftOverArgs.length; ++i) {
                dataFiles.add(new File(leftOverArgs[i]));
        }
        
        // Load the KB
        SimpleTypingKB dataSource;
        if (cli.hasOption("d") && cli.getOptionValue("d").equals("w")) {
            dataSource = new WikidataSimpleTypingKB();
        } else {
            dataSource = new SimpleTypingKB();
        }
        dataSource.setDelimiter(delimiter);
        dataSource.load(dataFiles);
        SimpleImpliedFactsEvaluator eval = new SimpleImpliedFactsEvaluator(dataSource);
        
        // Load the counts
        String[] gsFiles = cli.getOptionValues("gs");
        for (int i = 0; i < gsFiles.length; i++) {
            try {
                eval.readFile(gsFiles[i]);
            } catch (IllegalArgumentException e) {
                eval.addGS(ByteString.of("<" + gsFiles[i].split("_")[1] + ">" + ((gsFiles[i].split("_")[2].equals("y")) ? "-1" : "")), readClassFile(gsFiles[i]));
            }
        }
        
        String[] rsFiles = cli.getOptionValues("r");
        for (int i = 0; i < rsFiles.length; i++) {
            try {
                eval.readFile(rsFiles[i]);
            } catch (IllegalArgumentException e) {
                Pair<ByteString, String> rm = extractQueryArgs(rsFiles[i]);
                eval.addResult(rm.first, rm.second, readClassFile(rsFiles[i]));
            }
        }
        
        for (String method : eval.querySet) {
            for (ByteString relation : eval.queried) {
                eval.queryQ.add(new Pair<>(relation, method));
            }
        }
           
        
        System.err.println("Data loaded, beginning evaluation...");
        eval.computeImpliedFactsMT(nThreads);
        
        output.println("T\tMethod\tClassifier\tParameter\tRelation\tTrue Positives\tPredicted Size\tGS Size\tNFTP\tNFPS\tNFGS\tP\tR\tF1\tNFP\tNFR\tNFF1");
        Pair<Pair<ByteString, String>, ImpliedFacts> result;
        while((result = eval.resultQ.poll()) != null) {
            ImpliedFacts s = result.second;
            String rString = String.join("\t", 
                    (result.first.second.split("_").length >= 3) ? result.first.second.split("_")[2] : "",
                    result.first.second,
                    result.first.second.split("_")[0],
                    result.first.second.split("_")[1],
                    result.first.first,
                    Long.toString(s.TP),
                    Long.toString(s.PS),
                    Long.toString(s.GS),
                    Long.toString(s.NFTP),
                    Long.toString(s.NFPS),
                    Long.toString(s.NFGS),
                    Double.toString((s.PS == 0)? 1.0 : 1.0 * s.TP / s.PS),
                    Double.toString((s.GS == 0)? 1.0 : 1.0 * s.TP / s.GS),
                    Double.toString((s.PS == 0)? ((s.GS == 0) ? 1.0 : 0.0) : 2.0 * s.TP / s.PS * s.TP / s.GS / (1.0 * s.TP / s.PS + 1.0 * s.TP / s.GS)),
                    Double.toString((s.NFPS == 0)? 1.0 : 1.0 * s.NFTP / s.NFPS),
                    Double.toString((s.NFGS == 0)? 1.0 : 1.0 * s.NFTP / s.NFGS),
                    Double.toString((s.NFPS == 0)? ((s.NFGS == 0) ? 1.0 : 0.0) : 2.0 * s.NFTP / s.NFPS * s.NFTP / s.NFGS / (1.0 * s.NFTP / s.NFPS + 1.0 * s.NFTP / s.NFGS)));
            output.println(rString);
        }
    }
}
