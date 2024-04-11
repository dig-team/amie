//package amie.data.javatools.datatypes;
//
//import java.util.AbstractList;
//import java.util.Arrays;
//import java.util.Set;
//import java.util.Spliterator;
//
//import amie.data.javatools.administrative.D;
//
///**
// * This class is part of the Java Tools (see
// * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
// * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
// * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
// *
// *
// *
// *
// *
// * This class provides a very simple container implementation with zero
// * overhead. A FinalSet bases on a sorted, unmodifiable array. The constructor
// * can either be called with a sorted unmodifiable array (default constructor)
// * or with an array that can be cloned and sorted beforehand if desired.
// * Example:
// *
// * <PRE>
// *    FinalSet<String> f=new FinalSet("a","b","c");
// *    // equivalently:
// *    //   FinalSet<String> f=new FinalSet(new String[]{"a","b","c"});
// *    //   FinalSet<String> f=new FinalSet(SHALLNOTBECLONED,ISSORTED,"a","b","c");
// *    System.out.println(f.get(1));
// *    --> b
// * </PRE>
// */
//public class FinalSet<T extends Comparable<?>> extends AbstractList<T>
//		implements Set<T> {
//	/** Holds the data, must be sorted */
//	public T[] data;
//
//	/** Constructs a FinalSet from an array, clones the array if indicated. */
//	@SuppressWarnings("unchecked")
//	public FinalSet(boolean clone, T... a) {
//		if (clone) {
//			Comparable<?>[] b = new Comparable[a.length];
//			System.arraycopy(a, 0, b, 0, a.length);
//			a = (T[]) b;
//		}
//		Arrays.sort(a);
//		data = a;
//	}
//
//	/** Constructs a FinalSet from an array that does not need to be cloned */
//	public FinalSet(T... a) {
//		this(false, a);
//	}
//
//
//	/** Returns the element at position i */
//	public T get(int i) {
//		return (data[i]);
//	}
//
//	/** Returns the number of elements in this FinalSet */
//	public int size() {
//		return (data.length);
//	}
//
//	public Spliterator<T> spliterator() {
//    return super.spliterator();
//
//	}
//
//	/** Test routine */
//	public static void main(String[] args) {
//		Set<String> f = Set.of("b", "a", "c");
//		f.forEach(e -> D.p(e));
//	}
//}
