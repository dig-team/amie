/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

/**
 *
 * @author jlajus
 */
public class CardinalitySimpleTypingKB extends SimpleTypingKB {
    
    protected final Map<ByteString, IntHashMap<ByteString>> relationsCard = new HashMap<>();
    
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
                ByteString relationy = ByteString.of(relation.toString() + "-1");
                eS = relations.get(relationy);
                if (eS == null) {
                    relations.put(relationy, eS = new IntOpenHashSet());
                }
                IntHashMap<ByteString> eS2 = relationsCard.get(relation);
                if (eS2 == null) {
                    relationsCard.put(relation, eS2 = new IntHashMap<>());
                }
                eS2.add(subject);
                eS2 = relationsCard.get(relationy);
                if (eS2 == null) {
                    relationsCard.put(relationy, eS2 = new IntHashMap<>());
                }
                eS2.add(object);
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }
    
    public void computeCardinalities() {
        for (Map.Entry<ByteString, IntHashMap<ByteString>> entry : relationsCard.entrySet()) {
            Map<Integer, IntSet> t = new HashMap<>(entry.getValue().findMax());
            for (ByteString e : entry.getValue()) {
                Integer i = entry.getValue().get(e);
                IntSet rs = t.get(i);
                if (rs == null) {
                    t.put(i, rs = new IntOpenHashSet());
                }
                rs.add(e);
            }
            ArrayList<Integer> sortedKeys = new ArrayList<>(t.keySet());
            Collections.sort(sortedKeys, Comparator.reverseOrder());
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i > 0) {
                    t.get(sortedKeys.get(i)).addAll(t.get(sortedKeys.get(i-1)));
                    add(ByteString.of(entry.getKey().toString() + 
                             "_" + Integer.toString(sortedKeys.get(i-1)) + "+"),
                        Schema.subClassRelationBS,
                        ByteString.of(entry.getKey().toString() + 
                            "_" + Integer.toString(sortedKeys.get(i)) + "+"));
                }
                classes.put(ByteString.of(entry.getKey().toString() + 
                        "_" + Integer.toString(sortedKeys.get(i)) + "+"), 
                        t.get(sortedKeys.get(i)));
            }
            add(ByteString.of(entry.getKey().toString() + 
                        "_" + Integer.toString(sortedKeys.get(sortedKeys.size() - 1)) + "+"),
                Schema.subClassRelationBS,
                Schema.topBS);
        }
        relationsCard.clear();
    }
}
