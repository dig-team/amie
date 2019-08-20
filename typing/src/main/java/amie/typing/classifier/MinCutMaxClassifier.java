package amie.typing.classifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import javatools.datatypes.Pair;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.typing.classifier.SeparationClassifier.ParsedArguments;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class MinCutMaxClassifier extends SeparationClassifier {

	public MinCutMaxClassifier(KB source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	public MinCutMaxClassifier(KB source, File countsFile) {
		super(source, countsFile);
		// TODO Auto-generated constructor stub
	}

	public MinCutMaxClassifier(KB source, File typeCountFile,
			File typeIntersectionCountFile) {
		super(source, typeCountFile, typeIntersectionCountFile);
		// TODO Auto-generated constructor stub
	}

	public static <K extends Comparable<? super K>, V extends Comparable<? super V>> LinkedList<Map.Entry<K, V>> linkSortMap(Map<K, V> mmap, final Comparator<? super V> comparator) {
		LinkedList<Map.Entry<K, V>> r = new LinkedList<>();
		for(Map.Entry<K, V> e : mmap.entrySet()) {
			r.add(e);
		}
		Collections.sort(r, new Comparator<Map.Entry<K, V>>() { 
			public int compare(Map.Entry<K, V> e1, Map.Entry<K,V> e2) {
				if (comparator.compare(e1.getValue(), e2.getValue()) == 0) return e1.getKey().compareTo(e2.getKey());
				return comparator.compare(e1.getValue(), e2.getValue());
			}
		});
		return r;
	}
	
	private Pair<Integer, Double> getMaxEdge(Int2ObjectMap<LinkedList<Map.Entry<Integer, Double>>> graph, IntSet CV) {
		Double maxD = Double.NEGATIVE_INFINITY;
		int maxN = 0;
		for(int t : CV) {
			LinkedList<Map.Entry<Integer, Double>> tt = graph.get(t);
                        if(tt == null) continue;
			while(!tt.isEmpty()) {
				Map.Entry<Integer, Double> tmaxP = tt.peek();
				if (CV.contains(tmaxP.getKey())) {
					tt.remove();
					continue;
				}
				if (tmaxP.getValue() > maxD) {
					maxD = tmaxP.getValue();
					maxN = tmaxP.getKey();
				}
				break;
			}
		}
                if (maxN != 0) System.err.println(KB.unmap(maxN) + " " + Double.toString(maxD));
		return new Pair<>(maxN, maxD);
	}
	
	public IntSet t_MinCutMax(Int2ObjectMap<Int2DoubleMap> statistics, int t) {
		Int2ObjectMap<LinkedList<Map.Entry<Integer, Double>>> smm = new Int2ObjectOpenHashMap<>();
		for (int t1 : statistics.keySet()) {
			smm.put(t1, linkSortMap(statistics.get(t1), Collections.reverseOrder()));
		}
		// Implement t-MinCutMax algorithm
		IntSet V = new IntOpenHashSet(statistics.keySet());
		IntSet CV = new IntOpenHashSet();
		V.remove(t);
		CV.add(t);
		IntSet S = null;
		Double currentMin = Double.POSITIVE_INFINITY;
		while(!V.isEmpty()) {
			Pair<Integer, Double> cMax = getMaxEdge(smm, CV);
                        if (cMax.first == null) break;
			if (cMax.second < currentMin) {
				currentMin = cMax.second;
				S = new IntOpenHashSet(V);
			}
			CV.add(cMax.first);
			V.remove(cMax.first);
		}
                return S;
        }
                
        public void classify(Int2ObjectMap<Int2DoubleMap> statistics) {         
            for(int s : t_MinCutMax(statistics, KB.map("owl:Thing"))) {
                System.out.println(KB.unmap(s));
            }
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
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
        MinCutMaxClassifier mcmc;
        if (pa.countFile != null) {
        	if(pa.countIntersectionFile != null) {
        		mcmc = new MinCutMaxClassifier(dataSource, pa.countFile, pa.countIntersectionFile);
        	} else {
        		mcmc = new MinCutMaxClassifier(dataSource, pa.countFile);
        	}
        } else {
            System.out.print("Computing classes overlap...");
        	mcmc = new MinCutMaxClassifier(dataSource);
        	System.out.println(" Done.");
        }
        mcmc.classify(mcmc.computeStatistics(pa.query, pa.variable, pa.classSizeThreshold, pa.supportThreshold));
	}
}
