/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;

/**
 * Iterators to simulate SELECT DISTINCT operation as a generator.
 * 
 * Closure semantic:
 *
 * Every iterator that is given an Instantiator at construction-time MUST:
 *  * close it when the iterator has no more element.
 *  * close it and close every sub-iterator (if applicable) 
 *    when the close() method is explicitly called.
 * A closed iterator can no more be iterated upon.
 * @author jlajus
 */
public class KBIteratorU {
    
    // Because the Closeable interface throws IOException.
    public static interface CloseableNoThrow extends Closeable {
        @Override
        public void close();
    }
    
    public static class addNotInIfExistsIterator extends SetU.addNotInIntIterator implements IntIterator, CloseableNoThrow {
        
        KB kb;
        KB.Instantiator insty;

        public addNotInIfExistsIterator(KB kb, KB.Instantiator insty, IntSet toIterate, IntSet addTo) {
            super(toIterate, addTo);
            this.kb = kb;
            this.insty = insty;
        }

        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (toIterate == null) { return false; }
            while(toIterate.hasNext()) {
                if (!addTo.contains(next = toIterate.nextInt()) && kb.existsBS1(insty.instantiate(next))) { 
                    addTo.add(next);
                    return true;
                }
            }
            next = 0;
            insty.close();
            return false;
        }

        @Override
        public void close() {
            toIterate = null;
            insty.close();
        }
    }
    
        
    public static class recursiveSelectForOneVarIterator extends addNotInIfExistsIterator implements IntIterator, CloseableNoThrow {
        
        IntIterator subIterator;
        int variable;
        
        public recursiveSelectForOneVarIterator(KB kb, KB.Instantiator insty, int variable, IntSet toIterate, IntSet addTo) {
            super(kb, insty, toIterate, addTo);
            this.variable = variable;
            this.subIterator = IntIterators.EMPTY_ITERATOR;
        }
        
        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (toIterate == null) { return false; }
            while (subIterator.hasNext() || toIterate.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.nextInt();
                    return true;
                } else { // if (toIterate.hasNext()) {
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty.instantiate(toIterate.nextInt()));
                }
            }
            next = 0;
            insty.close();
            return false;
        }
        
        @Override
        public void close() {
            if (subIterator instanceof CloseableNoThrow) { 
                ((CloseableNoThrow) subIterator).close();
            }
            insty.close();
            toIterate = null;
        }
    }

    public static class recursiveSelectForTwoVarIterator implements IntIterator, CloseableNoThrow {
        
        Iterator<Int2ObjectMap.Entry<IntSet>> it1;
        IntIterator it2;
        KB kb;
        KB.Instantiator insty1, insty2;
        IntIterator subIterator;
        int variable, next;
        IntSet addTo;
        
        public recursiveSelectForTwoVarIterator(KB kb, KB.Instantiator insty1, KB.Instantiator insty2, int variable, Int2ObjectMap<IntSet> toIterate, IntSet addTo) {
            this.kb = kb;
            this.insty1 = insty1;
            this.insty2 = insty2;
            this.variable = variable;
            this.it1 = (toIterate == null) ? null : toIterate.int2ObjectEntrySet().iterator();
            this.it2 = IntIterators.EMPTY_ITERATOR;
            this.subIterator = IntIterators.EMPTY_ITERATOR;
            this.addTo = addTo;
        }
        
        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (it1 == null) { return false; }
            while (subIterator.hasNext() || it2.hasNext() || it1.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.nextInt();
                    return true;
                } else if (it2.hasNext()) {
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty2.instantiate(it2.nextInt()));
                } else {
                    Int2ObjectMap.Entry<IntSet> e1 = it1.next();
                    insty1.instantiate(e1.getIntKey());
                    it2 = e1.getValue().iterator();
                }
            }
            next = 0;
            insty1.close();
            insty2.close();
            return false;
        }

        @Override
        public int nextInt() {
            int r = 0;
            if (next != 0 || hasNext()) { r = next; next = 0; }
            return r;
        }

        @Override
        public void close() {
            if (subIterator instanceof CloseableNoThrow) { 
                ((CloseableNoThrow) subIterator).close();
            }
            insty1.close();
            insty2.close();
            it1 = null;
        }
    }
    
    public static class recursiveSelectForThreeVarIterator implements IntIterator, CloseableNoThrow {
        
        Iterator<Int2ObjectMap.Entry<Int2ObjectMap<IntSet>>> it1;
        Iterator<Int2ObjectMap.Entry<IntSet>> it2;
        IntIterator it3;
        KB kb;
        KB.Instantiator insty1, insty2, insty3;
        IntIterator subIterator;
        int variable, next;
        IntSet addTo;
        
        public recursiveSelectForThreeVarIterator(KB kb, KB.Instantiator insty1, KB.Instantiator insty2, KB.Instantiator insty3, int variable, Int2ObjectMap<Int2ObjectMap<IntSet>> toIterate, IntSet addTo) {
            this.kb = kb;
            this.insty1 = insty1;
            this.insty2 = insty2;
            this.insty3 = insty3;
            this.variable = variable;
            this.it1 = (toIterate == null) ? null : toIterate.int2ObjectEntrySet().iterator();
            this.it2 = Collections.emptyIterator();
            this.it3 = IntIterators.EMPTY_ITERATOR;
            this.subIterator = IntIterators.EMPTY_ITERATOR;
            this.addTo = addTo;
        }
        
        @Override
        public boolean hasNext() {
            if (next != 0) { return true; }
            if (it1 == null) { return false; }
            while (subIterator.hasNext() || it3.hasNext() || it2.hasNext() || it1.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.nextInt();
                    return true;
                } else if (it3.hasNext()) {
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty3.instantiate(it3.nextInt()));
                } else if (it2.hasNext()) {
                    Int2ObjectMap.Entry<IntSet> e2 = it2.next();
                    insty2.instantiate(e2.getIntKey());
                    it3 = e2.getValue().iterator();
                } else {
                    Int2ObjectMap.Entry<Int2ObjectMap<IntSet>> e1 = it1.next();
                    insty1.instantiate(e1.getIntKey());
                    it2 = e1.getValue().int2ObjectEntrySet().iterator();
                }
            }
            next = 0;
            insty1.close();
            insty2.close();
            insty3.close();
            return false;
        }

        @Override
        public int nextInt() {
            int r = 0;
            if (next != 0 || hasNext()) { r = next; next = 0; }
            return r;
        }

        @Override
        public void close() {
            if (subIterator instanceof CloseableNoThrow) { // Is it necessary ?
                ((CloseableNoThrow) subIterator).close();
            }
            insty1.close();
            insty2.close();
            insty3.close();
            it1 = null;
        }
    }
}
