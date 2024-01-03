package amie.data;

import amie.data.tuple.IntTriple;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;


import javatools.datatypes.IntHashMap;
import javatools.datatypes.Triple;

/**
 * Set of commonly used functions.
 * 
 * @author lgalarra
 *
 */
public class U {

        // IntHashMap utilities
    
        public static IntList decreasingKeys(Int2IntMap r) {
            IntList result = new IntArrayList(r.keySet());
            result.unstableSort((int e1, int e2) -> Integer.compare(r.get(e2), r.get(e1)));
            return result;
        }
    
        public static void increase(Int2IntMap r, Int2IntMap p) {
            for (int k : p.keySet()) {
                increase(r, k, p.get(k));
            }
        }
        
        public static void increase(Int2IntMap r, Int2ObjectMap<IntSet> p) {
            for (int k : p.keySet()) {
                increase(r, k, p.get(k).size());
            }
        }
        
        public static void increase(Int2IntMap r, IntSet p) {
            for (int k : p) {
                increase(r, k);
            }
        }
        
        public static void increase(Int2IntMap r, int k, int delta) {
            r.put(k, r.getOrDefault(k, 0) + delta);
            //r.put(k, r.get(k, 0) + delta);
        }
        
        public static void increase(Int2IntMap r, int k) {
            increase(r, k, 1);
        }
        
        public static void decrease(Int2IntMap r, Int2IntMap p) {
            for (int k : p.keySet()) {
                decrease(r, k, p.get(k));
            }
        }
        
        public static void decrease(Int2IntMap r, Int2ObjectMap<IntSet> p) {
            for (int k : p.keySet()) {
                decrease(r, k, p.get(k).size());
            }
        }
        
        public static void decrease(Int2IntMap r, IntSet p) {
            for (int k : p) {
                decrease(r, k);
            }
        }
        
        public static void decrease(Int2IntMap r, int k, int delta) {
            r.put(k, r.getOrDefault(k, 0) - delta);
            //r.put(k, r.get(k, 0) + delta);
        }
        
        public static void decrease(Int2IntMap r, int k) {
            decrease(r, k, 1);
        }
        
        /**
	 * Dequeues an element from the given collection. It returns null
	 * if the collection is empty.
	 */
	public static <T> T poll(Collection<T> collection) {
		if (collection.isEmpty()) 
			return null;
		Iterator<T> it = collection.iterator();
		T obj = it.next();
		it.remove();
		return obj;
	}
	
	/**
	 * It enumerates all the subsets of indexes of size 'n' for a collection of items of size 
	 * 'collectionSize'.
	 * @param collectionSize
	 * @param n
	 */
	private static void subsetsOfSize(int collectionSize, int size, List<int[]> output) {
		if (size == 1) {
			for (int i = 0; i < collectionSize; ++i) {
				output.add(new int[]{i});
			}
		} else if (size > 1) {
			List<int[]> setsOfSizeNMinus1 = new ArrayList<int[]>();
			subsetsOfSize(collectionSize, size - 1, setsOfSizeNMinus1);
			for (int[] s : setsOfSizeNMinus1) {
				for (int i = s[s.length - 1] + 1; i < collectionSize; ++i) {
					int[] newSet = new int[s.length + 1];
					for (int k = 0; k < s.length; ++k) {
						newSet[k] = s[k];
					}
					newSet[s.length] = i; 
					output.add(newSet);
				}
			}
		}
	}
	
	
	/**
	 * Recursively enumerates all the subsets of indexes of size 'n' for a collection of items of size 
	 * 'collectionSize'.
	 * @param collectionSize
	 * @param n
	 */
	public static List<int[]> subsetsOfSize(int collectionSize, int size) {
		List<int[]> results = new ArrayList<int[]>();
		subsetsOfSize(collectionSize, size, results);
		return results;
	}

	/**
	 * Enumerates all the subsets of indexes up to size 'n' for a collection of items of size 
	 * 'collectionSize'.
	 * @param collectionSize
	 * @param n
	 * @return
	 */
/**	public static List<int[]> subsetsUpToSize(int collectionSize, int n) {
		List<int[]> subsets = new ArrayList<int[]>();
		subsetsOfSize(collectionSize, n, subsets);
		return subsets;
	} **/
	
	public static void main(String[] args) {		
		for (int[] x : subsetsOfSize(10, 4)) {
			System.out.println(Arrays.toString(x));
		}
	}
	
	/**
	 * Applies reservoir sampling to a collection of items: http://www.geeksforgeeks.org/reservoir-sampling/
	 * @param someCollection
	 * @param sampleSize
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> reservoirSampling(Collection<T> someCollection, int sampleSize) {
		//Now sample them
		Collection<T> result = new ArrayList<>(sampleSize);	
		ArrayList<T> resultArrayList = (ArrayList<T>)result;
		if(someCollection.size() <= sampleSize){
			return someCollection;
		}else{
			Object[] candidates = someCollection.toArray();
			int i;
			Random r = new Random();
			for(i = 0; i < sampleSize; ++i){				
				result.add((T)candidates[i]);
			}
			
			while(i < candidates.length){
			    int rand = r.nextInt(i);
			    if(rand < sampleSize){
			    	//Pick a random number in the reservoir.
			    	resultArrayList.set(r.nextInt(sampleSize), (T)candidates[i]);
			    }
			    ++i;
			}
		}
		
		return result;
	}
	
	/**
	 * Return the full string representation of a IntHashMap
	 * @param histogram
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <K> String toString(IntHashMap<K> histogram) {
		if (histogram.isEmpty())
			return ("{}");
		StringBuilder b = new StringBuilder("{");
		for (K key : histogram.keys()) {
			b.append(key).append('=').append(histogram.get(key)).append(", ");
		}
		b.setLength(b.length() - 2);
		return (b.append("}").toString());
	}
	
	/**
	 * Adds a value to a multimap, represented as a map where the values
	 * are lists of objects.
	 * @param map
	 * @param key
	 * @param value
	 * @return true if the key already existed in the map.
	 */
	public static <K, V> boolean addToMap(Map<K, List<V>> map, K key, V value) {
		List<V> objects = map.get(key);
		boolean keyExists = true;
		if (objects == null) {
			objects = new ArrayList<V>();
			map.put(key, objects);
			keyExists = false;
		}
		objects.add(value);
		return keyExists;
	}
	
	/**
	 * It performs a KB coalesce between 2 KBs consisting of all the facts in both ontologies
	 * for the intersection of all entities in the first KB with the subjects of the second KB.
	 * @param source1
	 * @param source2
	 * @param withObjs If true, the coalesce is done between all the entities in the first KB
	 * and all the entities in the second KB.
	 */
	public static void coalesce(KB source1, 
			KB source2, boolean withObjs) {
		IntSet sourceEntities = new IntOpenHashSet();
		sourceEntities.addAll(source1.subjectSize.keySet());
		sourceEntities.addAll(source1.objectSize.keySet());
		for(int entity: sourceEntities){
			//Print all facts of the source ontology
			Int2ObjectMap<IntSet> tail1 = source1.subject2relation2object.get(entity);
			Int2ObjectMap<IntSet> tail2 = source2.subject2relation2object.get(entity);
			if(tail2 == null)
				continue;
						
			for(int predicate: tail1.keySet()){
				for(int object: tail1.get(predicate)){
					System.out.println(entity + "\t" + predicate + "\t" + object);
				}
			}
			//Print all facts in the target ontology
			for(int predicate: tail2.keySet()){
				for(int object: tail2.get(predicate)){
					System.out.println(entity + "\t" + predicate + "\t" + object);
				}
			}
		}
		
		if(withObjs){
			for(int entity: source2.objectSize.keySet()){
				if(sourceEntities.contains(entity)) continue;
				
				Int2ObjectMap<IntSet> tail2 = source2.subject2relation2object.get(entity);
				if(tail2 == null) continue;
				
				//Print all facts in the target ontology
				for(int predicate: tail2.keySet()){
					for(int object: tail2.get(predicate)){
						System.out.println(entity + "\t" + predicate + "\t" + object);
					}
				}
			}
		}
	}
	
	/**
	 * 
	 * @param source
	 */
	public static void printOverlapTable(KB source) {
		//for each pair of relations, print the overlap table
		System.out.println("Relation1\tRelation2\tRelation1-subjects"
				+ "\tRelation1-objects\tRelation2-subjects\tRelation2-objects"
				+ "\tSubject-Subject\tSubject-Object\tObject-Subject\tObject-Object");
		for(int r1: source.relationSize.keySet()){
			IntSet subjects1 = source.relation2subject2object.get(r1).keySet();
			IntSet objects1 = source.relation2object2subject.get(r1).keySet();
			int nSubjectsr1 = subjects1.size();
			int nObjectsr1 = objects1.size();
			for(int r2: source.relationSize.keySet()){
				if(r1 == (r2))
					continue;				
				System.out.print(r1 + "\t");
				System.out.print(r2 + "\t");
				IntSet subjects2 = source.relation2subject2object.get(r2).keySet();
				IntSet objects2 = source.relation2object2subject.get(r2).keySet();
				int nSubjectr2 = subjects2.size();
				int nObjectsr2 = objects2.size();
				System.out.print(nSubjectsr1 + "\t" + nObjectsr1 + "\t" + nSubjectr2 + "\t" + nObjectsr2 + "\t");
				System.out.print(computeOverlap(subjects1, subjects2) + "\t");
				System.out.print(computeOverlap(subjects1, objects2) + "\t");
				System.out.print(computeOverlap(subjects2, objects1) + "\t");
				System.out.println(computeOverlap(objects1, objects2));
			}
		}		
	}
		
	public static KB loadFiles(String args[]) throws IOException {
            return loadFiles(args, "\t");
        }
	/**
	 * Returns a KB with the content of all the files referenced in the string array.
	 * @param args
         * @param delimiter
	 * @return
	 * @throws IOException
	 */
	public static KB loadFiles(String args[], String delimiter) throws IOException {
		// Load the data
		KB kb = new KB();
                kb.setDelimiter(delimiter);
		List<File> files = new ArrayList<File>();
		for (int i = 0; i < args.length; ++i) {
			files.add(new File(args[i]));
		}
		kb.load(files);
		return kb;
	}
	
	/**
	 * Returns a KB with the content of all the files referenced in the object array.
	 * Each element of the array is converted to a string object
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public static KB loadFiles(Object args[]) throws IOException {
		// Load the data
		KB kb = new KB();
		List<File> files = new ArrayList<File>();
		for (int i = 0; i < args.length; ++i) {
			files.add(new File((String)args[i]));
		}
		kb.load(files);
		return kb;
	}
	
	
	/**
	 * Returns a KB with the content of all the files referenced in the string array
	 * starting from a given position.
	 * @param args
	 * @return
	 * @throws IOException
	 */
	public static KB loadFiles(String args[], int fromIndex) throws IOException {
		if (fromIndex >= args.length)
			throw new IllegalArgumentException("Index " + fromIndex + 
					" equal or bigger than size of the array.");
		// Load the data
		KB kb = new KB();
		List<File> files = new ArrayList<File>();
		for (int i = fromIndex; i < args.length; ++i) {
			files.add(new File(args[i]));
		}
		kb.load(files);
		return kb;
	}
	
	/**
	 * Returns a KB with the content of all the files referenced in the string array.
	 * @param args
	 * @return
	 * @throws IOException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static KB loadFiles(String args[], Class kbSubclass) 
			throws IOException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		// Load the data
		KB kb = (KB) kbSubclass.getConstructor().newInstance();
		List<File> files = new ArrayList<File>();
		for (int i = 0; i < args.length; ++i) {
			files.add(new File(args[i]));
		}
		kb.load(files);
		return kb;
	}
	
	/**
	 * Returns a KB with the content of all the files referenced in the subarray starting
	 * at the given index of the input array 'args'.
	 * @param args
	 * @return
	 * @throws IOException
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static KB loadFiles(String args[], int fromIndex, Class kbSubclass) 
			throws IOException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		// Load the data
		KB kb = (KB) kbSubclass.getConstructor().newInstance();
		List<File> files = new ArrayList<File>();
		for (int i = fromIndex; i < args.length; ++i) {
			files.add(new File(args[i]));
		}
		kb.load(files);
		return kb;
	}

	/**
	 * 
	 * @param subjects1
	 * @param subjects2
	 * @return
	 */
	private static int computeOverlap(IntSet subjects1,
			IntSet subjects2) {
		int overlap = 0; 
		for(int entity1 : subjects1){
			if(subjects2.contains(entity1))
				++overlap;
		}
		
		return overlap;
	}
	


	
	/**
	 * It returns the number of facts where the given entity participates as
	 * a subject or object.
	 * @param kb
	 * @param entity
	 * @return
	 */
	public static int numberOfFacts(KB kb, int entity) {
		int[] querySubject = KB.triple(entity, KB.map("?r"), KB.map("?o")); 
		int[] queryObject = KB.triple(KB.map("?s"), KB.map("?r"), entity); 
		return (int)kb.count(querySubject) + (int)kb.count(queryObject);
	}
	
	/**
	 * It returns the number of facts where the given entity participates as
	 * a subject or object.
	 * @param kb
	 * @param entity
	 * @param omittedRelations These relations are not counted as facts.
	 * @return
	 */
	public static int numberOfFacts(KB kb, int entity, IntCollection omittedRelations) {
		int[] querySubject = KB.triple(entity, KB.map("?r"), KB.map("?o")); 
		int[] queryObject = KB.triple(KB.map("?s"), KB.map("?r"), entity); 
		Int2ObjectMap<IntSet> relationsSubject = 
				kb.resultsTwoVariables(KB.map("?r"), KB.map("?o"), querySubject);
		Int2ObjectMap<IntSet> relationsObject = 
				kb.resultsTwoVariables(KB.map("?r"), KB.map("?s"), queryObject);
		int count1 = 0;
		int count2 = 0;
		for (int relation : relationsSubject.keySet()) {
			if (!omittedRelations.contains(relation))
				count1 += relationsSubject.get(relation).size();
		}
		
		for (int relation : relationsObject.keySet()) {
			if (!omittedRelations.contains(relation))
				count1 += relationsObject.get(relation).size();
		}

		return count1 + count2;
	}
	
	/**
	 * Returns true if the relation is defined as a function.
	 * @return
	 */
	public static boolean isFunction(KB kb, int relation) {
		return kb.contains(relation, KB.map("<isFunction>"), KB.map("TRUE"));
	}
	
	/**
	 * Returns true if the relation is defined as compulsory for all members 
	 * of its domain (this function assumes relations are always analyzed from 
	 * their most functional side.
	 * @return
	 */
	public static boolean isMandatory(KB kb, int relation) {
		return kb.contains(relation, KB.map("<isMandatory>"), KB.map("TRUE"));
	}

	/**
	 * It returns all the entities that have 'cardinality' different number of values
	 * for the given relation.
	 * @param kb
	 * @param relation
	 * @param cardinality
	 * @return
	 */
	public static IntSet getEntitiesWithCardinality(KB kb, int relation, int cardinality) {
		Int2ObjectMap<IntSet> results = null;
		List<int[]> query = KB.triples(KB.triple(KB.map("?s"), 
				relation, KB.map("?o")));
		if (kb.isFunctional(relation)) {
			results = kb.selectDistinct(KB.map("?s"), KB.map("?o"), query);
		} else {
			results = kb.selectDistinct(KB.map("?o"), KB.map("?s"), query);			
		}
		IntSet entities = new IntOpenHashSet();
		for (int e : results.keySet()) {
			if (results.get(e).size() == cardinality) {
				entities.add(e);
			}
		}
		return entities;
	}

	/**
	 * Outputs a list of objects separated by tabs in one line.
	 * @param list
	 */
	public static <T> void tsvOutput(List<T> line) {
		for (int i = 0; i < line.size() - 1; ++i) {
			System.out.print(line.get(i) + "\t");
		}	
		System.out.println(line.get(line.size() - 1));
	}
	
	/**
	 * Prints a IntHashMap representing a histogram.
	 * @param histogram
	 */
	public static void printHistogram(IntHashMap<Integer> histogram) {
		for (Integer key : histogram.keys()) {			
			System.out.println(key + "\t" + histogram.get(key));
		}
	}
	
	/**
	 * Prints a histogram as well as the probability that X > Xi
	 * for each Xi in the histogram.
	 * @param histogram
	 */
	public static void printHistogramAndCumulativeDistribution(IntHashMap<Integer> histogram) {
		double total = 1.0;
		double accum = 0.0;
		double sum = histogram.computeSum();
		for (Integer key : histogram.keys()) {
			double prob = histogram.get(key) / sum;
			accum += prob;
			System.out.println(key + "\t" + histogram.get(key) + "\t" + prob + "\t" + (total - accum));
		}
	}
	
	/**
	 * It constructs a histogram based on a multimap.
	 * @param map
	 * @return
	 */
	public static <E, T> IntHashMap<Integer> buildHistogram(Map<T, List<E>> map) {
		IntHashMap<Integer> histogram = new IntHashMap<>();
		for (T key : map.keySet()) {
			histogram.increase(map.get(key).size());
		}
		return histogram;
	}
	
	/**
	 * Converts an array into a triple
	 * @param array
	 * @return
	 */
	public static <T> Triple<T, T, T> toTriple(T[] array) {
		if (array.length < 3) {
			return null;
		} else {
			return new Triple<T, T, T>(array[0], array[1], array[2]);
		}
	}
	
	/**
	 * Converts an array into a triple
	 * @param array
	 * @return
	 */
	public static int[] toArray(IntTriple triple) {
		return new int[] { triple.first, triple.second, triple.third};
	}
	
	/**
	 * Performs a deep clone of the given list, i.e., it returns a new list where 
	 * each element has been cloned.
	 * @param collection
	 */
	public static <T> List<T[]> deepClone(List<T[]> collection) {
		List<T[]> newList = new ArrayList<>(collection.size());
		for (T[] t : collection) {
			newList.add(t.clone());
		}
		return newList;
	}
        
        /**
	 * Performs a deep clone of the given list, i.e., it returns a new list where 
	 * each element has been cloned.
	 * @param collection
	 */
	public static List<int[]> deepCloneInt(List<int[]> collection) {
		List<int[]> newList = new ArrayList<>(collection.size());
		for (int[] t : collection) {
			newList.add(t.clone());
		}
		return newList;
	}
	
}
