package amie.data;

import amie.data.tuple.IntArrays;
import amie.data.tuple.IntPair;
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
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import amie.data.javatools.datatypes.IntHashMap;
import amie.data.javatools.filehandlers.FileLines;

public class Schema {

	public static String top = "owl:Thing";
	public int topBS ;

	/** X rdf:type Class **/
	public static String typeRelation = "rdf:type";

	public int typeRelationBS ;

	/** Class1 rdfs:subClassOf Class2 **/
	public static String subClassRelation = "rdfs:subClassOf";

	public int subClassRelationBS ;

	/** relation1 rdfs:subPropertyOf relation2 **/
	public static String subPropertyRelation = "rdfs:subPropertyOf";

	public int subPropertyRelationBS ;

	/** Class rdfs:domain relation **/
	public static String domainRelation = "rdfs:domain";

	public int domainRelationBS ;

	/** Class rdfs:domain range **/
	public static String rangeRelation = "rdfs:range";

	public int rangeRelationBS ;

	public IntList schemaRelationsBS ;

	public static List<String> schemaRelations = Arrays.asList(typeRelation, subClassRelation,
			subPropertyRelation, domainRelation, rangeRelation);

	private static boolean taxonomyMaterialized = false;

	private static IntSet allDefinedTypesMaterialized = new IntOpenHashSet();

	private static Int2ObjectMap<IntSet> subClassMaterialized = new Int2ObjectOpenHashMap<>();

	private static Int2ObjectMap<IntSet> superClassMaterialized = new Int2ObjectOpenHashMap<>();

	protected ArrayList<String> idToEntity = new ArrayList<>(Arrays.asList("null"));
	protected Lock mappingLock = new ReentrantLock();
	protected ArrayList<String> compositeEntity = new ArrayList<>();

	protected Object2IntMap<String> entityToId = new Object2IntOpenHashMap<String>();
	protected Object2IntMap<String> compositeToId = new Object2IntOpenHashMap<String>();

	public final Map<String,String> prefixMap=new HashMap<>();

	/** Variable sign (as defined in SPARQL) **/
	public static final char VariableSign = '?';

	private static final String VariableRegex = Pattern.quote(Character.toString(VariableSign))
			+ "(_)?([a-z])([0-9])?([0-9])?";

	private static final Pattern VariablePattern = Pattern.compile(VariableRegex);

	public Schema() {
		idToEntity = new ArrayList<>(Arrays.asList("null"));
		mappingLock = new ReentrantLock();
		compositeEntity = new ArrayList<>();

		entityToId = new Object2IntOpenHashMap<String>();
		compositeToId = new Object2IntOpenHashMap<String>();

		topBS = map(top) ;
		typeRelationBS = map(typeRelation) ;
		subClassRelationBS = map(subClassRelation) ;
		subPropertyRelationBS = map(subPropertyRelation) ;
		domainRelationBS = map(domainRelation) ;
		rangeRelationBS = map(rangeRelation) ;

		schemaRelationsBS = IntArrays.asList(typeRelationBS, subClassRelationBS,
				subPropertyRelationBS, domainRelationBS, rangeRelationBS) ;
	}

	public static boolean isComposite(int id) {
		return id <= -2048;
	}

	public int mapComposite(CharSequence cs) {
		String b = _compress(cs);
		int r;
		mappingLock.lock();
		if (entityToId.containsKey(b)) {
			throw new IllegalStateException(cs.toString() + " is used as usual and composite entity");
		}
		if (compositeToId.containsKey(b)) {
			r = compositeToId.getInt(b);
		} else {
			compositeEntity.add(b);
			compositeToId.put(b, -2047-compositeEntity.size());
			r = -2047-compositeEntity.size();
		}
		mappingLock.unlock();
		return r;
	}

	public static final String hasNumberOfValuesEquals = "hasNumberOfValuesEquals";

	public static final String hasNumberOfValuesEqualsInv = "hasNumberOfValuesEqualsInv";

	public final int hasNumberOfValuesEqualsBS = mapComposite(hasNumberOfValuesEquals);

	public final int hasNumberOfValuesEqualsInvBS = mapComposite(hasNumberOfValuesEqualsInv);

	public static final String hasNumberOfValuesGreaterThan = "hasNumberOfValuesGreaterThan";

	public static final String hasNumberOfValuesGreaterThanInv = "hasNumberOfValuesGreaterThanInv";

	public final int hasNumberOfValuesGreaterThanBS = mapComposite(hasNumberOfValuesGreaterThan);

	public final int hasNumberOfValuesGreaterThanInvBS = mapComposite(hasNumberOfValuesGreaterThanInv);

	public static final String hasNumberOfValuesSmallerThan = "hasNumberOfValuesSmallerThan";

	public static final String hasNumberOfValuesSmallerThanInv = "hasNumberOfValuesSmallerThanInv";

	public final int hasNumberOfValuesSmallerThanBS = mapComposite(hasNumberOfValuesSmallerThan);

	public final int hasNumberOfValuesSmallerThanInvBS = mapComposite(hasNumberOfValuesSmallerThanInv);

	public final IntList cardinalityRelations = IntArrays.asList(hasNumberOfValuesEqualsBS,
			hasNumberOfValuesEqualsInvBS, hasNumberOfValuesGreaterThanBS, hasNumberOfValuesGreaterThanInvBS,
			hasNumberOfValuesSmallerThanBS, hasNumberOfValuesSmallerThanInvBS);

	private static final String cardinalityRelationsRegex = "(" +
			hasNumberOfValuesEquals + "|" + hasNumberOfValuesGreaterThan + "|" + hasNumberOfValuesSmallerThan + "|" +
			hasNumberOfValuesEqualsInv + "|" + hasNumberOfValuesGreaterThanInv + "|" + hasNumberOfValuesSmallerThanInv +
			")([0-9]+)";

	private static final Pattern cardinalityRelationsRegexPattern =
			Pattern.compile(cardinalityRelationsRegex);

	public static final int COMPOSE_SIZE = 15;

	public static int compose(int id, int n) {
		return -(-id + (n << COMPOSE_SIZE));
	}

	public static IntPair uncompose(int c) {
		if (!isComposite(c)) { return null; }
		return new IntPair(-((-c) % ((1 << COMPOSE_SIZE))), (-c) >> COMPOSE_SIZE);
	}

	public int mapComposite(CharSequence cs, int n) {
		return compose(mapComposite(cs), n);
	}

	private int parseCardinality(CharSequence cs) {
		Matcher m = cardinalityRelationsRegexPattern.matcher(cs);
		if (m.matches()) {
			return mapComposite(m.group(1), Integer.parseInt(m.group(2)));
		}
		return 0;
	}
	/**
	 * Determines whether the relation has
	 * @param composite
	 * @return
	 */
	public IntPair parseCardinalityRelation(int composite) {
		IntPair r = uncompose(composite);
		if (r == null) { return null; }
		if (!cardinalityRelations.contains(r.first)) { return null; }
		return r;
	}

	public String unmapComposite(int composite) {
		IntPair p = uncompose(composite);
		return compositeEntity.get(-p.first-2048).toString() + Integer.toString(p.second);
	}

	/** TRUE if the int is a SPARQL variable */
	public static boolean isVariable(CharSequence s) {
		return (s.length() > 0 && s.charAt(0) == VariableSign);
	}

	public static boolean isVariable(int s) {
		return (s < 0 && s > -2048);
	}

	public static boolean isOpenableVariable(CharSequence s) {
		return (s.length() > 1 && s.charAt(0) == VariableSign && s.charAt(1) != '_');
	}

	public static boolean isOpenableVariable(int s) {
		return (s < 0 && s > -1024);
	}

	/**
	 * Map a variable of the form [a-z][0-9]{1-2} to an integer between 1 and 1023.
	 * @param letter
	 * @param num1
	 * @param num2
	 * @return
	 */
	private static int mapVariable(char letter, int num1, int num2) {
		int r = letter - 96;
		if (num1 < 0) {
			return r;
		} else {
			if (num2 < 0) {
				return ((r-1) * 10) + num1 + 27;
			} else {
				return Math.min(((r-1) * 100) + num1*10 + num2 + 287, 1023);
			}
		}
	}

	public static int parseVariable(CharSequence cs) {
		Matcher m = VariablePattern.matcher(cs);
		if (m.matches()) {
			if (m.group(1) == null) {
				return -mapVariable(m.group(2).charAt(0), (m.group(3) == null) ? -1 : Integer.parseInt(m.group(3)),
						(m.group(4) == null) ? -1 : Integer.parseInt(m.group(4)));
			} else {
				return -1023-mapVariable(m.group(2).charAt(0), (m.group(3).isEmpty()) ? -1 : Integer.parseInt(m.group(3)),
						(m.group(4) == null) ? -1 : Integer.parseInt(m.group(4)));
			}
		}
		throw new IllegalArgumentException("Variable " + cs.toString() + " DO NOT MATCH \"\\?(_?)[a-z][0-9]{1,2}\"");
	}

	private static void unmapVariable(int pos, StringBuilder sb) {
		if (pos <= 26) {
			sb.append((char) (pos + 96));
		} else if (pos <= 286) {
			pos -= 27; // ?a0 <-> -27
			sb.append((char) (pos / 10 + 97));
			sb.append(pos % 10);
		} else {
			pos -= 287; // ?a00 <-> -287
			sb.append((char) (pos / 100 + 97));
			sb.append(pos % 100);
		}
	}

	public static String unparseVariable(int v) {
		StringBuilder sb = new StringBuilder();
		sb.append(VariableSign);
		if (!isOpenableVariable(v)) {
			sb.append('_');
			v += 1023;
		}
		unmapVariable(-v, sb);
		return sb.toString();
	}

	public String unmap(int e) {
		if (isComposite(e)) {
			return unmapComposite(e);
		}
		if (isVariable(e)) {
			return unparseVariable(e);
		}
		if (e < idToEntity.size()) {
			return idToEntity.get(e).toString();
		}

		throw new IllegalArgumentException("Cannot unmap invalid id: " + e + " (/" + idToEntity.size() + ")");
	}

	public int map(CharSequence cs) {
		if (isVariable(cs)) {
			return parseVariable(cs);
		}

		int r = 0;
		if ((r = parseCardinality(cs)) != 0) {
			return r;
		}

		String b = _compress(cs);
		mappingLock.lock();
		if (compositeToId.containsKey(b)) {
			r = mapComposite(b);
		} else if (entityToId.containsKey(b)) {
			r = entityToId.getInt(b);
		} else {
			r = idToEntity.size();
			idToEntity.add(b);
			entityToId.put(b, r);
		}
		mappingLock.unlock();
		return r;
	}


	/** Compresses a string to an internal string */
	private static String _compress(CharSequence s) {
		if (s instanceof String) {
			return (String) s;
		}
		String str = s.toString();
		int pos = str.indexOf("\"^^");
		if (pos != -1)
			str = str.substring(0, pos + 1);
		return str;
	}


	public void materializeTaxonomy(AbstractKB source) {
		List<int[]> query = KB.triples(source.triple("?x", subClassRelation, "?y"));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(map("?x"), query));
		allDefinedTypesMaterialized.addAll(source.selectDistinct(map("?y"), query));
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

	public void loadSchemaConf() {
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
					amie.data.U.class.getField(lineParts[0] + "BS").set(null, map(lineParts[1]));
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
	public boolean isSchemaRelation(int relation) {
		return this.schemaRelationsBS.contains(relation);
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
	public int getRelationDomain(AbstractKB source, int relation){
		List<int[]> query = KB.triples(KB.triple(relation, domainRelationBS, map("?x")));
		IntSet domains = source.selectDistinct(map("?x"), query);
		if(!domains.isEmpty()){
			return domains.iterator().nextInt();
		}

		//Try looking for the superproperty
		List<int[]> query2 = KB.triples(KB.triple(relation, subPropertyRelationBS, map("?y")),
				source.triple("?y", "rdfs:domain", "?x"));

		domains = source.selectDistinct(map("?x"), query2);
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
	public int getRelationRange(AbstractKB source, int relation){
		List<int[]> query = KB.triples(KB.triple(relation, rangeRelationBS, map("?x")));
		IntSet ranges = source.selectDistinct(map("?x"), query);
		if (!ranges.isEmpty()) {
			return ranges.iterator().nextInt();
		}

		//Try looking for the superproperty
		List<int[]> query2 = KB.triples(KB.triple(relation, subPropertyRelationBS, map("?y")),
				source.triple("?y", "rdfs:range", "?x"));

		ranges = source.selectDistinct(map("?x"), query2);
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
	public IntSet getMaterializedTypesForEntity(AbstractKB source, int entity){
		List<int[]> query = KB.triples(KB.triple(entity, typeRelationBS, map("?x")));
		return source.selectDistinct(map("?x"), query);
	}

	/**
	 * Determines whether a given type is specific, that is, it does not have subclasses.
	 * @param source
	 * @param type
	 * @return
	 */
	public boolean isLeafDatatype(AbstractKB source, int type){
		if (taxonomyMaterialized) {
			IntSet subTypes = subClassMaterialized.get(type);
			return subTypes == null || subTypes.isEmpty();
		}
		List<int[]> query = KB.triples(KB.triple(map("?x"), subClassRelationBS, type));
		return source.countDistinct(map("?x"), query) == 0;
	}

	/**
	 * It returns the most specific types of an entity according to the type hierarchy
	 * of the knowledge base.
	 * @param source
	 * @param entity
	 * @return
	 */
	public IntSet getLeafTypesForEntity(AbstractKB source, int entity){
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
	public IntSet getAllTypesForEntity(AbstractKB source, int entity){
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
	public IntSet getSuperTypes(AbstractKB source, int type){
		List<int[]> query = KB.triples(KB.triple(type, subClassRelationBS, map("?x")));
		return source.selectDistinct(map("?x"), query);
	}

	/**
	 * It returns all the supertypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public IntSet getAllSuperTypes(AbstractKB source, int type) {

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
	public IntSet getAllEntitiesForType(AbstractKB source, int type) {
		List<int[]> query = KB.triples(KB.triple(map("?x"), typeRelationBS, type));
		return new IntOpenHashSet(source.selectDistinct(map("?x"), query));
	}

	/**
	 * Returns the number of instances of the given class in a KB
	 * @param kb
	 * @param type
	 * @return
	 */
	public long getNumberOfEntitiesForType(AbstractKB kb, int type) {
		return kb.count(map("?s"), typeRelationBS, type);
	}

	/**
	 * Returns all present data types in the given KB.
	 * @param kb
	 */
	public IntSet getAllTypes(AbstractKB kb) {
		List<int[]> query = KB.triples(kb.triple("?x", typeRelation, "?t9"));
		return new IntOpenHashSet(kb.selectDistinct(map("?t9"), query));
	}

	/**
	 * Gets all the entities of the type of the given relation's domain.
	 * @param source
	 * @param relation
	 * @return
	 */
	public IntSet getDomainSet(KB source, int relation) {
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
	public IntSet getDomainSet(AbstractKB source, int relation,
							   int domainType) {
		List<int[]> query = null;
		String queryVar = "?s";
		query = KB.triples(KB.triple(map("?s"), relation, map("?o")),
				KB.triple(map(queryVar),
						typeRelationBS, domainType));

		return source.selectDistinct(map(queryVar), query);
	}


	/**
	 * Get all the immediate subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public IntSet getSubTypes(AbstractKB source, int type) {
		List<int[]> query = KB.triples(KB.triple(map("?x"), subClassRelationBS, type));
		return new IntOpenHashSet(source.selectDistinct(map("?x"), query));
	}

	public IntSet getSubtypes(AbstractKB source, int type) {
		return getSubTypes(source, type);
	}

	/**
	 * Get all subtypes of a given type.
	 * @param source
	 * @param type
	 * @return
	 */
	public IntSet getAllSubTypes(AbstractKB source, int type) {

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
	public IntSet getRangeSet(KB source, int relation) {
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
	public IntSet getRangeSet(AbstractKB source, int relation,
							  int rangeType) {
		List<int[]> query = null;
		String queryVar = "?o";
		query = KB.triples(KB.triple(map("?s"), relation, map("?o")),
				KB.triple(map(queryVar),
						typeRelationBS, rangeType));

		return source.selectDistinct(map(queryVar), query);
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
	public IntHashMap<Integer> getHistogramOnDomain(AbstractKB kb, int relation) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<int[]> query = null;
		String queryVar = null;
		String existVar = null;
		int targetType = 0;

		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple(map("?s"), relation, map("?o")));
			targetType = getRelationDomain(kb, relation);
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple(map("?o"), relation, map("?s")));
			targetType = getRelationRange(kb, relation);
		}

		if (targetType == 0) {
			return hist;
		}

		IntSet effectiveDomain = kb.selectDistinct(map(queryVar), query);
		IntSet theorethicalDomain = getAllEntitiesForType(kb, targetType);
		effectiveDomain.retainAll(theorethicalDomain);
		for (int entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, map(existVar)));
			} else {
				val = kb.count(KB.triple(map(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(map(existVar), query);

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
	public IntHashMap<Integer> getHistogramOnDomain(AbstractKB kb,
													int relation, int domainType) {
		IntHashMap<Integer> hist = new IntHashMap<>();
		List<int[]> query = null;
		String queryVar = null;
		String existVar = null;

		if (kb.isFunctional(relation)) {
			queryVar = "?s";
			existVar = "?o";
			query = KB.triples(KB.triple(map("?s"), relation, map("?o")),
					KB.triple(map("?s"),
							typeRelationBS, domainType));
		} else {
			queryVar = "?o";
			existVar = "?s";
			query = KB.triples(KB.triple(map("?o"), relation, map("?s")),
					KB.triple(map("?o"),
							typeRelationBS, domainType));
		}

		IntSet effectiveDomain = kb.selectDistinct(map(queryVar), query);
		for (int entity : effectiveDomain) {
			long val;
			if (kb.isFunctional(relation)) {
				val = kb.count(KB.triple(entity, relation, map(existVar)));
			} else {
				val = kb.count(KB.triple(map(queryVar), relation, entity));
			}
			hist.increase((int)val);
		}
		kb.selectDistinct(map(existVar), query);

		return hist;
	}

	/**
	 * It returns a map containing the number of instances of each class in the KB.
	 * @param kb
	 * @return
	 */
	public Int2IntMap getTypesCount(KB kb) {
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
			List<int[]> query = KB.triples(kb.triple("?s", typeRelation, "?o"));
			types2Instances = new Int2ObjectOpenHashMap<>();
			Int2ObjectMap<IntSet> ts =
					kb.selectDistinct(map("?o"), map("?s"), query);
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
	public Int2ObjectMap<Int2IntMap> getTypesIntersectionCount(KB kb) {
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
		List<int[]> query = KB.triples(kb.triple("?s", typeRelation, "?o1"), kb.triple("?s", typeRelation, "?o2"));
		Int2ObjectMap<Int2ObjectMap<IntSet>> types2types2Instances =
				kb.selectDistinct(map("?o1"), map("?o2"), map("?s"), query);

		for (int type1 : types2types2Instances.keySet()) {
			Int2IntMap result2 = new Int2IntOpenHashMap();
			result.put(type1, result2);
			for (int type2 : types2types2Instances.get(type1).keySet())
				result2.put(type2, types2types2Instances.get(type1).get(type2).size());
		}
		return result;
	}

	public Int2IntMap loadTypesCount(File f) throws IOException {
		Int2IntMap typesCount = new Int2IntOpenHashMap();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 2) {
				try {
					typesCount.put(map(split[0]), Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {}
			}
		}
		System.out.println("Loaded " + typesCount.size() + " classes");
		return typesCount;
	}

	public Int2ObjectMap<Int2IntMap> loadTypesIntersectionCount(File f) throws IOException {
		Int2ObjectMap<Int2IntMap> typesIntersectionCount = new Int2ObjectOpenHashMap<>();
		for (String line : new FileLines(f, "UTF-8", null)) {
			String[] split = line.split("\t");
			if (split.length == 3) {
				try {
					int i = Integer.parseInt(split[2]);
					Int2IntMap tc = typesIntersectionCount.get(map(split[0]));
					if (tc == null) {
						typesIntersectionCount.put(map(split[0]), tc = new Int2IntOpenHashMap());
					}
					tc.put(map(split[1]), i);
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
	public boolean isSuperType(AbstractKB kb, int parentType, int childType) {
		IntSet st1 = getSuperTypes(kb, childType);
		return st1.contains(parentType);
	}

	public boolean isTransitiveSuperType(AbstractKB kb, int parentType, int childType) {
		IntSet st1 = getAllSuperTypes(kb, childType);
		return st1.contains(parentType);
	}


	public void main(String args[]) throws IOException {
		KB d = new KB(new Schema());
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
