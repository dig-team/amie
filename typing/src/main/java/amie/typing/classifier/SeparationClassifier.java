package amie.typing.classifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;
import amie.data.Schema;
import amie.data.U;
import amie.typing.heuristics.TypingHeuristic;

public class SeparationClassifier {
	
	private KB db;
	public IntHashMap<ByteString> classSize;
	public Map<ByteString, IntHashMap<ByteString>> classIntersectionSize;
	
	protected double getStandardConfidenceWithThreshold(List<ByteString[]> head, List<ByteString[]> body, ByteString variable, int threshold, boolean unsafe) {
		List<ByteString[]> bodyC = (unsafe) ? new LinkedList<>(body) : body;
		long bodySize = db.countDistinct(variable, bodyC);
		bodyC.addAll(head);
		long support = db.countDistinct(variable, bodyC);
		if (support < threshold || bodySize == 0)
			return 0;
		return ((double) support) / bodySize;
	}
	
	/**
	 * Get all classes C where C => query has a support at least superior than a certain threshold. 
	 * @param query
	 * @param variable
	 * @param supportThreshold
	 * @return
	 */
	public Set<ByteString> getRelevantClasses(List<ByteString[]> query, ByteString variable, int supportThreshold) {
		Set<ByteString> result = new LinkedHashSet<>();
		for (ByteString c : classSize) {
			if (getStandardConfidenceWithThreshold(TypingHeuristic.typeL(c, variable), query, variable, supportThreshold, true) != 0) {
				result.add(c);
			}
		}
		return result;
	}
	
	public Map<ByteString, Map<ByteString, Double>> computeStatistics(List<ByteString[]> query, ByteString variable, int classSizeThreshold, int supportThreshold) {
		Map<ByteString, Map<ByteString, Double>> result = new LinkedHashMap<>();
		Set<ByteString> relevantClasses = getRelevantClasses(query, variable, supportThreshold);
		
		for (ByteString class1 : relevantClasses) {
			int c1size = classSize.get(class1);
			
			if (c1size < classSizeThreshold) {
				continue;
			}
			
			Map<ByteString, Double> r = new LinkedHashMap<>();
			List<ByteString[]> clause = TypingHeuristic.typeL(class1, variable);
			clause.addAll(query);
			
			for (ByteString class2 : relevantClasses) {
				
				assert(clause.size() == query.size() + 1);
				if (class1 == class2) continue;
				if (classSize.get(class2) < classSizeThreshold) {
					// Ensure the symmetry of the output.
					continue;
				}
				if (!classIntersectionSize.containsKey(class1) || !classIntersectionSize.get(class1).contains(class2)) {
					continue;
				}
				
				int c1c2size = classIntersectionSize.get(class1).get(class2);

				if (c1c2size < classSizeThreshold || c1size - c1c2size < classSizeThreshold) {
					/*
					 *  Ensure the symmetry of the output.
					 *  
					 *  If the sizes of c1 inter c2 or c1 minus c2 are not representative then c1 is not influenced by c2.
					 *  Still, it is useful in the case where c1 minus c2 is not representative because c2 minus c1 might be.
					 *  
					 *  Side-effect 1: if c1 in c2, the original metric would have output +Inf * 0.
					 *  
					 *  Cases:
					 *  * if score(c2,c1) << 1 then c2 cannot convene OK.
					 *  * if score(c2,c1) ~= 1 then both can (as c1 is a subclass) OK.
					 *  * if score(c2,c1) >> 1 then c2 minus c1 can OK.
					 *  
					 *  Side-effect 2: if c1 inter c2 is null, the original metric would have output 0 * +Inf.
					 *  
					 *  In this case, both side will output 1, ie, they both can OK.
					 */
					r.put(class2, 1.);
				} else {
					// c1 , q => c2
					double s = getStandardConfidenceWithThreshold(TypingHeuristic.typeL(class2, variable), clause, variable, -1, true);
					r.put(class2, ((double) c1c2size / (c1size - c1c2size)) * (1.0-s)/s);
				}
			}
			
			if (!r.isEmpty()) {
				result.put(class1, r);
			}
		}
		return result;
	}
	
	public void classify(Map<ByteString, Map<ByteString, Double>> statistics, double eliminationRatio) {
		// add an unionRatio ?
		double lratio = Math.log(eliminationRatio);
		for (ByteString class1 : statistics.keySet()) {
			for (ByteString class2 : statistics.get(class1).keySet()) {
				if (class1.compareTo(class2) >= 0) continue;
				String header = "(" + class1.toString() + "," + class2.toString() + "): ";
				double s1 = Math.log(statistics.get(class1).get(class2));
				double s2 = Math.log(statistics.get(class2).get(class1));
				String ss1 = Double.toString(statistics.get(class1).get(class2));
				String ss2 = Double.toString(statistics.get(class2).get(class1));
				if(s1 > lratio) {
					System.out.println(header + "!1, !2, 1-2\t" + ss1);
				}
				if(s2 > lratio) {
					System.out.println(header + "!1, !2, 2-1\t" + ss2);
				}
				if(s1 < -lratio && s2 < -lratio) {
					System.out.println(header + "!1, !2, 1^2\t" + ss1 + "," + ss2);
				}
				if(s1 < lratio && s1 > -lratio) {
					if(s2 < lratio && s2 > -lratio) {
						System.out.println(header + "1U2\t" + ss1 + "," + ss2);
					}
					if(s2 < -lratio) {
						System.out.println(header + "1, !2\t" + ss1 + "," + ss2);
					}
				}
				if(s2 < lratio && s2 > -lratio && s1 < -lratio) {
					System.out.println(header + "!1, 2\t" + ss1 + "," + ss2);
				}
			}
		}
	}
	
	public SeparationClassifier(KB source) {
		db = source;
		classSize = Schema.getTypesCount(source);
		classIntersectionSize = Schema.getTypesIntersectionCount(source);
	}
	
	public static void main(String[] args) throws IOException {
		
		CommandLine cli = null;
		Integer classSizeThreshold = 50;
		Integer supportThreshold = 50;
		double eliminationRatio = 1.5;
		String delimiter = "\t";
		
		HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();
        Option  popularityOpt = OptionBuilder.withArgName("cst")
                .hasArg()
                .withDescription("Class size Threshold. Default: 50")
                .create("cst");
        Option outputThresholdOpt = OptionBuilder.withArgName("st")
                .hasArg()
                .withDescription("Support threshold. Default: 50")
                .create("st");
        Option eliminationRatioOpt = OptionBuilder.withArgName("er")
                .hasArg()
                .withDescription("Elimination ratio. Default: 1.5")
                .create("er");
        Option delimiterOpt = OptionBuilder
        		.hasArg()
        		.withDescription("Delimiter in KB files")
        		.create("d");
        Option typeRelationOpt = OptionBuilder.withArgName("typeRelation")
                .hasArg()
                .withDescription("Type relation used in this KB. Default: rdf:type")
                .create("tr");
        Option queryOpt = OptionBuilder.withArgName("query")
                .hasArg()
                .withDescription("Queried attribute [-1]")
                .create("q");
        
        options.addOption(delimiterOpt);
        options.addOption(popularityOpt);
        options.addOption(outputThresholdOpt);
        options.addOption(typeRelationOpt);
        options.addOption(queryOpt);
        
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }
        
        if (cli.hasOption("cst")) {
        	try {
        		String cstStr = cli.getOptionValue("cst");
        		classSizeThreshold = Integer.parseInt(cstStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -cst (class size threshold) requires an integer as argument");
                System.err.println("SeparationClassifier [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("SeparationClassifier", options);
                System.exit(1);
            }
        }
        
        if (cli.hasOption("st")) {
        	try {
        		String stStr = cli.getOptionValue("st");
        		supportThreshold = Integer.parseInt(stStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -st (support threshold) requires an integer as argument");
                System.err.println("SeparationClassifier [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("SeparationClassifier", options);
                System.exit(1);
            }
        }
        
        if (cli.hasOption("er")) {
        	try {
        		String erStr = cli.getOptionValue("er");
        		eliminationRatio = Double.parseDouble(erStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -er (elimination ratio) requires a double as argument");
                System.err.println("Typing [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("Typing", options);
                System.exit(1);
            }
        }
        if (cli.hasOption("tr")) {
            Schema.typeRelation = cli.getOptionValue("tr");
            Schema.typeRelationBS = ByteString.of(Schema.typeRelation);
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
        
        if (!cli.hasOption("q")) {
        	System.err.println("Argument query is required");
        	formatter.printHelp("Typing", options);
            System.exit(1);
        }
        
        String[] attr = cli.getOptionValue("q").split("-1");
        List<ByteString[]> query = KB.triples(KB.triple("?x", attr[0], "?y"));
        ByteString variable = (attr.length == 1) ? ByteString.of("?x") : ByteString.of("?y");
        
        dataSource.setDelimiter(delimiter);
        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();
        
        System.out.print("Computing classes overlap...");
        SeparationClassifier sc = new SeparationClassifier(dataSource);
        System.out.println(" Done.");
        sc.classify(sc.computeStatistics(query, variable, classSizeThreshold, supportThreshold), eliminationRatio);
	}

}
