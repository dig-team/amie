package amie.typing.classifier;

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
import amie.typing.heuristics.TypingHeuristic;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class SeparationClassifier {

    protected KB db;
    public Int2IntMap classSize;
    public Int2ObjectMap<Int2IntMap> classIntersectionSize;

    protected double getStandardConfidenceWithThreshold(List<int[]> head, List<int[]> body, int variable, int threshold, boolean unsafe) {
        long support, bodySize;
        
        if(db instanceof SimpleTypingKB) {
            SimpleTypingKB simpledb = (SimpleTypingKB) db;
            int relation;
            if (body.size() == 1) {
                relation = (body.get(0)[0] == (variable)) ? body.get(0)[1] : KB.map(KB.unmap(body.get(0)[1]) + "-1");
                bodySize = simpledb.countElements(relation);
                support = simpledb.countElements(relation, head.get(0)[2]);
            } else if (body.size() == 2) {
                relation = (body.get(1)[0] == (variable)) ? body.get(1)[1] : KB.map(KB.unmap(body.get(1)[1]) + "-1");
                return simpledb.typingStdConf(relation, body.get(0)[2], head.get(0)[2], threshold);
                //bodySize = simpledb.countElements(relation, body.get(0)[2]);
                //support = simpledb.countElements(relation, body.get(0)[2], head.get(0)[2]);
            } else {
                throw new UnsupportedOperationException("Simple KB can only deal with simple queries");
            }
        } else {
            List<int[]> bodyC = (unsafe) ? new LinkedList<>(body) : body;
            bodySize = db.countDistinct(variable, bodyC);
            bodyC.addAll(head);
            support = db.countDistinct(variable, bodyC);
        }
        
        
        if (support < threshold || bodySize == 0) {
            return 0;
        }
        return ((double) support) / bodySize;
    }

    /**
     * Get all classes C where C => query has a support at least superior than a
     * certain threshold.
     *
     * @param query
     * @param variable
     * @param supportThreshold
     * @return
     */
    public IntSet getRelevantClasses(List<int[]> query, int variable, int supportThreshold) {
        IntSet result = new IntOpenHashSet();
        for (int c : classSize.keySet()) {
            if (getStandardConfidenceWithThreshold(TypingHeuristic.typeL(c, variable), query, variable, supportThreshold, true) != 0) {
                result.add(c);
            }
        }
        return result;
    }

    public Int2ObjectMap<Int2DoubleMap> computeStatistics(List<int[]> query, int variable, int classSizeThreshold, int supportThreshold) {
        Int2ObjectMap<Int2DoubleMap> result = new Int2ObjectOpenHashMap<>();
        IntSet relevantClasses = getRelevantClasses(query, variable, supportThreshold);

        for (int class1 : relevantClasses) {
            int c1size = classSize.get(class1);

            if (c1size < classSizeThreshold) {
                continue;
            }

            Int2DoubleMap r = new Int2DoubleOpenHashMap();
            List<int[]> clause = TypingHeuristic.typeL(class1, variable);
            clause.addAll(query);

            for (int class2 : relevantClasses) {

                assert (clause.size() == query.size() + 1);
                if (class1 == class2) {
                    continue;
                }
                if (classSize.get(class2) < classSizeThreshold) {
                    // Ensure the symmetry of the output.
                    continue;
                }
                if (!classIntersectionSize.containsKey(class1) || !classIntersectionSize.get(class1).containsKey(class2)) {
                    continue;
                }

                int c1c2size = classIntersectionSize.get(class1).get(class2);

                if (c1c2size < classSizeThreshold) {
                    continue;
                    //r.put(class2, Double.NaN);
                } else if (c1size - c1c2size < classSizeThreshold) {
                    continue;
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
                    //r.put(class2, 1.);
                } else {
                    // c1 , q => c2
                    double s = getStandardConfidenceWithThreshold(TypingHeuristic.typeL(class2, variable), clause, variable, -1, true);
                    r.put(class2, ((double) c1c2size / (c1size - c1c2size)) * (1.0 - s) / s);
                }
            }

            if (!r.isEmpty()) {
                result.put(class1, r);
            }
        }
        return result;
    }

    public void classify(Int2ObjectMap<Int2DoubleMap> statistics, double eliminationRatio) {
        // add an unionRatio ?
        double lratio = Math.abs(Math.log(eliminationRatio));
        IntSet V = new IntOpenHashSet(statistics.keySet());
        for (int class1 : statistics.keySet()) {
            for (int class2 : statistics.get(class1).keySet()) {
                double s1 = Math.log(statistics.get(class1).get(class2));
                if (Double.isNaN(s1)) {
                    continue;
                }
                if (s1 < -lratio) {
                    V.remove(class1);
                }
                if (s1 > lratio) {
                    V.remove(class2);
                }

                /*
				if(s1 > lratio) {
					System.out.println(header + "!1, !2, 1-2\t" + ss1);
				}
				if(s2 > lratio) {
					System.out.println(header + "!1, !2, 2-1\t" + ss2);
				}
				if(s1 < -lratio && s2 < -lratio) {
					System.out.println(header + "!1, !2, 1^2\t" + ss1 + "," + ss2);
				}
				if(Double.isNaN(s1) || (s1 > -lratio && s1 < lratio)) {
					if(s2 < lratio && s2 > -lratio) {
						System.out.println(header + "1U2\t" + ss1 + "," + ss2);
					}
					if(s2 < -lratio) {
						System.out.println(header + "1, !2\t" + ss1 + "," + ss2);
					}
				}
				if((Double.isNaN(s2) || (s2 < lratio && s2 > -lratio)) && s1 < -lratio) {
					System.out.println(header + "!1, 2\t" + ss1 + "," + ss2);
				}
                 */
            }
        }
        for (int t : V) {
            System.out.println(KB.unmap(t));
        }
    }

    public SeparationClassifier(KB source) {
        db = source;
        classSize = Schema.getTypesCount(source);
        classIntersectionSize = Schema.getTypesIntersectionCount(source);
    }

    public SeparationClassifier(KB source, File countsFile) {
        db = source;
        try {
            classSize = Schema.loadTypesCount(countsFile);
            classIntersectionSize = Schema.loadTypesIntersectionCount(countsFile);
        } catch (IOException e) {
            System.err.println("Counts file import failed, recomputing...");
            classSize = Schema.getTypesCount(source);
            classIntersectionSize = Schema.getTypesIntersectionCount(source);
        }
    }

    public SeparationClassifier(KB source, File typeCountFile, File typeIntersectionCountFile) {
        db = source;
        try {
            classSize = Schema.loadTypesCount(typeCountFile);
            classIntersectionSize = Schema.loadTypesIntersectionCount(typeIntersectionCountFile);
        } catch (IOException e) {
            System.err.println("Counts file import failed, recomputing...");
            classSize = Schema.getTypesCount(source);
            classIntersectionSize = Schema.getTypesIntersectionCount(source);
        }
    }

    public SeparationClassifier(KB source, Int2IntMap cS, Int2ObjectMap<Int2IntMap> cIS) {
        db = source;
        classSize = cS;
        classIntersectionSize = cIS;
    }

    public static Options getOptions() {
        Options options = new Options();
        Option popularityOpt = OptionBuilder.withArgName("cst")
                .hasArg()
                .withDescription("Class size Threshold. Default: 50")
                .create("cst");
        Option outputThresholdOpt = OptionBuilder.withArgName("st")
                .hasArg()
                .withDescription("Support threshold. Default: 50")
                .create("st");
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
        Option queryOpt = OptionBuilder.withArgName("query")
                .hasArg()
                .withDescription("Queried attribute [-1]")
                .create("q");
        Option countFile = OptionBuilder.withArgName("countsF")
                .hasArg()
                .withDescription("Counts file, type count if intersection count file is set")
                .create("cf");
        Option countIntersectionFile = OptionBuilder.withArgName("iCountF")
                .hasArg()
                .withDescription("Intersection count file")
                .create("icf");
        Option wikidataOpt = OptionBuilder.withDescription("Assume wikidata setup")
                .create("w");

        // Parameters
        options.addOption(popularityOpt);
        options.addOption(outputThresholdOpt);
        // Query
        options.addOption(queryOpt);
        // Count files options
        options.addOption(countFile);
        options.addOption(countIntersectionFile);
        // KB related options
        options.addOption(delimiterOpt);
        options.addOption(typeRelationOpt);
        options.addOption(topOpt);
        options.addOption(subClassRelationOpt);
        options.addOption(wikidataOpt);
        
        return options;
    }

    public static class ParsedArguments {

        public Integer classSizeThreshold = 50;
        public Integer supportThreshold = 50;
        public String delimiter = "\t";
        public String[] leftOverArgs = null;
        public List<int[]> query;
        public int variable;
        public File countFile = null;
        public File countIntersectionFile = null;

        public ParsedArguments(CommandLine cli, HelpFormatter formatter, Options options) {
            if (cli.hasOption("cst")) {
                try {
                    String cstStr = cli.getOptionValue("cst");
                    classSizeThreshold = Integer.parseInt(cstStr);
                } catch (NumberFormatException e) {
                    System.err.println("The option -cst (class size threshold) requires an integer as argument");
                    System.err.println("*Classifier [OPTIONS] <.tsv INPUT FILES>");
                    formatter.printHelp("*Classifier", options);
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
                System.err.println("*Classifier [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }

            if (cli.hasOption("cf")) {
                try {
                    countFile = new File(cli.getOptionValue("cf"));
                } catch (Exception e) {
                    System.err.println("Error while creating countFile object");
                }
                if (cli.hasOption("icf") && countFile != null) {
                    try {
                        countIntersectionFile = new File(cli.getOptionValue("icf"));
                    } catch (Exception e) {
                        System.err.println("Error while creating intersection countFile object");
                    }
                }
            }

            if (cli.hasOption("q")) {

                String[] attr = cli.getOptionValue("q").split("-1");
                query = KB.triples(KB.triple("?x", attr[0], "?y"));
                variable = (attr.length == 1) ? KB.map("?x") : KB.map("?y");
            }
        }
    }

    public static void main(String[] args) throws IOException {

        CommandLine cli = null;
        double eliminationRatio = 1.5;

        HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = getOptions();

        Option eliminationRatioOpt = OptionBuilder.withArgName("er")
                .hasArg()
                .withDescription("Elimination ratio. Default: 1.5")
                .create("er");
        options.addOption(eliminationRatioOpt);

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("SeparationClassifier [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        ParsedArguments pa = new ParsedArguments(cli, formatter, options);

        if (cli.hasOption("er")) {
            try {
                String erStr = cli.getOptionValue("er");
                eliminationRatio = Double.parseDouble(erStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -er (elimination ratio) requires a double as argument");
                System.err.println("SeparationClassifier [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("SeparationClassifier", options);
                System.exit(1);
            }
        }

        //Load database
        List<File> dataFiles = new ArrayList<>();
        for (int i = 0; i < pa.leftOverArgs.length; ++i) {
            dataFiles.add(new File(pa.leftOverArgs[i]));
        }
        KB dataSource = new KB();
        dataSource.setDelimiter(pa.delimiter);
        dataSource.load(dataFiles);

        //Load classifier
        SeparationClassifier sc;
        if (pa.countFile != null) {
            if (pa.countIntersectionFile != null) {
                sc = new SeparationClassifier(dataSource, pa.countFile, pa.countIntersectionFile);
            } else {
                sc = new SeparationClassifier(dataSource, pa.countFile);
            }
        } else {
            System.out.print("Computing classes overlap...");
            sc = new SeparationClassifier(dataSource);
            System.out.println(" Done.");
        }

        sc.classify(sc.computeStatistics(pa.query, pa.variable, pa.classSizeThreshold, pa.supportThreshold), eliminationRatio);
    }
}
