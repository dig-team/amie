package amie.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

	private static boolean taxonomyMaterialized = false;
	
	private static IntSet allDefinedTypesMaterialized = new IntOpenHashSet();
	
	private static Int2ObjectMap<IntSet> subClassMaterialized = new Int2ObjectOpenHashMap<>();
	
	private static Int2ObjectMap<IntSet> superClassMaterialized = new Int2ObjectOpenHashMap<>();

	public static boolean isTaxonomyMaterialized() {
		return taxonomyMaterialized;
	}
	
	public static IntSet getAllDefinedTypes() {
		return IntSets.unmodifiable(allDefinedTypesMaterialized);
	}

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
