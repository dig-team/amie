/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import static amie.data.SetU.countIntersection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javatools.datatypes.ByteString;

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
        private ByteString c1, c2;
        
        public LazySet(ByteString c1, ByteString c2) {
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
    protected boolean add(ByteString subject, ByteString relation, ByteString object) {
        if (relation.equals(Schema.typeRelationBS)) {
            //System.err.println(object);
            synchronized (classes) {
                IntSet eS = classes.get(object);
                if (eS == null) {
                    classes.put(object, eS = new IntOpenHashSet());
                }
                return eS.add(subject);
            }
        } else if (relation.equals(Schema.subClassRelationBS)) {
            return super.add(subject, relation, object);
        } else {
            //System.err.println(relation);
            synchronized (relations) {
                IntSet eS = relations.get(relation);
                if (eS == null) {
                    relations.put(relation, eS = new IntOpenHashSet());
                }
                eS.add(subject);
                ByteString relationy = KB.map(relation.toString() + "-1");
                eS = relations.get(relationy);
                if (eS == null) {
                    relations.put(relationy, eS = new IntOpenHashSet());
                }
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }

    private IntSet lazyIntersectionSet(ByteString c1, ByteString c2) {
        if (c1.compareTo(c2) < 0) {
            return lazyIntersectionSet(c2, c1);
        }
        throw new UnsupportedOperationException();
        //return classIntersection.get(c1).get(c2).getResource();
    }
    
    public long countElements(ByteString relation) {
        if (!relations.containsKey(relation)) { return 0; }
        return relations.get(relation).size();
    }
    
    public long countElements(ByteString relation, ByteString type) {
        if (!classes.containsKey(type)) { return 0; }
        return countIntersection(relations.get(relation), classes.get(type));
    }
    
    public long countElements(ByteString relation, ByteString type1, ByteString type2) {
        return countIntersection(relations.get(relation), lazyIntersectionSet(type1, type2));
    }
    
    public double typingStdConf(ByteString relation, ByteString bodyType, ByteString headType, int supportThreshold) {
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
//        for(ByteString c1 : classes.keySet()) {
//            Int2ObjectMap<LazySet> c1I = new Int2ObjectOpenHashMap<>();
//            classIntersection.put(c1, c1I);
//            for(ByteString c2 : classes.keySet()) {
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
