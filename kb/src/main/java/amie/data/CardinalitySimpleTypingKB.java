/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import static amie.data.U.increase;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collections;
import java.util.Comparator;


/**
 *
 * @author jlajus
 */
public class CardinalitySimpleTypingKB extends SimpleTypingKB {
    
    protected final Int2ObjectMap<Int2IntMap> relationsCard = new Int2ObjectOpenHashMap<>();
    
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
                Int2IntMap eS2 = relationsCard.get(relation);
                if (eS2 == null) {
                    relationsCard.put(relation, eS2 = new Int2IntOpenHashMap());
                }
                increase(eS2, subject);
                eS2 = relationsCard.get(relationy);
                if (eS2 == null) {
                    relationsCard.put(relationy, eS2 = new Int2IntOpenHashMap());
                }
                increase(eS2, object);
                relationSet.add(relation);
                return eS.add(object);
            }
        }
    }
    
    public void computeCardinalities() {
        for (Int2ObjectMap.Entry<Int2IntMap> entry : relationsCard.int2ObjectEntrySet()) {
            Int2ObjectMap<IntSet> t = new Int2ObjectOpenHashMap<>();
            for (int e : entry.getValue().keySet()) {
                int i = entry.getValue().get(e);
                IntSet rs = t.get(i);
                if (rs == null) {
                    t.put(i, rs = new IntOpenHashSet());
                }
                rs.add(e);
            }
            IntList sortedKeys = new IntArrayList(t.keySet());
            Collections.sort(sortedKeys, Comparator.reverseOrder());
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i > 0) {
                    t.get(sortedKeys.getInt(i)).addAll(t.get(sortedKeys.getInt(i-1)));
                    add(KB.map(KB.unmap(entry.getIntKey()) + 
                             "_" + Integer.toString(sortedKeys.getInt(i-1)) + "+"),
                        Schema.subClassRelationBS,
                        KB.map(KB.unmap(entry.getIntKey()) + 
                            "_" + Integer.toString(sortedKeys.getInt(i)) + "+"));
                }
                classes.put(KB.map(KB.unmap(entry.getIntKey()) + 
                        "_" + Integer.toString(sortedKeys.getInt(i)) + "+"), 
                        t.get(sortedKeys.getInt(i)));
            }
            add(KB.map(KB.unmap(entry.getIntKey()) + 
                        "_" + Integer.toString(sortedKeys.getInt(sortedKeys.size() - 1)) + "+"),
                Schema.subClassRelationBS,
                Schema.topBS);
        }
        relationsCard.clear();
    }
}
