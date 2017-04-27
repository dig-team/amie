package amie.data.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import amie.data.KB;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.filehandlers.TSVFile;

public class RelevanceCalculator {

	enum RelevanceMetric { Wikilength, NumberOfFacts, IngoingLinks, JointMeasure }
	
	private static void calculateRelevanceFromAdditionalFile(String[] args) throws IOException {
		try(TSVFile tsv = new TSVFile(new File(args[0]))) {
			RelevanceMetric relevanceMetric = RelevanceMetric.NumberOfFacts;
			KB kb = null;
			if (args.length > 1) {
				kb = new KB();
				kb.load(new File(args[1]));
			}
			
			if (args.length > 2) {
				relevanceMetric = RelevanceMetric.valueOf(args[2]);
			}
			
			
			IntHashMap<String> ingoingLinksMap = new IntHashMap<>();
			IntHashMap<String> wikiLengthMap = new IntHashMap<>();
			
			for (List<String> line : tsv) {
				String entity = line.get(0);
				String relation = line.get(1);
				String value = extractInteger(line.get(2));
				if (relation.equals("<hasIngoingLinks>")) {
					ingoingLinksMap.add(entity, Integer.parseInt(value));
				} else if (relation.equals("<hasWikipediaArticleLength>")) {
					wikiLengthMap.add(entity, Integer.parseInt(value));					
				}
			}
			
			Set<String> allEntities = new LinkedHashSet<>();
			allEntities.addAll(ingoingLinksMap.increasingKeys());
			allEntities.addAll(wikiLengthMap.increasingKeys());
			for (String entity : allEntities) {
				double nFacts = 1;
				int wikiLength = wikiLengthMap.get(entity);
				if (wikiLength <= 0) {
					wikiLength = 2;
				}
				int ingoingLinks = ingoingLinksMap.get(entity);
				if (ingoingLinks <= 0) {
					ingoingLinks = 2;
				}
				if (kb != null) {
					nFacts = kb.count(ByteString.of(entity), ByteString.of("?p"), ByteString.of("?o"));
					nFacts += kb.count(ByteString.of("?s"), ByteString.of("?p"), ByteString.of(entity));
				}
				
				double coefficient = 0.0;
				switch (relevanceMetric) {
				case Wikilength :					
					coefficient = wikiLength;
					break;
				case JointMeasure :					
					coefficient = (Math.log10(wikiLength)) * (ingoingLinks) * (nFacts);
					break;
				case NumberOfFacts :
					coefficient = nFacts;
					break;
				case IngoingLinks :
					coefficient = ingoingLinks;
					break;
				}
				System.out.println(entity + "\t<hasRelevance>\t" + coefficient);								
			}
		}
	}
	
	private static void calculateRelevance(String args[]) throws IOException {
		KB kb = amie.data.U.loadFiles(args);
		IntHashMap<ByteString> allEntities = kb.getEntitiesOccurrences();
		for (ByteString entity : allEntities) {
			System.out.println(entity + "\t<hasRelevance>\t" + allEntities.get(entity));
		}
	}
	
	/**
	 * For each entity in an input KB (given as a TSV file), it outputs the relevance
	 * of the entities based on the formula:
	 * 
	 * log(wiki-length) * (ingoing-links + 2) * (number-facts) 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 2) {
			calculateRelevanceFromAdditionalFile(args);
		} else {
			calculateRelevance(args);
		}

	}
	
	private static String extractInteger(String xsdInteger) {
		return xsdInteger.replace("\"", "").replace("^^xsd:integer", "");
	}
}
