/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;

/**
 *
 * @author jlajus
 */
public class KBIteratorU {
    
    // Because the Closeable interface throws IOException.
    public static interface CloseableNoThrow extends Closeable {
        @Override
        public void close();
    }
    
    public static class addNotInIfExistsIterator extends SetU.addNotInIterator<ByteString> implements Iterator<ByteString>, CloseableNoThrow {
        
        KB kb;
        KB.Instantiator insty;

        public addNotInIfExistsIterator(KB kb, KB.Instantiator insty, Set<ByteString> toIterate, Set<ByteString> addTo) {
            super(toIterate, addTo);
            this.kb = kb;
            this.insty = insty;
        }

        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (toIterate == null) { return false; }
            while(toIterate.hasNext()) {
                if (!addTo.contains(next = toIterate.next()) && kb.existsBS1(insty.instantiate(next))) { 
                    addTo.add(next);
                    return true;
                }
            }
            next = null;
            return false;
        }

        @Override
        public void close() {
            insty.close();
        }
    }
    
    public static class recursiveSelectForOneVarIterator extends addNotInIfExistsIterator implements Iterator<ByteString>, CloseableNoThrow {
        
        Iterator<ByteString> subIterator;
        ByteString variable;
        
        public recursiveSelectForOneVarIterator(KB kb, KB.Instantiator insty, ByteString variable, Set<ByteString> toIterate, Set<ByteString> addTo) {
            super(kb, insty, toIterate, addTo);
            this.variable = variable;
        }
        
        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (toIterate == null) { return false; }
            while (subIterator.hasNext() || toIterate.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.next();
                    return true;
                } else { // if (toIterate.hasNext()) {
                    if (subIterator instanceof CloseableNoThrow) { 
                        ((CloseableNoThrow) subIterator).close();
                    }
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty.instantiate(toIterate.next()));
                }
            }
            next = null;
            return false;
        }
    }
    
    public static class recursiveSelectForTwoVarIterator implements Iterator<ByteString>, CloseableNoThrow {
        
        Iterator<Map.Entry<ByteString, IntHashMap<ByteString>>> it1;
        Iterator<ByteString> it2;
        KB kb;
        KB.Instantiator insty1, insty2;
        Iterator<ByteString> subIterator;
        ByteString variable, next;
        Set<ByteString> addTo;
        
        public recursiveSelectForTwoVarIterator(KB kb, KB.Instantiator insty1, KB.Instantiator insty2, ByteString variable, Map<ByteString, IntHashMap<ByteString>> toIterate, Set<ByteString> addTo) {
            this.kb = kb;
            this.insty1 = insty1;
            this.insty2 = insty2;
            this.variable = variable;
            this.it1 = (toIterate == null) ? null : toIterate.entrySet().iterator();
            this.it2 = Collections.emptyIterator();
            this.addTo = addTo;
        }
        
        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (it1 == null) { return false; }
            while (subIterator.hasNext() || it2.hasNext() || it1.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.next();
                    return true;
                } else if (it2.hasNext()) {
                    if (subIterator instanceof CloseableNoThrow) { // Is it necessary ?
                        ((CloseableNoThrow) subIterator).close();
                    }
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty2.instantiate(it2.next()));
                } else {
                    Map.Entry<ByteString, IntHashMap<ByteString>> e1 = it1.next();
                    insty1.instantiate(e1.getKey());
                    it2 = e1.getValue().iterator();
                }
            }
            next = null;
            return false;
        }

        @Override
        public ByteString next() {
            ByteString r = null;
            if (next != null || hasNext()) { r = next; next = null; }
            return r;
        }

        @Override
        public void close() {
            insty1.close();
            insty2.close();
        }
    }
    
    public static class recursiveSelectForThreeVarIterator implements Iterator<ByteString>, CloseableNoThrow {
        
        Iterator<Map.Entry<ByteString, Map<ByteString, IntHashMap<ByteString>>>> it1;
        Iterator<Map.Entry<ByteString, IntHashMap<ByteString>>> it2;
        Iterator<ByteString> it3;
        KB kb;
        KB.Instantiator insty1, insty2, insty3;
        Iterator<ByteString> subIterator;
        ByteString variable, next;
        Set<ByteString> addTo;
        
        public recursiveSelectForThreeVarIterator(KB kb, KB.Instantiator insty1, KB.Instantiator insty2, KB.Instantiator insty3, ByteString variable, Map<ByteString, Map<ByteString, IntHashMap<ByteString>>> toIterate, Set<ByteString> addTo) {
            this.kb = kb;
            this.insty1 = insty1;
            this.insty2 = insty2;
            this.insty3 = insty3;
            this.variable = variable;
            this.it1 = (toIterate == null) ? null : toIterate.entrySet().iterator();
            this.it2 = Collections.emptyIterator();
            this.it3 = Collections.emptyIterator();
            this.addTo = addTo;
        }
        
        @Override
        public boolean hasNext() {
            if (next != null) { return true; }
            if (it1 == null) { return false; }
            while (subIterator.hasNext() || it3.hasNext() || it2.hasNext() || it1.hasNext()) {
                if (subIterator.hasNext()) {
                    next = subIterator.next();
                    return true;
                } else if (it3.hasNext()) {
                    if (subIterator instanceof CloseableNoThrow) { // Is it necessary ?
                        ((CloseableNoThrow) subIterator).close();
                    }
                    subIterator = kb.selectDistinctIterator(addTo, variable, insty3.instantiate(it3.next()));
                } else if (it2.hasNext()) {
                    Map.Entry<ByteString, IntHashMap<ByteString>> e2 = it2.next();
                    insty2.instantiate(e2.getKey());
                    it3 = e2.getValue().iterator();
                } else {
                    Map.Entry<ByteString, Map<ByteString, IntHashMap<ByteString>>> e1 = it1.next();
                    insty1.instantiate(e1.getKey());
                    it2 = e1.getValue().entrySet().iterator();
                }
            }
            next = null;
            return false;
        }

        @Override
        public ByteString next() {
            ByteString r = null;
            if (next != null || hasNext()) { r = next; next = null; }
            return r;
        }

        @Override
        public void close() {
            insty1.close();
            insty2.close();
            insty3.close();
        }
    }
}