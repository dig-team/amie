//package amie.data.javatools.datatypes;
//
//import java.util.Arrays;
//import java.util.WeakHashMap;
//
//import amie.data.javatools.administrative.D;
//
///**
// * This class is part of the Java Tools (see
// * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
// * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
// * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
// *
// * This class represents strings with 1 byte per character. Thus, they use
// * roughly half as much space as ordinary strings -- but they also cannot store
// * all characters. ByteStrings are always unique. they can be compared with ==.
// *
// * @author Fabian M. Suchanek
// *
// */
//public class ByteString implements Comparable<ByteString>, CharSequence {
//
//  /** Holds all strings */
//  protected static WeakHashMap<ByteString, ByteString> values = new WeakHashMap<ByteString, ByteString>();
//
//  /** Holds the string */
//  public byte[] data;
//
//  /** Hash code */
//  protected int hashCode;
//
//  /** is interned */
//  public boolean isInterned = false;
//
//  /** Constructor*/
//  public static ByteString of(CharSequence s) {
//    ByteString newOne=new ByteString(s);
//    synchronized (values) {
//      ByteString canonic = values.get(newOne);
//      if (canonic != null) return (canonic);
//      values.put(newOne,newOne);
//      newOne.isInterned=true;
//      /* We need this flag, because if we go directly always by ==, then WeakHashMap will not be able to detect if the String is already there...*/
//    }
//    return (newOne);
//  }
//
//  /** Use subSequence()*/
//  protected ByteString(ByteString s, int start, int end) {
//    data = Arrays.copyOfRange(s.data, start, end);
//    hashCode = Arrays.hashCode(data);
//  }
//
//  /** Use of() */
//  protected ByteString(CharSequence s) {
//    data = new byte[s.length()];
//    for (int i = 0; i < s.length(); i++) {
//      data[i] = (byte) (s.charAt(i) - 128);
//    }
//    hashCode = Arrays.hashCode(data);
//  }
//
//  @Override
//  public char charAt(int arg0) {
//    return (char) (data[arg0] + 128);
//  }
//
//  @Override
//  public int length() {
//    return data.length;
//  }
//
//  @Override
//  public CharSequence subSequence(int arg0, int arg1) {
//    return new ByteString(this, arg0, arg1);
//  }
//
//  @Override
//  public int hashCode() {
//    return hashCode;
//  }
//
//  @Override
//  public boolean equals(Object obj) {
//    if(!(obj instanceof ByteString)) return(false);
//    ByteString other=(ByteString) obj;
//    if(this.isInterned && other.isInterned) return(this==other);
//    return(Arrays.equals(other.data, data));
//  }
//
//  @Override
//  public String toString() {
//    StringBuilder b = new StringBuilder();
//    for (int i = 0; i < length(); i++) {
//      b.append(charAt(i));
//    }
//    return b.toString();
//  }
//
//  public int compareTo(ByteString anotherString) {
//    int len1 = data.length;
//    int len2 = anotherString.data.length;
//    int lim = Math.min(len1, len2);
//    byte v1[] = data;
//    byte v2[] = anotherString.data;
//
//    int k = 0;
//    while (k < lim) {
//      char c1 = (char)v1[k];
//      char c2 = (char)v2[k];
//      if (c1 != c2) {
//        return c1 - c2;
//      }
//      k++;
//    }
//    return len1 - len2;
//  }
//
//
//  public static void main(String[] args) throws Exception {
//    D.p(new ByteString("Hallo du!"));
//    D.p(new ByteString("<wikicat_People_from_Liepāja>"));
//  }
//}
