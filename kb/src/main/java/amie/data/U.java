package amie.data;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import amie.data.javatools.datatypes.IntHashMap;

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



        public static void decrease(Int2IntMap r, int k, int delta) {
            r.put(k, r.getOrDefault(k, 0) - delta);
            //r.put(k, r.get(k, 0) + delta);
        }
        
        public static void decrease(Int2IntMap r, int k) {
            decrease(r, k, 1);
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

//

		
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
	 * It returns all the entities that have 'cardinality' different number of values
	 * for the given relation.
	 * @param kb
	 * @param relation
	 * @param cardinality
	 * @return
	 */
//	public static IntSet getEntitiesWithCardinality(KB kb, int relation, int cardinality) {
//		Int2ObjectMap<IntSet> results = null;
//		List<int[]> query = KB.triples(KB.triple(KB.map("?s"),
//				relation, KB.map("?o")));
//		if (kb.isFunctional(relation)) {
//			results = kb.selectDistinct(KB.map("?s"), KB.map("?o"), query);
//		} else {
//			results = kb.selectDistinct(KB.map("?o"), KB.map("?s"), query);
//		}
//		IntSet entities = new IntOpenHashSet();
//		for (int e : results.keySet()) {
//			if (results.get(e).size() == cardinality) {
//				entities.add(e);
//			}
//		}
//		return entities;
//	}

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
