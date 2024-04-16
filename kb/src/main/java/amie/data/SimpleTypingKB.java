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


}
