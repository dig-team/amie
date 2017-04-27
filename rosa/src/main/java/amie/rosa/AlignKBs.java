package amie.rosa;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import amie.data.KB;
import amie.mining.AMIE;
import amie.rules.Metric;
import amie.rules.Rule;
import javatools.datatypes.ByteString;
import javatools.datatypes.Pair;
import javatools.datatypes.Triple;
import javatools.filehandlers.TSVFile;
import telecom.util.collections.MultiMap;

public class AlignKBs {
	
	public static String prefixkb1 = "<";
	
	public static String prefixkb2 = "<db:";
	
	public static String wikidataMappings = "/infres/ic2/galarrag/AMIE/Data/wikidata/mapping_yago_wikidata.tsv";
	
	/**
	 * Determines if a rule r => r' is a cross-ontology mapping, i.e., 
	 * r and r' belong to different ontologies.
	 * @param rule
	 * @return
	 */
	public static boolean isCrossOntology(Rule rule) {
		ByteString r1, r2;
		r1 = rule.getHead()[1];
		r2 = rule.getBody().get(0)[1];
		return (!r1.toString().startsWith(prefixkb2) && r2.toString().startsWith(prefixkb2)) || 
			(!r2.toString().startsWith(prefixkb2) && r1.toString().startsWith(prefixkb2));
	}

	public static KB loadFiles(String args[], int idx) throws IOException {
		KB kb = new KB();
		for (int i = idx; i < args.length; ++i) {
			String fileName = args[i];
			// Check of the format of the file
			BufferedReader brTest = new BufferedReader(new FileReader(fileName));
			String text = brTest.readLine();
			brTest.close();
			if (text.contains("\t")) {
				kb.load(new File(fileName));
			} else {
				TSVFile file = new TSVFile(new File(fileName));
				for (List<String> line : file) {
					String[] parts = line.get(0).split("> <");
					if (parts.length == 3) {
						kb.add(parts[0] + ">", "<" + parts[1] + ">", "<" + parts[2]);
					} else if (parts.length == 2) {
						String[] tailParts = parts[1].split("> \"");
						if (tailParts.length == 2) {
							kb.add(parts[0] + ">", "<" + tailParts[0] + ">", "\"" + tailParts[1]);
						}
					}
				}
				file.close();
			}
		}
		System.out.println(kb.size() + " facts loaded");
		return kb;
	}
	
	public static String map(Map<String, String> map, String val) {
		if (map.containsKey(val)) 
			return map.get(val);
		else
			return val;
	}
	
	public static KB loadAndMapFiles(String args[], int idx) throws IOException {
		KB kb = new KB();
		Map<String, String> yago2wiki = new HashMap<>();
		// Load the mappings 
		try(TSVFile mappings = new TSVFile(new File(wikidataMappings))) {
			for (List<String> mapping : mappings) {
				yago2wiki.put(mapping.get(0), mapping.get(2));
			}
		}
		
		for (int i = idx; i < args.length; ++i) {
			String fileName = args[i];
			TSVFile file = new TSVFile(new File(fileName));
			for (List<String> line : file) {
				String[] parts = line.get(0).split("> <");
				if (parts.length == 3) {
					kb.add(map(yago2wiki, parts[0] + ">"), "<" + parts[1] + ">", map(yago2wiki, "<" + parts[2]));
				} else if (parts.length == 2) {
					String[] tailParts = parts[1].split("> \"");
					if (tailParts.length == 2) {
						kb.add(map(yago2wiki, parts[0] + ">"), "<" + tailParts[0] + ">", map(yago2wiki, "\"" + tailParts[1]));
					}
				}
			}
			file.close();
		}
		return kb;
	}


	
	public static void main(String[] args) throws Exception {	
		KB kb = null;
		boolean smartMode = args[0].equals("smart");
		if (args[1].equals("wikidata"))
			kb = loadAndMapFiles(args, 2);
		else
			kb = loadFiles(args, 2);
		AMIE amie = AMIE.getVanillaSettingInstance(kb);
		amie.getAssistant().setMaxDepth(2);
		amie.setPruningMetric(Metric.Support);
		amie.setMinSignificanceThreshold(10);
		amie.setMinInitialSupport(10);
		amie.setRealTime(false);		
		List<Rule> rules = amie.mine();
		List<Rule> crossOntologyRules = new ArrayList<>();
		for (Rule rule : rules) {
			if (isCrossOntology(rule)) {
				crossOntologyRules.add(rule);
			}
			//if (isCrossOntology(rule) && rule.getPcaConfidence() >= 0.3) {
			//	crossOntologyRules.add(rule);			
			//}				
		}
		
		List<ROSAEquivalence> rosaEquivalences = 
				EquivalenceRulesBuilder.findEquivalences(kb, crossOntologyRules);
		
		Collections.sort(rosaEquivalences, new Comparator<ROSAEquivalence>() {
			@Override
			public int compare(ROSAEquivalence o1, ROSAEquivalence o2) {
				return Double.compare(o2.getConfidence(), o1.getConfidence());
			}			
		});
		
		// Additional steps to remove stupid mappings
		MultiMap<String, Triple<String, ROSAEquivalence, Boolean>> originalMap = new MultiMap<>();
		MultiMap<String, Triple<String, ROSAEquivalence, Boolean>> invertedMap = new MultiMap<>();
		
		System.out.println("Mappings");
		if (smartMode) {
			for (ROSAEquivalence rule : rosaEquivalences) {
				rule.prefix1 = prefixkb1;
				rule.prefix2 = prefixkb2;
				Pair<String, String> relations = rule.getRelations();
				invertedMap.put(relations.second, new Triple<String, ROSAEquivalence, Boolean>(relations.first, rule, true));
				originalMap.put(relations.first, new Triple<String, ROSAEquivalence, Boolean>(relations.second, rule, true));
			}
			
			for (String relation : originalMap.keySet()) {
				for (Triple<String, ROSAEquivalence, Boolean> mapping : originalMap.get(relation)) {
					if (!mapping.third)
						continue; // The guy was taken already by somebody else :(
					
					ROSAEquivalence currentMapping = mapping.second;
					String currentRelation = mapping.first;
					if (invertedMap.get(currentRelation).size() > 1) {
						// Here we have a problem, this mapping belongs to other relations. 
						// Dear, you have to choose
						Triple<String, ROSAEquivalence, Boolean> bestMappingTriple = 
								getBestMapping(invertedMap.get(currentRelation));
												
						if (bestMappingTriple != null && 
								bestMappingTriple.second == currentMapping) {
							// The guy decided to stay with me ^_^, hooray
							System.out.println(mapping.second.toShortString());
							// Remove it from the other map of others, it is only mine lero lero! 						
							for (String otherRelation : originalMap.keySet()) {
								if (!otherRelation.equals(relation)) {
									for (Triple<String, ROSAEquivalence, Boolean> otherMappings : originalMap.get(relation)) {
										if (otherMappings.first.equals(currentRelation)) {
											otherMappings.third = false;
										}	
									}
								}
							}
						} else {
							// Let him be happy even if it is not with me ;-(
							mapping.third = false;
						}
					} else {
						// This means the guy is only mine, hooray ^_^
						System.out.println(mapping.second.toShortString());
					}
				}
			}
		} else {
			for (ROSAEquivalence rule : rosaEquivalences) {
				rule.prefix1 = prefixkb1;
				rule.prefix2 = prefixkb2;
				System.out.println(rule.toShortString());
			}
		}
		
	}

	/**
	 * 
	 * @param list
	 * @return
	 */
	private static Triple<String, ROSAEquivalence, Boolean> getBestMapping(List<Triple<String, ROSAEquivalence, Boolean>> list) {
		Triple<String, ROSAEquivalence, Boolean> bestMapping = null;
		double bestConfidence = 0.0;
		
		for (Triple<String, ROSAEquivalence, Boolean> triple : list) {
			if (triple.second.getConfidence() > bestConfidence) {
				if (triple.third) { // Only if it has not been removed
					bestMapping = triple;
					bestConfidence = triple.second.getConfidence();
				}
			}
		}
		
		return bestMapping;
	}

}
