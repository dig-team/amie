package amie.typing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javatools.datatypes.ByteString;

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
import amie.typing.heuristics.*;

public class Typing {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		CommandLine cli = null;
		String heuristic = "std";
		Integer popularityThreshold = 50;
		double outputThreshold = 0.95;
		String delimiter = "\t";
		
        Schema.typeRelation = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
        Schema.typeRelationBS = ByteString.of(Schema.typeRelation);
		
		HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();
        Option heuristicOpt = OptionBuilder.withArgName("heuristic")
                .hasArg()
                .withDescription("Heuristic to use among: pop, *std*, rev")
                .create("heur");
        Option  popularityOpt = OptionBuilder.withArgName("popularity-threshold")
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
        
        options.addOption(delimiterOpt);
        options.addOption(heuristicOpt);
        options.addOption(popularityOpt);
        options.addOption(outputThresholdOpt);
        options.addOption(snapshotOpt);
        options.addOption(cacheOpt);
        options.addOption(typesOpt);
        
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
        
        if (cli.hasOption("heur")) {
        	heuristic = cli.getOptionValue("heur");
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
        
        KB dataSource = new KB();
        
        if (cli.hasOption("c"))
        	dataSource.countCacheEnabled = true;
        
        if (cli.hasOption("d")) {
        	delimiter = cli.getOptionValue("d");
        }
        
        dataSource.setDelimiter(delimiter);
        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();
        
        List<TypingHeuristic> typingHeuristics = new LinkedList<>();
        TypingHeuristic typingHeuristic= new StdConfHeuristic(dataSource);
        
        switch (heuristic) {
        case "std" :
        	System.out.println("Standard Confidence heuristic selected");
    		break;
        case "rev" :
            typingHeuristic = new ReverseStdConfHeuristic(dataSource);
            System.out.println("Reverse Standard Confidence heuristic selected");
            break;
        case "pop" :
        	typingHeuristic = new PopularityHeuristic(dataSource, popularityThreshold);
        	System.out.println("Popularity heuristic selected, parameter: "+popularityThreshold.toString());
        	break;
        case "cond":
        	typingHeuristic = new ConditionalProbability(dataSource);
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
        	typingHeuristics.add(new StdConfHeuristic(dataSource));
        	typingHeuristics.add(new ReverseStdConfHeuristic(dataSource));
        	typingHeuristics.add(new PopularityHeuristic(dataSource, popularityThreshold));
        	typingHeuristics.add(new ConditionalProbability(dataSource));
        	typingHeuristics.add(new Spread(dataSource));
        	typingHeuristics.add(new HSpread(dataSource));
        	typingHeuristics.add(new Amplification(dataSource));
        	typingHeuristics.add(new TrueType(dataSource));
        	System.out.println("All heuristics selected, parameter for popularity: "+popularityThreshold.toString());
        	if (!cli.hasOption("snapshot"))		// Bit of hacking
        		break;
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
        		if (cli.hasOption("t"))
        			diffDB.load(new File(cli.getOptionValue("t")));
        		Set<ByteString> oldEntities = diffDB.selectDistinct(ByteString.of("?x"), TypingHeuristic.typeL(ByteString.of("?y"), ByteString.of("?x")));
        		TypingHeuristic newEntities = new StdConfHeuristic(dataSource.newEntitiesKB(oldEntities));
        		newEntities.name = "newEntities";
        		if (heuristic.equals("all")) {	 // Consequence of the bit of hacking
        			typingHeuristics.add(newEntities);
        		} else {
        			typingHeuristic = newEntities;
        			System.out.println("New Entities heuristic selected");
        		}
        	} 
        	catch (Exception e) {
        		System.err.println("Unable to load snapshot file");
                e.printStackTrace();
                System.exit(1);
        	}
        	break;
        case "MRC":
        	System.out.println("MRC heuristic selected");
        	break;
        default:
        	throw new UnsupportedOperationException("Custom heuristics support not yet implemented");
        }
        
        List<ByteString[]> query = new ArrayList<>(1);
        query.add(KB.triple(ByteString.of("?x"), ByteString.of("?y"), ByteString.of("?z")));
        Set<ByteString> relations = dataSource.selectDistinct(ByteString.of("?y"), query);
        relations.remove(Schema.typeRelationBS);
        relations.remove(PopularityHeuristic.popularityRelationBS);
        relations.remove(TrueType.trueTypeBS);
        for(ByteString r : relations) {
        	System.err.println(r.toString());
        }
        
        query.get(0)[1] = Schema.typeRelationBS;
        Set<ByteString> classes = dataSource.selectDistinct(ByteString.of("?z"), query);
        List<ByteString[]> clause = new LinkedList<>();

        TypingHeuristic spread = new Spread(dataSource);
        TypingHeuristic amplification = new Amplification(dataSource);
        double mrc_score = 0;
        ByteString mrc = ByteString.of("NONE");
        double mrc_score2 = 0;
        ByteString mrc2 = ByteString.of("NONE");
        
        for (ByteString r: relations) {
        	clause.add(KB.triple(ByteString.of("?x"), r, ByteString.of("?y")));
        	Double prob = 0.0;
        	List<Double> probs = new LinkedList<>();
        	for (ByteString c : classes) {
        		if (!c.toString().contains("dbpedia.org"))
        			continue;
        		if (heuristic.equals("MRC")) {
        			if ((prob = spread.evaluate(c, clause, ByteString.of("?x"))*amplification.evaluate(c, clause, ByteString.of("?x"))) > mrc_score) {
        				mrc = c;
        				mrc_score = prob;
        			}
        			if ((prob = spread.evaluate(c, clause, ByteString.of("?y"))*amplification.evaluate(c, clause, ByteString.of("?y"))) > mrc_score2) {
        				mrc2 = c;
        				mrc_score2 = prob;
        			}
        			continue;
        		}
        		if (!typingHeuristics.isEmpty()) {
        			
        			for (TypingHeuristic th : typingHeuristics) {
        				System.out.println(th.name + "\t" + c.toString() + "\t" + r.toString() + "\t" + th.evaluate(c, clause, ByteString.of("?x")));
        			}
        			for (TypingHeuristic th : typingHeuristics) {
        				System.out.println(th.name + "\t" + c.toString() + "\t" + r.toString()+"-1" + "\t" + th.evaluate(c, clause, ByteString.of("?y")));
        			}
        		} else {
        		if ((prob = typingHeuristic.evaluate(c, clause, ByteString.of("?x"))) > outputThreshold)
        			System.out.println(c.toString() + "\t" + r.toString() + "\t" + prob.toString());
        		if ((prob = typingHeuristic.evaluate(c, clause, ByteString.of("?y"))) > outputThreshold)
        			System.out.println(c.toString() + "\t" + r.toString()+"-1" + "\t" + prob.toString());
        		}
        	}
        	if (heuristic.equals("MRC")) {
        		System.out.println("MRC("+r.toString()+")\t=" + mrc.toString());
        		System.out.println("MRC("+r.toString()+"-1)\t=" + mrc2.toString());
        		mrc_score = mrc_score2 = 0;
        		mrc = ByteString.of("NONE");
        		mrc2 = ByteString.of("NONE");
        	}
        	clause.clear();
        }
	}

}
