package amie.typing.classifier;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javatools.datatypes.ByteString;
import javatools.datatypes.Pair;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.typing.classifier.SeparationClassifier.ParsedArguments;

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
	
	private Pair<ByteString, Double> getMaxEdge(Map<ByteString, LinkedList<Map.Entry<ByteString, Double>>> graph, Set<ByteString> CV) {
		Double maxD = Double.NEGATIVE_INFINITY;
		ByteString maxN = null;
		for(ByteString t : CV) {
			LinkedList<Map.Entry<ByteString, Double>> tt = graph.get(t);
                        if(tt == null) continue;
			while(!tt.isEmpty()) {
				Map.Entry<ByteString, Double> tmaxP = tt.peek();
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
                if (maxN != null) System.err.println(maxN.toString() + " " + Double.toString(maxD));
		return new Pair<>(maxN, maxD);
	}
	
	public Set<ByteString> t_MinCutMax(Map<ByteString, Map<ByteString, Double>> statistics, ByteString t) {
		Map<ByteString, LinkedList<Map.Entry<ByteString, Double>>> smm = new HashMap<>();
		for (ByteString t1 : statistics.keySet()) {
			smm.put(t1, linkSortMap(statistics.get(t1), Collections.reverseOrder()));
		}
		// Implement t-MinCutMax algorithm
		Set<ByteString> V = new HashSet<>(statistics.keySet());
		Set<ByteString> CV = new LinkedHashSet<>();
		V.remove(t);
		CV.add(t);
		Set<ByteString> S = null;
		Double currentMin = Double.POSITIVE_INFINITY;
		while(!V.isEmpty()) {
			Pair<ByteString, Double> cMax = getMaxEdge(smm, CV);
                        if (cMax.first == null) break;
			if (cMax.second < currentMin) {
				currentMin = cMax.second;
				S = new HashSet<>(V);
			}
			CV.add(cMax.first);
			V.remove(cMax.first);
		}
                return S;
        }
                
        public void classify(Map<ByteString, Map<ByteString, Double>> statistics) {         
            for(ByteString s : t_MinCutMax(statistics, ByteString.of("owl:Thing"))) {
                System.out.println(s.toString());
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
