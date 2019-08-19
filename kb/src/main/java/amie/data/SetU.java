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
    
    public static class intersectionIterable<T> implements Iterable<T> {

        Set<T> s1, s2;
        
        public intersectionIterable(final Set<T> s1, final Set<T> s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
        
        @Override
        public Iterator<T> iterator() {
            return new intersectionIterator(s1, s2);
        }
        
        public static <T> Iterable<T> getInstance(final Set<T> s1, final Set<T> s2) {
            return new intersectionIterable(s1, s2);
        }
    }
    
    public static class intersectionIterator<T> implements Iterator<T> {

        final Set<T> big;
        Iterator<T> sit;
        T next;
        
        public intersectionIterator(final Set<T> s1, final Set<T> s2) {
            if (s1 == null || s2 == null) {
                big = null;
                sit = null;
            } else {
                if (s1.size() <= s2.size()) {
                    sit = s1.iterator();
                    big = s2;
                } else {
                    sit = s2.iterator();
                    big = s1;
                }
            }
            next = null;
        }
        
        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (sit == null) { return false; }
            while(sit.hasNext()) {
                if (big.contains(next = sit.next())) { return true; }
            }
            next = null;
            return false;
        }

        @Override
        public T next() {
            T r = null;
            if (next != null || hasNext()) { r = next; next = null; }
            return r;
        }
    }
    
    public static class intersectionIntIterator implements IntIterator {

        final IntSet big;
        IntIterator sit;
        int next;
        
        public intersectionIntIterator(final IntSet s1, final IntSet s2) {
            if (s1 == null || s2 == null) {
                big = null;
                sit = null;
            } else {
                if (s1.size() <= s2.size()) {
                    sit = s1.iterator();
                    big = s2;
                } else {
                    sit = s2.iterator();
                    big = s1;
                }
            }
            next = 0;
        }
        
        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (sit == null) { return false; }
            while(sit.hasNext()) {
                if (big.contains(next = sit.next())) { return true; }
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
    
    public static class addNotInIterator<T> implements Iterator<T> {

        Set<T> addTo;
        Iterator<T> toIterate;
        T next;
        
        public addNotInIterator(Set<T> toIterate, Set<T> addTo) {
            if (addTo == null || toIterate == null) {
                addTo = null;
                toIterate = null;
            } else {
                this.addTo = addTo;
                this.toIterate = toIterate.iterator();
            }
            next = null;
        }
        
        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (toIterate == null) { return false; }
            while(toIterate.hasNext()) {
                if (!addTo.contains(next = toIterate.next())) { 
                    addTo.add(next);
                    return true;
                }
            }
            next = null;
            return false;
        }

        @Override
        public T next() {
            T r = null;
            if (next != null || hasNext()) { r = next; next = null; }
            return r;
        }
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
