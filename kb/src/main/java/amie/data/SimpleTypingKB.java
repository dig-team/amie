/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import static amie.data.SetU.countIntersection;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 *
 * @author jlajus
 */
public class SimpleTypingKB extends KB {
    public final Int2ObjectMap<IntSet> relations = new Int2ObjectOpenHashMap<>();
    //protected final Int2ObjectMap<Int2ObjectMap<LazySet>> classIntersection = new Int2ObjectOpenHashMap<>();
    public final Int2ObjectMap<IntSet> classes = new Int2ObjectOpenHashMap<>();
    public final IntSet relationSet = new IntOpenHashSet();
    
    class LazySet {
        private volatile IntSet resource = null;
        private int c1, c2;
        
        public LazySet(int c1, int c2) {
            this.c1 = c1;
            this.c2 = c2;
        } 

        public IntSet getResource() {
            IntSet resource = this.resource;
            if (resource == null) {
                synchronized (this) {
                    resource = this.resource;
                    if (resource == null) {
                        resource = new IntOpenHashSet(classes.get(c1));
                        resource.retainAll(classes.get(c2));
                        this.resource = resource;
                    }
                }
            }
            return resource;
        }
    }
    
    @Override
    protected boolean add(int subject, int relation, int object) {
        if (relation == Schema.typeRelationBS) {
            //System.err.println(object);
            synchronized (classes) {
                IntSet eS = classes.get(object);
                if (eS == null) {
                    classes.put(object, eS = new IntOpenHashSet());
                }
                return eS.add(subject);
            }
        } else if (relation == Schema.subClassRelationBS) {
            return super.add(subject, relation, object);
        } else {
            //System.err.println(relation);
            synchronized (relations) {
                IntSet eS = relations.get(relation);
                if (eS == null) {
                    relations.put(relation, eS = new IntOpenHashSet());
                }
                eS.add(subject);
                int relationy = KB.map(KB.unmap(relation) + "-1");
                eS = relations.get(relationy);
                if (eS == null) {
                    relations.put(relationy, eS = new IntOpenHashSet());
                }
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }

    private IntSet lazyIntersectionSet(int c1, int c2) {
        if (c1 < c2) {
            return lazyIntersectionSet(c2, c1);
        }
        throw new UnsupportedOperationException();
        //return classIntersection.get(c1).get(c2).getResource();
    }
    
    public long countElements(int relation) {
        if (!relations.containsKey(relation)) { return 0; }
        return relations.get(relation).size();
    }
    
    public long countElements(int relation, int type) {
        if (!classes.containsKey(type)) { return 0; }
        return countIntersection(relations.get(relation), classes.get(type));
    }
    
    public long countElements(int relation, int type1, int type2) {
        return countIntersection(relations.get(relation), lazyIntersectionSet(type1, type2));
    }
    
    public double typingStdConf(int relation, int bodyType, int headType, int supportThreshold) {
        IntSet body = new IntOpenHashSet(relations.get(relation));
        body.retainAll(classes.get(bodyType));
        long bodySize = body.size();
        long support = countIntersection(body, classes.get(headType));
        if (support < supportThreshold || bodySize == 0) {
            return 0;
        }
        return ((double) support) / bodySize;
    }
    
    @Override
    public IntSet getRelationSet() {
//        for(int c1 : classes.keySet()) {
//            Int2ObjectMap<LazySet> c1I = new Int2ObjectOpenHashMap<>();
//            classIntersection.put(c1, c1I);
//            for(int c2 : classes.keySet()) {
//                if (c1.compareTo(c2) > 0) c1I.put(c2, new LazySet(c1, c2));
//            }
//        }
        return new IntOpenHashSet(relationSet);
    }
    
    @Override
    public IntSet getClassSet() {
        return new IntOpenHashSet(classes.keySet());
    }
}
