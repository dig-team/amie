package amie.data.javatools.datatypes;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).

This class can be used to keep track of a non-functional, directed relation
*/
public class MultiMap<A, B> implements Iterable<Entry<A,B>> {
  private Map<A, Set<B>> relation;
  
  public MultiMap() {
    relation = new HashMap<A, Set<B>>();
  }

  public void put(A a, B b) {
    Set<B> bs = relation.get(a);
    
    if (bs == null) {
      bs = new HashSet<B>();
      relation.put(a, bs);
    }
    
    bs.add(b);
  }
  
  public Set<B> get(A a) {
    return relation.get(a);
  }

  @Override
  public Iterator<Entry<A,B>> iterator() {
    return new MultiMapIterator();
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    
    for (Entry<A, B> e: this) {
      sb.append(e.getKey() + " - " + e.getValue() + ", ");
    }
    
    return sb.toString();
  }
  
  private class MultiMapIterator implements Iterator<Entry<A,B>> {

    private Iterator<A> aIterator;
    private A currentA;
    
    private Iterator<B> bIterator;
        
    public MultiMapIterator() {
      aIterator = relation.keySet().iterator();
    }
    
    @Override
    public boolean hasNext() {
      if (aIterator.hasNext()) {
        if (bIterator == null) {
          currentA = aIterator.next();
          bIterator = relation.get(currentA).iterator();
        }

        return true;
      } else if (bIterator != null) {
        return bIterator.hasNext();
      } else {
        return false;
      }
    }

    @Override
    public Entry<A,B> next() {      
      // there is at least 1 element in bIterator
      B currentB = bIterator.next();
      
      if (!bIterator.hasNext()) {
        bIterator = null;
      }
      
      return new AbstractMap.SimpleEntry<A,B>(currentA, currentB);
    }

    @Override
    public void remove() {
      // not supported
    }
  }
}
