package amie.data.javatools.administrative;

//import amie.data.javatools.parsers.Char17;

/** 
 This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
 It is licensed under the Creative Commons Attribution License 
 (see http://creativecommons.org/licenses/by/3.0) by 
 the YAGO-NAGA team (see http://mpii.de/yago-naga).
 
 
 

 
 This class provides convenience methods for Input/Output.
 Allows to do basic I/O with easy procedure calls
 -- nearly like in normal programming languages.
 Furthermore, the class provides basic set operations for EnumSets, NULL-safe
 comparisons and adding to maps.<BR>
 Example:
 <PRE>
 D.p("This is an easy way to write a string");
 // And this is an easy way to read one:
 String s=D.r();
 
 // Here is a cool way to print something inline
 computeProduct(factor1,(Integer)D.p(factor2));
 
 // Here are some tricks with enums
 enum T {a,b,c};
 EnumSet&lt;T> i=D.intersection(EnumSet.of(T.a,T.b),EnumSet.of(T.b,T.c));
 EnumSet&lt;T> u=D.union(EnumSet.of(T.a,T.b),EnumSet.of(T.b,T.c));
 
 // Here is how to compare things, even if they are NULL
 D.compare(object1, object2);
 
 // Here is how to add something to maps that contain lists
 Map&lt;String,List&lt;String>> string2list=new TreeMap&lt;String,List&lt;String>>();
 D.addKeyValue(string2list,"key","new list element",ArrayList.class); 
 // now, the map contains "key" -> [ "new list element" ]
 D.addKeyValue(string2list,"key","again a new list element",ArrayList.class);
 // now, the map contains "key" -> [ "new list element", "again a new list element" ]  

 // Here is how to add something to maps that contain integers
 Map&lt;String,Integer> string2list=new TreeMap&lt;String,Integer>();
 D.addKeyValue(string2list,"key",7); // map now contains "key" -> 7
 D.addKeyValue(string2list,"key",3); // map now contains "key" -> 10

 </PRE>  
 */
public class D {

  /** Indentation margin. All methods indent their output by indent spaces */
  public static int indent = 0;

  /** Prints <indent> spaces */
  protected static void i() {
    for (int i = 0; i < indent; i++)
      System.out.print(" ");
  }

  /** Prints some Objects, returns them */
  public static Object p(Object... a) {
    pl(a);
    System.out.println("");
    if (a == null || a.length == 0) return (null);
    if (a.length == 1) return (a[0]);
    return (a);
  }

  /** Prints some Objects on one line */
  public static void pl(Object... a) {    
    System.out.print(toString(a));
  }

  /** Prints an array of integers*/
  public static int[] p(int[] a) {
    i();
    if (a == null) System.out.print("null-array");
    else for (int i = 0; i < a.length; i++)
      System.out.print(a[i] + ", ");
    System.out.println("");
    return (a);
  }


  /** Waits for a number of milliseconds */
  public static void waitMS(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ex) {
    }
  }
  /** TRUE if the first enum is before the second*/
  public static <C extends Enum<C>> boolean smaller(Enum<C> e1, Enum<C> e2) {
    return (e1.ordinal() < e2.ordinal());
  }

  /** Returns a reasonable String representation of a sequence of things. Handles arrays, deep arrays and NULL.*/
  public static String toString(Object... o) {
    if (o == null) {
      return ("null");
    }
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < o.length; i++) {
      if (o[i] == null) {
        b.append("null");
        continue;
      }
      if (o[i].getClass().isArray()) {
        b.append("[");
        if (((Object[]) o[i]).length != 0) {
          for (Object obj : (Object[]) o[i]) {
            b.append(toString(obj)).append(", ");
          }
        }
        b.append("]");
      } else {
        b.append(o[i].toString());
      }
      if (i != o.length - 1) b.append(" ");
    }
    return (b.toString());
  }

//  /** Returns the size of the intersection*/
//  public static<T> int intersectionSize(Collection<T> c1, Collection<T> c2) {
//	  if(c1.size()>c2.size()) return(intersectionSize(c2,c1));
//	  int result=0;
//	  for(T t : c1) if(c2.contains(t)) result++;
//	  return(result);
//  }
}
