package amie.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.MultiMap;
import javatools.filehandlers.FileLines;

public class Schema {
	
        public static String top = "owl:Thing";
        public static ByteString topBS = ByteString.of(top);
    
	/** X rdf:type Class **/
	public static String typeRelation = "rdf:type";
	
	public static ByteString typeRelationBS = ByteString.of(typeRelation);
	
	/** Class1 rdfs:subClassOf Class2 **/
	public static String subClassRelation = "rdfs:subClassOf";
	
	public static ByteString subClassRelationBS = ByteString.of(subClassRelation);
	
	/** relation1 rdfs:subPropertyOf relation2 **/
	public static String subPropertyRelation = "rdfs:subPropertyOf";
	
	public static ByteString subPropertyRelationBS = ByteString.of(subPropertyRelation);
	
	/** Class rdfs:domain relation **/
	public static String domainRelation = "rdfs:domain";
	
	public static ByteString domainRelationBS = ByteString.of(domainRelation);
	
	/** Class rdfs:domain range **/
	public static String rangeRelation = "rdfs:range";
	
	public static ByteString rangeRelationBS = ByteString.of(rangeRelation);
	
	public static List<ByteString> schemaRelationsBS = Arrays.asList(typeRelationBS, subClassRelationBS, 
			subPropertyRelationBS, domainRelationBS, rangeRelationBS);
	
	public static List<String> schemaRelations = Arrays.asList(typeRelation, subClassRelation, 
			subPropertyRelation, domainRelation, rangeRelation);
	
	private static boolean taxonomyMaterialized = false;
	
	private static IntSet allDefinedTypesMaterialized = new IntOpenHashSet();
	
	private static MultiMap<ByteString, ByteString> subClassMaterialized = new MultiMap<>();
	
	private static MultiMap<ByteString, ByteString> superClassMaterialized = new MultiMap<>();
    
	public static void materializeTaxonomy(KB source) {
		List<ByteString[]> query = KB.triples(KB.triple("?x", subClassRelationBS, "?y"));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(ByteString.of("?x"), query));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(ByteString.of("?y"), query));
		for (ByteString type : allDefinedTypesMaterialized) {
			for (ByteString subtype : getAllSubTypes(source, type)) subClassMaterialized.put(type, subtype);
			for (ByteString supertype : getAllSuperTypes(source, type)) superClassMaterialized.put(type, supertype);
		}
		taxonomyMaterialized = true;
	}
	
	public static boolean isTaxonomyMaterialized() {
		return taxonomyMaterialized;
	}
	
	public static IntSet getAllDefinedTypes() {
		return Collections.unmodifiableSet(allDefinedTypesMaterialized);
	}
	
	public static void loadSchemaConf() {
		try {
			String schemaPath = System.getProperty("schema");
			if (schemaPath == null) {
				schemaPath = "conf/schema_properties";
			}
			List<String> lines = Files.readAllLines(Paths.get(schemaPath),
			        Charset.defaultCharset());
			for (String line : lines) {
				String[] lineParts = line.split("=");
				if (lineParts.length < 2)
					continue;
				try {
					amie.data.U.class.getField(lineParts[0]).set(null, lineParts[1]);
					amie.data.U.class.getField(lineParts[0] + "BS").set(null, ByteString.of(lineParts[1]));
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
						| SecurityException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.err.println("Using the default schema relations");
		}
		
	}
	
	/**
	 * True if the relation is a special RDF/RDFS relation such as
	 * rdf:type
	 * @param relation
	 * @return
	 */
	public static boolean isSchemaRelation(ByteString relation) {
		return schemaRelationsBS.contains(relation);
	}
	
	/**
	 * True if the relation is a special RDF/RDFS relation such as
	 * rdf:type
	 * @param relation
	 * @return
	 */
	public static boolean isSchemaRelation(String relation) {
		return schemaRelations.contains(relation);
	}

	/**
	 * Returns the domain of a given relation in a knowledge base
	 * @param source
	 * @param relation
	 * @return
	 */
	public static ByteString getRelationDomain(KB source, ByteString relation){
		List<ByteString[]> query = KB.triples(KB.triple(relation, domainRelation, "?x"));
		IntSet domains = source.selectDistinct(ByteString.of("?x"), query);
		if(!domains.isEmpty()){
			return domains.iterator().next();
		}
		
		//Try looking for the superproperty
		List<ByteString[]> query2 = KB.triples(KB.triple(relation, subPropertyRelation, "?y"), 
				KB.triple("?y", "rdfs:domain", "?x"));
		
		domains = source.selectDistinct(ByteString.of("?x"), query2);
		if(!domains.isEmpty()){
			return domains.iterator().next();
		}
		
		return null;
	}
	
	/**
	 * Returns the range of a given relation in a knowledge base.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static ByteString getRelationRange(KB source, ByteString relation){
		List<ByteString[]> query = KB.triples(KB.triple(relation, rangeRelation, "?x"));
		IntSet ranges = source.selectDistinct(ByteString.of("?x"), query);
		if(!ranges.isEmpty()){
			return ranges.iterator().next();
		}
		
		//Try looking for the superproperty
		List<ByteString[]> query2 = KB.triples(KB.triple(relation, subPropertyRelation, "?y"), 
				KB.triple("?y", "rdfs:range", "?x"));
		
		ranges = source.selectDistinct(ByteString.of("?x"), query2);
		if(!ranges.isEmpty()){
			return ranges.iterator().next();
		}
		
		return null;		
	}
	
	/**
	 * It returns all the materialized types of an entity in a knowledge base.
	 * @param source
	 * @param entity
	 * @return
	 */
	public static IntSet getMaterializedTypesForEntity(KB source, ByteString entity){
		List<ByteString[]> query = KB.triples(KB.triple(entity, typeRelationBS, ByteString.of("?x")));
		return source.selectDistinct(ByteString.of("?x"), query);
	}
	
	/**
	 * Determines whether a given type is specific, that is, it does not have subclasses.
	 * @param source
	 * @param type
	 * @return
	 */
	public static boolean isLeafDatatype(KB source, ByteString type){
		if (taxonomyMaterialized) {
			IntSet subTypes = subClassMaterialized.get(type);
			return subTypes == null || subTypes.size() == 0;
		}
		List<ByteString[]> query = KB.triples(KB.triple("?x", subClassRelation, type));		
		return source.countDistinct(ByteString.of("?x"), query) == 0;
	}
	
	/**
	 * It returns the most specific types of an entity according to the type hierarchy
	 * of the knowledge base.
	 * @param source
	 * @param entity
	 * @return
	 */
	public static IntSet getLeafTypesForEntity(KB source, ByteString entity){
		IntSet tmpTypes = getMaterializedTypesForEntity(source, entity);
		IntSet resultTypes = new IntOpenHashSet();
		
		for(ByteString type: tmpTypes){
			if(isLeafDatatype(source, type)){
				resultTypes.add(type);
			}
		}
		
		return resultTypes;
	}
	
	/**
	 * It returns all the types of a given entity.
	 * @param source
	 * @param entity
	 * @return
	 */
	public static IntHashMap<ByteString> getAllTypesForEntity(KB source, ByteString entity){
		IntSet leafTypes = getMaterializedTypesForEntity(source, entity);
		IntHashMap<ByteString> resultTypes = new IntHashMap<ByteString>(leafTypes);
		for(ByteString leafType: leafTypes){
			resultTypes.addAll(getAllSuperTypes(source, leafType));
		}
		return resultTypes;
	}
	
	/**
	 * It returns all the immediate super-types of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getSuperTypes(KB source, ByteString type){
		List<ByteString[]> query = KB.triples(KB.triple(type, subClassRelation, "?x"));		
		return new IntOpenHashSet(source.selectDistinct(ByteString.of("?x"), query));
	}
	
	/**
	 * It returns all the supertypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getAllSuperTypes(KB source, ByteString type) {
		
		if (taxonomyMaterialized) {
			return superClassMaterialized.get(type);
		}
		
		IntSet resultSet = new IntOpenHashSet();
		Queue<ByteString> queue = new LinkedList<>();
		IntSet seenTypes = new IntOpenHashSet();
		IntSet superTypes = getSuperTypes(source, type);
		queue.addAll(superTypes);
		seenTypes.addAll(superTypes);
		
		while (!queue.isEmpty()) {
			ByteString currentType = queue.poll();
			resultSet.add(currentType);
			superTypes = getSuperTypes(source, currentType);
			for (ByteString st : superTypes) {
		        if (!seenTypes.contains(st)) {
	                seenTypes.add(st);
	                queue.add(st);
		        }
			}
		}
		
		return resultSet;
	}
	
	/**
	 * It returns all the instances of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getAllEntitiesForType(KB source, ByteString type) {
		List<ByteString[]> query = KB.triples(KB.triple("?x", typeRelation, type));		
		return new IntOpenHashSet(source.selectDistinct(ByteString.of("?x"), query));	
	}
	
	/**
	 * Returns the number of instances of the given class in a KB
	 * @param kb
	 * @param type
	 * @return
	 */
	public static long getNumberOfEntitiesForType(KB kb, ByteString type) {
		return kb.count(ByteString.of("?s"), typeRelationBS, type);
	}
	
	/**
	 * Returns all present data types in the given KB.
	 * @param kb
	 */
	public static IntSet getAllTypes(KB kb) {
		List<ByteString[]> query = KB.triples(KB.triple("?x", typeRelation, "?type"));		
		return new IntOpenHashSet(kb.selectDistinct(ByteString.of("?type"), query));	
	}
	
	/**
	 * Gets all the entities of the type of the given relation's domain.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static IntSet getDomainSet(KB source, ByteString relation) {
		ByteString domainType = getRelationDomain(source, relation);
		IntSet result = new IntOpenHashSet();
		if (domainType != null) 
			result.addAll(getAllEntitiesForType(source, domainType));
		result.addAll(source.relation2subject2object.get(relation).keySet());
		return result;
	}
	
	/**
	 * Gets all the entities of the given type that occur as subjects in the relation.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static IntSet getDomainSet(KB source, ByteString relation, 
			ByteString domainType) {
		List<ByteString[]> query = null;
		String queryVar = "?s";
		query = KB.triples(KB.triple("?s", relation, "?o"), 
						   KB.triple(ByteString.of(queryVar), 
								   typeRelationBS, domainType));
		
		return source.selectDistinct(ByteString.of(queryVar), query);		
	}

	
	/**
	 * Get all the immediate subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getSubTypes(KB source, ByteString type) {
		List<ByteString[]> query = KB.triples(KB.triple(ByteString.of("?x"), subClassRelation, type));		
		return new IntOpenHashSet(source.selectDistinct(ByteString.of("?x"), query));	
	}
	
	public static IntSet getSubtypes(KB source, ByteString type) {	
		return getSubTypes(source, type);	
	}
	
	/**
	 * Get all subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getAllSubTypes(KB source, ByteString type) {
		
		if (taxonomyMaterialized) {
			return subClassMaterialized.get(type);
		}
		
		IntSet resultSet = new IntOpenHashSet();
		Queue<ByteString> queue = new LinkedList<>();
		IntSet seenTypes = new IntOpenHashSet();
		IntSet subTypes = getSubTypes(source, type);
		queue.addAll(subTypes);
		seenTypes.addAll(subTypes);
		
		while (!queue.isEmpty()) {
			ByteString currentType = queue.poll();
			resultSet.add(currentType);
			subTypes = getSubtypes(source, currentType);
			for (ByteString st : subTypes) {
		        if (!seenTypes.contains(st)) {
	                seenTypes.add(st);
	                queue.add(st);
		        }
			}
		}
		
		return resultSet;
	}
	
	/**
	 * Gets all the entities of the type of the given relation's range.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static IntSet getRangeSet(KB source, ByteString relation) {
		ByteString rangeType = getRelationRange(source, relation);
		IntSet result = new IntOpenHashSet();
		if (rangeType != null) 
			result.addAll(getAllEntitiesForType(source, rangeType));
		result.addAll(source.relation2object2subject.get(relation).keySet());
		return result;
	}
	

	/**
	 * Gets all the entities of the given type that occur as objects in the relation.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static IntSet getRangeSet(KB source, ByteString relation, 
			ByteString rangeType) {
		List<ByteString[]> query = null;
		String queryVar = "?o";
		query = KB.triples(KB.triple("?s", relation, "?o"), 
						   KB.triple(ByteString.of(queryVar), 
								   typeRelationBS, rangeType));
		
		return source.selectDistinct(ByteString.of(queryVar), query);		
	}
	
	/**
	 * Compute a histogram on the theorethical domain of the relation (all the instances
	 * of the type defined as domain of the relation). This function looks at the most functional
	 * side, that is, if the relation is more inverse functional than functional it will calculate
	 * the histogram on the inverse relation (meaning it will provide a histogram of the range).
	 * @param kb
	 * @param relation
	 * @return
	 */
	public static IntHashMap<Integer> getHistogramOnDomain(KB kb, ByteString relation) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<ByteString[]> query = null;
		String queryVar = null;
		String existVar = null;
		ByteString targetType = null;
	
		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple("?s", relation, "?o"));
			targetType = getRelationDomain(kb, relation);
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple("?o", relation, "?s"));
			targetType = getRelationRange(kb, relation);
		}
		
		if (targetType == null) {
			return hist;
		}
		
		IntSet effectiveDomain = kb.selectDistinct(ByteString.of(queryVar), query);
		IntSet theorethicalDomain = getAllEntitiesForType(kb, targetType);
		effectiveDomain.retainAll(theorethicalDomain);
		for (ByteString entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, ByteString.of(existVar)));
			} else {
				val = kb.count(KB.triple(ByteString.of(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(ByteString.of(existVar), query);
		
		return hist;		
	}
	
	/**
	 * Computes a histogram for relation on a given type (all the instances
	 * of the provided type). The type must be a subclass of the domain of the relation.
	 * This function looks at the most functional side of the relation, that is, if the relation 
	 * is more inverse functional than functional it will calculate the histogram on the 
	 * inverse relation (meaning it will provide a histogram of the range).
	 * @param kb
	 * @param relation
	 * @return
	 */
	public static IntHashMap<Integer> getHistogramOnDomain(KB kb,
			ByteString relation, ByteString domainType) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<ByteString[]> query = null;
		String queryVar = null;
		String existVar = null;
	
		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple("?s", relation, "?o"), 
							   KB.triple(ByteString.of("?s"), 
									   typeRelationBS, domainType));
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple("?o", relation, "?s"),
					 		   KB.triple(ByteString.of("?o"), 
					 				   typeRelationBS, domainType));
		}
				
		IntSet effectiveDomain = kb.selectDistinct(ByteString.of(queryVar), query);
		for (ByteString entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, ByteString.of(existVar)));
			} else {
				val = kb.count(KB.triple(ByteString.of(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(ByteString.of(existVar), query);
		
		return hist;	
	}
	
	/**
	 * It returns a map containing the number of instances of each class in the KB.
	 * @param kb
	 * @return
	 */
	public static IntHashMap<ByteString> getTypesCount(KB kb) {
                Map<ByteString, IntSet> types2Instances = null;
                if (kb instanceof SimpleTypingKB) {
                    System.err.print("Computing type counts... ");
                    types2Instances = ((SimpleTypingKB) kb).classes;
                    IntHashMap<ByteString> result = new IntHashMap<>();
                    for (ByteString type : types2Instances.keySet()) {
			result.put(type, types2Instances.get(type).size());
                    }
                    System.err.println(Integer.toString(result.size()) + " classes found");
                    return result;
                } else {
                    List<ByteString[]> query = KB.triples(KB.triple("?s", typeRelation, "?o"));
                    types2Instances = new HashMap<>();
                    Map<ByteString, IntSet> ts = 
                            kb.selectDistinct(ByteString.of("?o"), ByteString.of("?s"), query);
                    IntHashMap<ByteString> result = new IntHashMap<>();
                    for (ByteString type : ts.keySet()) {
			result.put(type, ts.get(type).size());
                    }
                    return result;
                }
	}
	
	/**
	 * It returns a map of map containing the number of instances in the intersection of every two classes in the KB.
	 * @param kb
	 * @return
	 */
	public static Map<ByteString, IntHashMap<ByteString>> getTypesIntersectionCount(KB kb) {
            Map<ByteString, IntHashMap<ByteString>> result = new LinkedHashMap<>();
            System.err.println("Count class size required");
            if (kb instanceof SimpleTypingKB) {
                System.err.print("Computing type intersection counts... ");
                SimpleTypingKB db = (SimpleTypingKB) kb;
                for (ByteString type1 : db.classes.keySet()) {
			IntHashMap<ByteString> result2 = new IntHashMap<>();
			result.put(type1, result2);
			for (ByteString type2 : db.classes.keySet()) {
                            int is = (int) SetU.countIntersection(db.classes.get(type1), db.classes.get(type2));
                            result2.put(type2, is);
                        }
                }
                System.err.println();
		return result;
            }
		List<ByteString[]> query = KB.triples(KB.triple("?s", typeRelation, "?o1"), KB.triple("?s", typeRelation, "?o2"));
		Map<ByteString, Map<ByteString, IntSet>> types2types2Instances = 
				kb.selectDistinct(ByteString.of("?o1"), ByteString.of("?o2"), ByteString.of("?s"), query);
		
		for (ByteString type1 : types2types2Instances.keySet()) {
			IntHashMap<ByteString> result2 = new IntHashMap<>();
			result.put(type1, result2);
			for (ByteString type2 : types2types2Instances.get(type1).keySet())
			result2.put(type2, types2types2Instances.get(type1).get(type2).size());
		}
		return result;
	}
	
	public static IntHashMap<ByteString> loadTypesCount(File f) throws IOException {
		IntHashMap<ByteString> typesCount = new IntHashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 2) {
				try {
					typesCount.put(ByteString.of(split[0]), Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {}
			}
		}
		System.out.println("Loaded " + typesCount.size() + " classes");
		return typesCount;
	}
	
	public static Map<ByteString, IntHashMap<ByteString>> loadTypesIntersectionCount(File f) throws IOException {
		Map<ByteString, IntHashMap<ByteString>> typesIntersectionCount = new HashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 3) {
				try {
					int i = Integer.parseInt(split[2]);
					IntHashMap<ByteString> tc = typesIntersectionCount.get(ByteString.of(split[0]));
					if (tc == null) {
						typesIntersectionCount.put(ByteString.of(split[0]), tc = new IntHashMap<ByteString>());
					}
					tc.put(ByteString.of(split[1]), i);
				} catch (NumberFormatException e) {}
			}
		}
		System.out.println("Loaded " + typesIntersectionCount.size() + " intersection sizes.");
		return typesIntersectionCount;
	}

	/**
	 * Determines if the first class is a superclass of the second.
	 * @param parentType
	 * @param childType
	 * @return
	 */
	public static boolean isSuperType(KB kb, ByteString parentType, ByteString childType) {
		IntSet st1 = getSuperTypes(kb, childType);
		return st1.contains(parentType);
	}
	
	public static boolean isTransitiveSuperType(KB kb, ByteString parentType, ByteString childType) {
		IntSet st1 = getAllSuperTypes(kb, childType);
		return st1.contains(parentType);
	}
	
	
	public static void main(String args[]) throws IOException {
		KB d = new KB();
	    ArrayList<File> files = new ArrayList<File>();
	    for(String file: args)
	    	files.add(new File(file));
	    
	    d.load(files);
	    
	    for(ByteString relation: d.relationSize){
	    	System.out.println(relation + "\t" + getRelationDomain(d, relation) 
	    			+ "\t" + getRelationRange(d, relation));
	    }
	}
}
