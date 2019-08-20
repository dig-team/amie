package amie.typing.classifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.typing.classifier.SeparationClassifier.ParsedArguments;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class MinMaxHierarchicalClassifier extends MinCutClassifier {

	public MinMaxHierarchicalClassifier(KB source) {
		super(source);
		// TODO Auto-generated constructor stub
	}

	public MinMaxHierarchicalClassifier(KB source, File countsFile) {
		super(source, countsFile);
		// TODO Auto-generated constructor stub
	}

	public MinMaxHierarchicalClassifier(KB source, File typeCountFile,
			File typeIntersectionCountFile) {
		super(source, typeCountFile, typeIntersectionCountFile);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void classify(Int2ObjectMap<Int2DoubleMap> statistics) {
		// TODO Auto-generated method stub
		Int2DoubleMap result = new Int2DoubleOpenHashMap();
		for (int t1 : statistics.keySet()) {
			for (int t2 : statistics.get(t1).keySet()) {
				if (Double.isNaN(statistics.get(t2).get(t1)) || classIntersectionSize.get(t1).get(t2) == classSize.get(t2))
					continue;
				result.put(t1, (!result.containsKey(t1) || result.get(t1) < statistics.get(t2).get(t1)) ? statistics.get(t2).get(t1) : result.get(t1));
			}
		}
		for (int t1 : result.keySet()) {
			System.out.println(KB.unmap(t1) + "\t" + Double.toString(result.get(t1)));
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
        MinMaxHierarchicalClassifier mmhc;
        if (pa.countFile != null) {
        	if(pa.countIntersectionFile != null) {
        		mmhc = new MinMaxHierarchicalClassifier(dataSource, pa.countFile, pa.countIntersectionFile);
        	} else {
        		mmhc = new MinMaxHierarchicalClassifier(dataSource, pa.countFile);
        	}
        } else {
            System.out.print("Computing classes overlap...");
            mmhc = new MinMaxHierarchicalClassifier(dataSource);
        	System.out.println(" Done.");
        }
        mmhc.classify(mmhc.computeStatistics(pa.query, pa.variable, pa.classSizeThreshold, pa.supportThreshold));

	}

}
