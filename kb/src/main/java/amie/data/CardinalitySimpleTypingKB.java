/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author jlajus
 */
public class CardinalitySimpleTypingKB extends SimpleTypingKB {
    
    protected final Int2ObjectMap<Int2IntMap> relationsCard = new Int2ObjectOpenHashMap<>();
    
    @Override
    protected boolean add(int subject, int relation, int object) {
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
                int relationy = KB.map(relation.toString() + "-1");
                eS = relations.get(relationy);
                if (eS == null) {
                    relations.put(relationy, eS = new IntOpenHashSet());
                }
                Int2IntMap eS2 = relationsCard.get(relation);
                if (eS2 == null) {
                    relationsCard.put(relation, eS2 = new Int2IntOpenHashMap());
                }
                eS2.add(subject);
                eS2 = relationsCard.get(relationy);
                if (eS2 == null) {
                    relationsCard.put(relationy, eS2 = new Int2IntOpenHashMap());
                }
                eS2.add(object);
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }
    
    public void computeCardinalities() {
        for (Int2ObjectMap.Entry<Int2IntMap> entry : relationsCard.entrySet()) {
            Map<Integer, IntSet> t = new HashMap<>(entry.getValue().findMax());
            for (int e : entry.getValue()) {
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
                    add(KB.map(entry.getKey().toString() + 
                             "_" + Integer.toString(sortedKeys.get(i-1)) + "+"),
                        Schema.subClassRelationBS,
                        KB.map(entry.getKey().toString() + 
                            "_" + Integer.toString(sortedKeys.get(i)) + "+"));
                }
                classes.put(KB.map(entry.getKey().toString() + 
                        "_" + Integer.toString(sortedKeys.get(i)) + "+"), 
                        t.get(sortedKeys.get(i)));
            }
            add(KB.map(entry.getKey().toString() + 
                        "_" + Integer.toString(sortedKeys.get(sortedKeys.size() - 1)) + "+"),
                Schema.subClassRelationBS,
                Schema.topBS);
        }
        relationsCard.clear();
    }
}
