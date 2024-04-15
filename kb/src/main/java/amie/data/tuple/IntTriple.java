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
public class IntTriple extends IntPair {
    /** Holds the second component */
  public int third;

  /** Returns the second */
  public int third() {
    return third;
  }
  
  /** Constructs a Triple*/
  public IntTriple(int first, int second, int third) {
    super(first, second);
    this.third = third;
  }

  public int hashCode() {
    return (super.hashCode() ^ third);
  }

  public boolean equals(Object obj) {
    if (obj instanceof IntTriple) {
        IntTriple o = ((IntTriple) obj);
        return (o.first() == first) && (o.second() == second) && (o.third() == third);
    }
    return false;
  }

  /** Returns "first/second"*/
  public String toString() {
    return first + "/" + second + "/" +third;
  }
}
