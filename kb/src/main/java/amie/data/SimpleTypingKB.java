/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import static amie.data.KB.compress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class SimpleTypingKB extends KB {
    public final Map<ByteString, Set<ByteString>> relations = new HashMap<>();
    //protected final Map<ByteString, Map<ByteString, LazySet>> classIntersection = new HashMap<>();
    public final Map<ByteString, Set<ByteString>> classes = new HashMap<>();
    public final Set<ByteString> relationSet = new HashSet<>();
    
    class LazySet {
        private volatile Set<ByteString> resource = null;
        private ByteString c1, c2;
        
        public LazySet(ByteString c1, ByteString c2) {
            this.c1 = c1;
            this.c2 = c2;
        } 

        public Set<ByteString> getResource() {
            Set<ByteString> resource = this.resource;
            if (resource == null) {
                synchronized (this) {
                    resource = this.resource;
                    if (resource == null) {
                        resource = new LinkedHashSet<>(classes.get(c1));
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
                Set<ByteString> eS = classes.get(object);
                if (eS == null) {
                    classes.put(object, eS = new LinkedHashSet<>());
                }
                return eS.add(subject);
            }
        } else if (relation.equals(Schema.subClassRelationBS)) {
            return super.add(subject, relation, object);
        } else {
            //System.err.println(relation);
            synchronized (relations) {
                Set<ByteString> eS = relations.get(relation);
                if (eS == null) {
                    relations.put(relation, eS = new LinkedHashSet<>());
                }
                eS.add(subject);
                ByteString relationy = ByteString.of(relation.toString() + "-1");
                eS = relations.get(relationy);
                if (eS == null) {
                    relations.put(relationy, eS = new LinkedHashSet<>());
                }
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }

    private Set<ByteString> lazyIntersectionSet(ByteString c1, ByteString c2) {
        if (c1.compareTo(c2) < 0) {
            return lazyIntersectionSet(c2, c1);
        }
        throw new UnsupportedOperationException();
        //return classIntersection.get(c1).get(c2).getResource();
    }
    
    public static long countIntersection(Set<ByteString> s1, Set<ByteString> s2) {
        if (s1 == null || s2 == null) { return 0; }
        if (s1.size() > s2.size()) { return countIntersection(s2, s1); }
        long result = 0;
        for (ByteString e1 : s1) {
            if (s2.contains(e1)) result++;
        }
        return result;
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
        Set<ByteString> body = new LinkedHashSet<>(relations.get(relation));
        body.retainAll(classes.get(bodyType));
        long bodySize = body.size();
        long support = countIntersection(body, classes.get(headType));
        if (support < supportThreshold || bodySize == 0) {
            return 0;
        }
        return ((double) support) / bodySize;
    }
    
    @Override
    public Set<ByteString> getRelationSet() {
//        for(ByteString c1 : classes.keySet()) {
//            Map<ByteString, LazySet> c1I = new HashMap<>();
//            classIntersection.put(c1, c1I);
//            for(ByteString c2 : classes.keySet()) {
//                if (c1.compareTo(c2) > 0) c1I.put(c2, new LazySet(c1, c2));
//            }
//        }
        return new HashSet<>(relationSet);
    }
    
    @Override
    public Set<ByteString> getClassSet() {
        return new HashSet<>(classes.keySet());
    }
}
