package amie.data;

import amie.data.tuple.IntArrays;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


import javatools.datatypes.IntHashMap;
import javatools.filehandlers.FileLines;

public class Schema {
	
        public static String top = "owl:Thing";
        public static int topBS = KB.map(top);
    
	/** X rdf:type Class **/
	public static String typeRelation = "rdf:type";
	
	public static int typeRelationBS = KB.map(typeRelation);
	
	/** Class1 rdfs:subClassOf Class2 **/
	public static String subClassRelation = "rdfs:subClassOf";
	
	public static int subClassRelationBS = KB.map(subClassRelation);
	
	/** relation1 rdfs:subPropertyOf relation2 **/
	public static String subPropertyRelation = "rdfs:subPropertyOf";
	
	public static int subPropertyRelationBS = KB.map(subPropertyRelation);
	
	/** Class rdfs:domain relation **/
	public static String domainRelation = "rdfs:domain";
	
	public static int domainRelationBS = KB.map(domainRelation);
	
	/** Class rdfs:domain range **/
	public static String rangeRelation = "rdfs:range";
	
	public static int rangeRelationBS = KB.map(rangeRelation);
	
	public static IntList schemaRelationsBS = IntArrays.asList(typeRelationBS, subClassRelationBS, 
			subPropertyRelationBS, domainRelationBS, rangeRelationBS);
	
	public static List<String> schemaRelations = Arrays.asList(typeRelation, subClassRelation, 
			subPropertyRelation, domainRelation, rangeRelation);
	
	private static boolean taxonomyMaterialized = false;
	
	private static IntSet allDefinedTypesMaterialized = new IntOpenHashSet();
	
	private static Int2ObjectMap<IntSet> subClassMaterialized = new Int2ObjectOpenHashMap<>();
	
	private static Int2ObjectMap<IntSet> superClassMaterialized = new Int2ObjectOpenHashMap<>();
    
	public static void materializeTaxonomy(KB source) {
		List<int[]> query = KB.triples(KB.triple("?x", subClassRelation, "?y"));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(KB.map("?x"), query));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(KB.map("?y"), query));
		for (int type : allDefinedTypesMaterialized) {
			subClassMaterialized.put(type, getAllSubTypes(source, type));
			superClassMaterialized.put(type, getAllSuperTypes(source, type));
		}
		taxonomyMaterialized = true;
	}
	
	public static boolean isTaxonomyMaterialized() {
		return taxonomyMaterialized;
	}
	
	public static IntSet getAllDefinedTypes() {
		return IntSets.unmodifiable(allDefinedTypesMaterialized);
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
					amie.data.U.class.getField(lineParts[0] + "BS").set(null, KB.map(lineParts[1]));
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
	public static boolean isSchemaRelation(int relation) {
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
	public static int getRelationDomain(KB source, int relation){
		List<int[]> query = KB.triples(KB.triple(relation, domainRelationBS, KB.map("?x")));
		IntSet domains = source.selectDistinct(KB.map("?x"), query);
		if(!domains.isEmpty()){
			return domains.iterator().nextInt();
		}
		
		//Try looking for the superproperty
		List<int[]> query2 = KB.triples(KB.triple(relation, subPropertyRelationBS, KB.map("?y")), 
				KB.triple("?y", "rdfs:domain", "?x"));
		
		domains = source.selectDistinct(KB.map("?x"), query2);
		if(!domains.isEmpty()){
			return domains.iterator().nextInt();
		}
		
		return 0;
	}
	
	/**
	 * Returns the range of a given relation in a knowledge base.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static int getRelationRange(KB source, int relation){
		List<int[]> query = KB.triples(KB.triple(relation, rangeRelationBS, KB.map("?x")));
		IntSet ranges = source.selectDistinct(KB.map("?x"), query);
		if(!ranges.isEmpty()){
			return ranges.iterator().nextInt();
		}
		
		//Try looking for the superproperty
		List<int[]> query2 = KB.triples(KB.triple(relation, subPropertyRelationBS, KB.map("?y")), 
				KB.triple("?y", "rdfs:range", "?x"));
		
		ranges = source.selectDistinct(KB.map("?x"), query2);
		if(!ranges.isEmpty()){
			return ranges.iterator().nextInt();
		}
		
		return 0;		
	}
	
	/**
	 * It returns all the materialized types of an entity in a knowledge base.
	 * @param source
	 * @param entity
	 * @return
	 */
	public static IntSet getMaterializedTypesForEntity(KB source, int entity){
		List<int[]> query = KB.triples(KB.triple(entity, typeRelationBS, KB.map("?x")));
		return source.selectDistinct(KB.map("?x"), query);
	}
	
	/**
	 * Determines whether a given type is specific, that is, it does not have subclasses.
	 * @param source
	 * @param type
	 * @return
	 */
	public static boolean isLeafDatatype(KB source, int type){
		if (taxonomyMaterialized) {
			IntSet subTypes = subClassMaterialized.get(type);
			return subTypes == null || subTypes.isEmpty();
		}
		List<int[]> query = KB.triples(KB.triple(KB.map("?x"), subClassRelationBS, type));		
		return source.countDistinct(KB.map("?x"), query) == 0;
	}
	
	/**
	 * It returns the most specific types of an entity according to the type hierarchy
	 * of the knowledge base.
	 * @param source
	 * @param entity
	 * @return
	 */
	public static IntSet getLeafTypesForEntity(KB source, int entity){
		IntSet tmpTypes = getMaterializedTypesForEntity(source, entity);
		IntSet resultTypes = new IntOpenHashSet();
		
		for(int type: tmpTypes){
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
	public static IntSet getAllTypesForEntity(KB source, int entity){
		IntSet leafTypes = getMaterializedTypesForEntity(source, entity);
		IntSet resultTypes = new IntOpenHashSet(leafTypes);
		for (int leafType: leafTypes) {
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
	public static IntSet getSuperTypes(KB source, int type){
		List<int[]> query = KB.triples(KB.triple(type, subClassRelationBS, KB.map("?x")));		
		return source.selectDistinct(KB.map("?x"), query);
	}
	
	/**
	 * It returns all the supertypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
    public static IntSet getAllSuperTypes(KB source, int type) {

        if (taxonomyMaterialized) {
            return superClassMaterialized.get(type);
        }

        IntSet resultSet = new IntOpenHashSet();
        Queue<Integer> queue = new LinkedList<>();
        IntSet seenTypes = new IntOpenHashSet();
        IntSet superTypes = getSuperTypes(source, type);
        queue.addAll(superTypes);
        seenTypes.addAll(superTypes);

        while (!queue.isEmpty()) {
            int currentType = queue.poll();
            resultSet.add(currentType);
            superTypes = getSuperTypes(source, currentType);
            for (int st : superTypes) {
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
	public static IntSet getAllEntitiesForType(KB source, int type) {
		List<int[]> query = KB.triples(KB.triple(KB.map("?x"), typeRelationBS, type));		
		return new IntOpenHashSet(source.selectDistinct(KB.map("?x"), query));	
	}
	
	/**
	 * Returns the number of instances of the given class in a KB
	 * @param kb
	 * @param type
	 * @return
	 */
	public static long getNumberOfEntitiesForType(KB kb, int type) {
		return kb.count(KB.map("?s"), typeRelationBS, type);
	}
	
	/**
	 * Returns all present data types in the given KB.
	 * @param kb
	 */
	public static IntSet getAllTypes(KB kb) {
		List<int[]> query = KB.triples(KB.triple("?x", typeRelation, "?t9"));		
		return new IntOpenHashSet(kb.selectDistinct(KB.map("?t9"), query));	
	}
	
	/**
	 * Gets all the entities of the type of the given relation's domain.
	 * @param source
	 * @param relation
	 * @return
	 */
	public static IntSet getDomainSet(KB source, int relation) {
		int domainType = getRelationDomain(source, relation);
		IntSet result = new IntOpenHashSet();
		if (domainType != 0) 
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
	public static IntSet getDomainSet(KB source, int relation, 
			int domainType) {
		List<int[]> query = null;
		String queryVar = "?s";
		query = KB.triples(KB.triple(KB.map("?s"), relation, KB.map("?o")), 
						   KB.triple(KB.map(queryVar), 
								   typeRelationBS, domainType));
		
		return source.selectDistinct(KB.map(queryVar), query);		
	}

	
	/**
	 * Get all the immediate subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getSubTypes(KB source, int type) {
		List<int[]> query = KB.triples(KB.triple(KB.map("?x"), subClassRelationBS, type));		
		return new IntOpenHashSet(source.selectDistinct(KB.map("?x"), query));	
	}
	
	public static IntSet getSubtypes(KB source, int type) {	
		return getSubTypes(source, type);	
	}
	
	/**
	 * Get all subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public static IntSet getAllSubTypes(KB source, int type) {
		
		if (taxonomyMaterialized) {
			return subClassMaterialized.get(type);
		}
		
		IntSet resultSet = new IntOpenHashSet();
		Queue<Integer> queue = new LinkedList<>();
		IntSet seenTypes = new IntOpenHashSet();
		IntSet subTypes = getSubTypes(source, type);
		queue.addAll(subTypes);
		seenTypes.addAll(subTypes);
		
		while (!queue.isEmpty()) {
			int currentType = queue.poll();
			resultSet.add(currentType);
			subTypes = getSubtypes(source, currentType);
			for (int st : subTypes) {
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
	public static IntSet getRangeSet(KB source, int relation) {
		int rangeType = getRelationRange(source, relation);
		IntSet result = new IntOpenHashSet();
		if (rangeType != 0) 
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
	public static IntSet getRangeSet(KB source, int relation, 
			int rangeType) {
		List<int[]> query = null;
		String queryVar = "?o";
		query = KB.triples(KB.triple(KB.map("?s"), relation, KB.map("?o")), 
						   KB.triple(KB.map(queryVar), 
								   typeRelationBS, rangeType));
		
		return source.selectDistinct(KB.map(queryVar), query);		
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
	public static IntHashMap<Integer> getHistogramOnDomain(KB kb, int relation) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<int[]> query = null;
		String queryVar = null;
		String existVar = null;
		int targetType = 0;
	
		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple(KB.map("?s"), relation, KB.map("?o")));
			targetType = getRelationDomain(kb, relation);
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple(KB.map("?o"), relation, KB.map("?s")));
			targetType = getRelationRange(kb, relation);
		}
		
		if (targetType == 0) {
			return hist;
		}
		
		IntSet effectiveDomain = kb.selectDistinct(KB.map(queryVar), query);
		IntSet theorethicalDomain = getAllEntitiesForType(kb, targetType);
		effectiveDomain.retainAll(theorethicalDomain);
		for (int entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, KB.map(existVar)));
			} else {
				val = kb.count(KB.triple(KB.map(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(KB.map(existVar), query);
		
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
			int relation, int domainType) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<int[]> query = null;
		String queryVar = null;
		String existVar = null;
	
		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple(KB.map("?s"), relation, KB.map("?o")), 
							   KB.triple(KB.map("?s"), 
									   typeRelationBS, domainType));
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple(KB.map("?o"), relation, KB.map("?s")),
					 		   KB.triple(KB.map("?o"), 
					 				   typeRelationBS, domainType));
		}
				
		IntSet effectiveDomain = kb.selectDistinct(KB.map(queryVar), query);
		for (int entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, KB.map(existVar)));
			} else {
				val = kb.count(KB.triple(KB.map(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(KB.map(existVar), query);
		
		return hist;	
	}
	
	/**
	 * It returns a map containing the number of instances of each class in the KB.
	 * @param kb
	 * @return
	 */
	public static Int2IntMap getTypesCount(KB kb) {
                Int2ObjectMap<IntSet> types2Instances = null;
                if (kb instanceof SimpleTypingKB) {
                    System.err.print("Computing type counts... ");
                    types2Instances = ((SimpleTypingKB) kb).classes;
                    Int2IntMap result = new Int2IntOpenHashMap();
                    for (int type : types2Instances.keySet()) {
			result.put(type, types2Instances.get(type).size());
                    }
                    System.err.println(Integer.toString(result.size()) + " classes found");
                    return result;
                } else {
                    List<int[]> query = KB.triples(KB.triple("?s", typeRelation, "?o"));
                    types2Instances = new Int2ObjectOpenHashMap<>();
                    Int2ObjectMap<IntSet> ts = 
                            kb.selectDistinct(KB.map("?o"), KB.map("?s"), query);
                    Int2IntMap result = new Int2IntOpenHashMap();
                    for (int type : ts.keySet()) {
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
	public static Int2ObjectMap<Int2IntMap> getTypesIntersectionCount(KB kb) {
            Int2ObjectMap<Int2IntMap> result = new Int2ObjectOpenHashMap<>();
            System.err.println("Count class size required");
            if (kb instanceof SimpleTypingKB) {
                System.err.print("Computing type intersection counts... ");
                SimpleTypingKB db = (SimpleTypingKB) kb;
                for (int type1 : db.classes.keySet()) {
			Int2IntMap result2 = new Int2IntOpenHashMap();
			result.put(type1, result2);
			for (int type2 : db.classes.keySet()) {
                            int is = (int) SetU.countIntersection(db.classes.get(type1), db.classes.get(type2));
                            result2.put(type2, is);
                        }
                }
                System.err.println();
		return result;
            }
		List<int[]> query = KB.triples(KB.triple("?s", typeRelation, "?o1"), KB.triple("?s", typeRelation, "?o2"));
		Int2ObjectMap<Int2ObjectMap<IntSet>> types2types2Instances = 
				kb.selectDistinct(KB.map("?o1"), KB.map("?o2"), KB.map("?s"), query);
		
		for (int type1 : types2types2Instances.keySet()) {
			Int2IntMap result2 = new Int2IntOpenHashMap();
			result.put(type1, result2);
			for (int type2 : types2types2Instances.get(type1).keySet())
			result2.put(type2, types2types2Instances.get(type1).get(type2).size());
		}
		return result;
	}
	
	public static Int2IntMap loadTypesCount(File f) throws IOException {
		Int2IntMap typesCount = new Int2IntOpenHashMap();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 2) {
				try {
					typesCount.put(KB.map(split[0]), Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {}
			}
		}
		System.out.println("Loaded " + typesCount.size() + " classes");
		return typesCount;
	}
	
	public static Int2ObjectMap<Int2IntMap> loadTypesIntersectionCount(File f) throws IOException {
		Int2ObjectMap<Int2IntMap> typesIntersectionCount = new Int2ObjectOpenHashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 3) {
				try {
					int i = Integer.parseInt(split[2]);
					Int2IntMap tc = typesIntersectionCount.get(KB.map(split[0]));
					if (tc == null) {
						typesIntersectionCount.put(KB.map(split[0]), tc = new Int2IntOpenHashMap());
					}
					tc.put(KB.map(split[1]), i);
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
	public static boolean isSuperType(KB kb, int parentType, int childType) {
		IntSet st1 = getSuperTypes(kb, childType);
		return st1.contains(parentType);
	}
	
	public static boolean isTransitiveSuperType(KB kb, int parentType, int childType) {
		IntSet st1 = getAllSuperTypes(kb, childType);
		return st1.contains(parentType);
	}
	
	
	public static void main(String args[]) throws IOException {
		KB d = new KB();
	    ArrayList<File> files = new ArrayList<File>();
	    for(String file: args)
	    	files.add(new File(file));
	    
	    d.load(files);
	    
	    for(int relation: d.relationSize.keySet()){
	    	System.out.println(relation + "\t" + getRelationDomain(d, relation) 
	    			+ "\t" + getRelationRange(d, relation));
	    }
	}
}
