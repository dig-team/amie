/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data.tuple;

/**
 *
 * @author jlajus
 */
public class IntPair implements Comparable<IntPair> {
    /** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga). */
  
  /** Holds the first component */
  public int first;
  /** Holds the second component */  
  public int second;
  /** Returns the first */
  public int first() {
    return first;
  }
  /** Sets the first */
  public void setFirst(int first) {
    this.first=first;
  }
  /** Returns the second */
  public int second() {
    return second;
  }
  /** Sets the second */
  public void setSecond(int second) {
    this.second=second;
  }
  
  /** Constructs a Pair*/
  public IntPair(int first, int second) {
    super();
    this.first=first;
    this.second=second;
  }
  
  /** Constructs an empty pair */
  public IntPair(){
    super();
  }

  public int hashCode() {
    return(first^second);
  }
  
  @Override
  public boolean equals(Object obj) {   
    return obj instanceof IntPair && ((IntPair)obj).first == (first) && ((IntPair)obj).second == (second);
  }
  /** Returns "first/second"*/
  @Override
  public String toString() {
    return first+"/"+second;
  }
  
  @SuppressWarnings("unchecked")
  public int compareTo(IntPair o) {
    int firstCompared=Integer.compare(first, o.first);
    if(firstCompared!=0) return(firstCompared);
    return(Integer.compare(second, o.second));
  }

}
