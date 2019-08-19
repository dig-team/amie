package amie.typing.classifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javatools.datatypes.Integer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import amie.data.KB;
import amie.data.Schema;
import amie.typing.classifier.SeparationClassifier.ParsedArguments;

public class MinCutClassifier extends SeparationClassifier {

	public MinCutClassifier(KB source) {
		super(source);
		// TODO Auto-generated constructor stub
	}
	
	public MinCutClassifier(KB source, File cf) {
		super(source, cf);
		// TODO Auto-generated constructor stub
	}
	
	public MinCutClassifier(KB source, File tcf, File ticf) {
		super(source, tcf, ticf);
		// TODO Auto-generated constructor stub
	}

	public void classify(Int2ObjectMap<Int2ObjectMap<Double>> statistics) {
		SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge>  graph = 
	            new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class); 
		for (int t1 : statistics.keySet()) {
			graph.addVertex(t1);
			for(int t2 : statistics.get(t1).keySet()) {
				if (Double.isNaN(statistics.get(t2).get(t1))) { continue; }
				graph.addVertex(t2);
				DefaultWeightedEdge e = graph.addEdge(t1, t2);
				graph.setEdgeWeight(e, statistics.get(t2).get(t1));
			}
		}
		PushRelabelMFImpl<Integer, DefaultWeightedEdge> mf = new PushRelabelMFImpl<>(graph);
		IntSet result = Collections.emptySet();
		double mcMinVal = Double.POSITIVE_INFINITY, mcVal;
		for (int t1 : graph.vertexSet()) {
			for (int t2 : graph.vertexSet()) {
				if (t1 == t2) continue;
				if ((mcVal = mf.calculateMinCut(t1, t2)) < mcMinVal) {
					mcMinVal = mcVal;
					result = mf.getSourcePartition();
				}
			}
		}
		for (int r : result) {
			System.out.println(r.toString());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		
		CommandLine cli = null;
		
		HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = getOptions();
        
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }
        
        ParsedArguments pa = new ParsedArguments(cli, formatter, options);
        
        List<File> dataFiles = new ArrayList<>();

        //Load database
        for (int i = 0; i < pa.leftOverArgs.length; ++i) {
                dataFiles.add(new File(pa.leftOverArgs[i]));
        }
        
        KB dataSource = new KB();
        
        dataSource.setDelimiter(pa.delimiter);
        dataSource.load(dataFiles);
        
        //Load classifier
        MinCutClassifier mcc;
        if (pa.countFile != null) {
        	if(pa.countIntersectionFile != null) {
        		mcc = new MinCutClassifier(dataSource, pa.countFile, pa.countIntersectionFile);
        	} else {
        		mcc = new MinCutClassifier(dataSource, pa.countFile);
        	}
        } else {
            System.out.print("Computing classes overlap...");
        	mcc = new MinCutClassifier(dataSource);
        	System.out.println(" Done.");
        }
        mcc.classify(mcc.computeStatistics(pa.query, pa.variable, pa.classSizeThreshold, pa.supportThreshold));
	}

}
