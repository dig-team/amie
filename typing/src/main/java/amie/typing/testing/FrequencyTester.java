package amie.typing.testing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import amie.data.KB;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;


import javatools.datatypes.FrequencyVector;
import javatools.filehandlers.FileLines;

public class FrequencyTester {

	private Int2ObjectMap<IntSet> attribute2classes = null;
	private Map<String, Int2ObjectMap<Int2ObjectMap<Double>>> heuristic2attribute2class2score;

	public void loadResults(File f) throws IOException {
		heuristic2attribute2class2score = new HashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.trim().split("\t");
			if (split.length == 4) {
				String heuristic = split[0].trim();
				Int2ObjectMap<Int2ObjectMap<Double>> attribute2class2score = heuristic2attribute2class2score.get(heuristic);
				if (attribute2class2score == null) {
					heuristic2attribute2class2score.put(heuristic, attribute2class2score = new Int2ObjectOpenHashMap<>());
				}
				int attribute = KB.map(split[2].trim());
				if (attribute2classes != null && !attribute2classes.containsKey(attribute)) continue;
				Int2ObjectMap<Double> class2score = attribute2class2score.get(attribute);
				if (class2score == null) {
					attribute2class2score.put(attribute, class2score = new Int2ObjectOpenHashMap<>());
				}
				class2score.put(KB.map(split[1].trim()), Double.valueOf(split[3].trim()));
			}
		}
	}
	
	public void loadGoldStandard(KB taxo, File f) throws IOException {
		attribute2classes = new Int2ObjectOpenHashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.trim().split("\t");
			if (split.length == 2) {
				int attribute = KB.map(split[0].trim());
				IntSet classes = attribute2classes.get(attribute);
				if (classes == null) {
					classes = new IntOpenHashSet();
					attribute2classes.put(attribute, classes);
				}
				int dclass = KB.map("<http://dbpedia.org/ontology/" + split[1].trim() + ">");
				if(dclass != KB.map("<http://dbpedia.org/ontology/None>")) {
					classes.add(dclass);
					classes.addAll(amie.data.Schema.getAllSubTypes(taxo, dclass));
				}
			}
		}
	}
	
	public void printGoldStandard() {
		for (int attribute : attribute2classes.keySet()) {
			for (int classes : attribute2classes.get(attribute)) {
				System.out.println(KB.unmap(attribute) + "\t" + KB.unmap(classes));
			}
		}
	}
	
	public void test(String heuristic, int attribute) {
		FrequencyVector<Integer, Double> resultFV = new FrequencyVector<>(heuristic2attribute2class2score.get(heuristic).get(attribute));
		double precision = resultFV.weightedPrecisionWithRespectTo(attribute2classes.get(attribute));
		double recall = resultFV.recallWithRespectTo(attribute2classes.get(attribute));
		System.out.println(heuristic + "\t" + KB.unmap(attribute) + "\t" 
							+ String.valueOf(precision) + "\t" + String.valueOf(recall) 
							+ ((attribute2classes.get(attribute).isEmpty())?"\tE":""));
	}
	
	public void testAll() {
		for (String heuristic : heuristic2attribute2class2score.keySet()) {
			for (int attribute : attribute2classes.keySet()) {
				if (!attribute2classes.get(attribute).isEmpty()) test(heuristic, attribute);
			}
		}
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		if (args.length < 3) {
			System.err.println("USAGE: FrequencyTester taxonomy.ttl goldstandard.tsv results.tsv");
			System.exit(1);
		}
		
		FrequencyTester ft = new FrequencyTester();
		KB taxo = new KB();
		taxo.setDelimiter(" ");
		taxo.load(new File(args[0]));
		ft.loadGoldStandard(taxo, new File(args[1]));
		ft.loadResults(new File(args[2]));
		//ft.test("StdConf", KB.map("<http://dbpedia.org/ontology/architect>-1"));
		ft.testAll();
		//ft.printGoldStandard();
	}

}
