/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author jlajus
 */
public class SetU {
    
    public static <T> long countIntersection(Set<T> s1, Set<T> s2) {
        if (s1 == null || s2 == null) { return 0; }
        if (s1.size() > s2.size()) { return countIntersection(s2, s1); }
        long result = 0;
        for (T e1 : s1) {
            if (s2.contains(e1)) result++;
        }
        return result;
    }


    
    public static class addNotInIntIterator implements IntIterator {

        IntSet addTo;
        IntIterator toIterate;
        int next;
        
        public addNotInIntIterator(IntSet toIterate, IntSet addTo) {
            if (addTo == null || toIterate == null) {
                addTo = null;
                toIterate = null;
            } else {
                this.addTo = addTo;
                this.toIterate = toIterate.iterator();
            }
            next = 0;
        }
        
        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (toIterate == null) { return false; }
            while(toIterate.hasNext()) {
                if (!addTo.contains(next = toIterate.nextInt())) { 
                    addTo.add(next);
                    return true;
                }
            }
            next = 0;
            return false;
        }

        @Override
        public int nextInt() {
            int r = 0;
            if (next != 0 || hasNext()) { r = next; next = 0; }
            return r;
        }
    }
}
