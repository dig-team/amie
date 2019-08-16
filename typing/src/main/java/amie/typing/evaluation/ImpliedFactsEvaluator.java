/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.evaluation;

import amie.data.KB;
import amie.data.Schema;
import static amie.typing.classifier.SeparationClassifier.getOptions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javatools.datatypes.ByteString;
import javatools.datatypes.Pair;
import javatools.filehandlers.FileLines;
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
public class ImpliedFactsEvaluator {

    private KB db;
    public IntSet queried;
    public Set<String> querySet;
    public BlockingQueue<Pair<ByteString, String>> queryQ = new LinkedBlockingQueue<>();
    public BlockingQueue<Pair<Pair<ByteString, String>, ImpliedFacts>> resultQ = new LinkedBlockingQueue<>();

    public static final ByteString gsRelation = ByteString.of("<inGoldStandardOf>");

    public static ByteString resultWithThresholdRelation(String method) {
        return ByteString.of("<inResult_" + method + ">");
    }

    public static ByteString rwtr(String t) {
        return resultWithThresholdRelation(t);
    }

    public ImpliedFactsEvaluator(KB db) {
        this.db = db;
        queried = new IntOpenHashSet();
        querySet = new HashSet<>();
    }
    
//    public ImpliedFactsEvaluator(KB db, Map<ByteString, IntSet> goldStandard,
//            Map<ByteString, Map<String, IntSet>> results) {
//        this.db = db;
//        queried = new IntOpenHashSet(goldStandard.keySet());
//        for (ByteString q : queried) {
//            for (ByteString gsResult : goldStandard.get(q)) {
//                for (ByteString e : Schema.getAllEntitiesForType(db, gsResult)) {
//                    db.add(e, gsRelation, q);
//                }
//            }
//            if (!results.containsKey(q)) { continue; }
//            for (String t : results.get(q).keySet()) {
//                queryQ.add(new Pair<>(q, t));
//                for (ByteString rtClass : results.get(q).get(t)) {
//                    for (ByteString e : Schema.getAllEntitiesForType(db, rtClass)) {
//                        db.add(e, rwtr(t), q);
//                    }
//                }
//            }
//        }
//    }
    
    public void addGS(ByteString relation, IntSet classes) {
        for (ByteString rtClass : classes) {
            addGS(relation, rtClass);
        }
    }
        
    public void addGS(ByteString relation, ByteString rtClass) {
        queried.add(relation);
        for (ByteString e : Schema.getAllEntitiesForType(db, rtClass)) {
                db.add(e, gsRelation, relation);
        }
    }
    
    public void addResult(ByteString relation, String method, IntSet classes) {
        for (ByteString rtClass : classes) {
            addResult(relation, method, classes);
        }
    }
        
    public void addResult(ByteString relation, String method, ByteString rtClass) {
        querySet.add(method);
        for (ByteString e : Schema.getAllEntitiesForType(db, rtClass)) {
                db.add(e, rwtr(method), relation);
        }
    }
    
    /**
     * TP: True Positive
     * PS: Predicted size (TP + FP)
     * GS: Gold Standard size (TP + FN)
     * NFx: same with only new facts deduced.
     */
    public class ImpliedFacts {
        public long TP, PS, GS, NFTP, NFPS, NFGS;
        public ImpliedFacts(long tp, long ps, long gs, long nftp, long nfps, long nfgs) {
            TP=tp; PS=ps; GS=gs; 
            NFTP=nftp; NFPS=nfps; NFGS=nfgs; }
    }
    
    public ImpliedFacts computeImpliedFacts(ByteString query, String method) {
        throw new UnsupportedOperationException("No longer...");
//        if (!queried.contains(query)) {
//            //return new ImpliedFacts(0, 0, 0, 0);
//        }
//        final ByteString x = ByteString.of("?x");
//        List<ByteString[]> gsSizeQ = KB.triples(KB.triple(x, gsRelation, query));
//        List<ByteString[]> rtSizeQ = KB.triples(KB.triple(x, rwtr(method), query));
//        long gsSize = db.countDistinct(x, gsSizeQ);
//        long rtSize = db.countDistinct(x, rtSizeQ);
//        gsSizeQ.addAll(rtSizeQ);
//        long tp = db.countDistinct(x, gsSizeQ);
//        String[] q = query.toString().split("-1");
//        if (q.length == 1) {
//            gsSizeQ.add(KB.triple(x, ByteString.of(query), ByteString.of("?y")));
//        } else {
//            gsSizeQ.add(KB.triple(ByteString.of("?y"), ByteString.of(q[0]), x));
//        }
//        long oldFacts = db.countDistinct(x, gsSizeQ);
//        //return new ImpliedFacts(tp, rtSize - tp, gsSize - tp, tp - oldFacts);
    }
    
    public class ImpliedFactsMTEvaluator extends Thread {
        @Override
        public void run() {
            Pair<ByteString, String> qP;
            while(true) {
                try {
                    qP = queryQ.take();
                    if (qP.first.equals(ByteString.of("STOP"))) break;
                    resultQ.put(new Pair<>(qP, computeImpliedFacts(qP.first, qP.second)));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ImpliedFactsEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void computeImpliedFactsMT(int nThreads) throws InterruptedException {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        nThreads = Math.max(1, Math.min(nThreads, nProcessors - 1));
        
        List<Thread> threadList = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            queryQ.add(new Pair<>(ByteString.of("STOP"), ""));
            threadList.add(new ImpliedFactsMTEvaluator());
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
    }
    
    public static Pair<ByteString, String> extractQueryArgs(String path) {
        String[] t = path.split("/");
        String filename = t[t.length - 1];
        filename = filename.replace("_cleaned", "");
        String[] fns = filename.split("_");
        if (fns.length == 2 && t.length > 1) {
            return new Pair<>(ByteString.of("<" + (fns[0].contains("-1")? (fns[0].replace("-1", "") + ">-1") : (fns[0] + ">"))), t[t.length - 2] + "_" + fns[1]);
        } else if (fns.length == 3) {
            return new Pair<>(ByteString.of("<" + (fns[1].contains("-1")? (fns[1].replace("-1", "") + ">-1") : (fns[1] + ">"))), fns[0] + "_" + fns[2]);
        } else {
            System.err.println("ERROR parsing \"" + path + "\"");
            return null;
        }
    }
    
    public static IntSet readClassFile(String path) throws IOException {
        IntSet res = new IntOpenHashSet();
        for (String line : new FileLines(new File(path), "UTF-8", null)) {
            res.add(ByteString.of(line.trim()));
        }
        return res;
    }
    
    public void readFile(String path) throws IOException {
        for (String line : new FileLines(new File(path), "UTF-8", null)) {
            String[] s = line.split("\t");
            if (s.length == 2) {
                addGS(ByteString.of(s[0]), ByteString.of(s[1]));
            } else if (s.length == 4) {
                addResult(ByteString.of(s[2]), s[0]+"_"+s[1], ByteString.of(s[3]));
            } else if (s.length == 5) {
                addResult(ByteString.of(s[3]), s[1]+"_"+s[2]+"_"+s[0], ByteString.of(s[4]));
            } else {
                throw new IllegalArgumentException("Not well formatted file");
            }
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<ByteString, IntSet> relation2classGS = new HashMap<>();
        

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
        options.addOption(gsfOpt);
        options.addOption(rsfOpt);
        options.addOption(coresOpt);
        options.addOption(delimiterOpt);
        
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
        KB dataSource = new KB();
        dataSource.setDelimiter(delimiter);
        dataSource.load(dataFiles);
        ImpliedFactsEvaluator eval = new ImpliedFactsEvaluator(dataSource);
        
        // Load the counts
        String[] gsFiles = cli.getOptionValues("gs");
        for (int i = 0; i < gsFiles.length; i++) {
            try {
                eval.readFile(gsFiles[i]);
            } catch (IllegalArgumentException e) {
                eval.addGS(ByteString.of("<" + gsFiles[i].split("_")[1] + ((gsFiles[i].split("_")[2].equals("y")) ? "-1" : "") + ">"), readClassFile(gsFiles[i]));
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
        
        System.out.println("Method\tClassifier\tParameter\tRelation\tTrue Positives\tPredicted Size\tGS Size\tNFTP\tNFPS\tNFGS\tP\tR\tF1\tNFP\tNFR\tNFF1");
        Pair<Pair<ByteString, String>, ImpliedFacts> result;
        while((result = eval.resultQ.poll()) != null) {
            ImpliedFacts s = result.second;
            String rString = String.join("\t", 
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
                    Double.toString((s.PS == 0)? ((s.GS == 0) ? 1.0 : 0.0) : 1.0 * s.TP / s.PS),
                    Double.toString((s.GS == 0)? 1.0 : 1.0 * s.TP / s.GS),
                    Double.toString((s.PS == 0)? ((s.GS == 0) ? 1.0 : 0.0) : 2.0 * s.TP / s.PS * s.TP / s.GS / (1.0 * s.TP / s.PS + 1.0 * s.TP / s.GS)),
                    Double.toString((s.NFPS == 0)? ((s.NFGS == 0) ? 1.0 : 0.0) : 1.0 * s.NFTP / s.NFPS),
                    Double.toString((s.NFGS == 0)? 1.0 : 1.0 * s.NFTP / s.NFGS),
                    Double.toString((s.NFPS == 0)? ((s.NFGS == 0) ? 1.0 : 0.0) : 2.0 * s.NFTP / s.NFPS * s.NFTP / s.NFGS / (1.0 * s.NFTP / s.NFPS + 1.0 * s.NFTP / s.NFGS)));
            System.out.println(rString);
        }
    }
}
