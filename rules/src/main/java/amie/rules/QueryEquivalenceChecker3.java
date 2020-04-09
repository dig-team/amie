package amie.rules;

import static amie.data.KB.isVariable;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;

/**
 * Compares two rules. A rule may not have two equivalent body atoms. 
 * 
 * @author galarrag
 *
 */
public class QueryEquivalenceChecker3 {


    /** TRUE if two rules are equal. A rule may not have two equivalent body atoms. */
    public static boolean areEquivalent(List<int[]> r1, List<int[]> r2) {
        Int2IntMap map1to2 = new Int2IntOpenHashMap();
        IntSet mapped2 = new IntOpenHashSet();
        if (!unify(r1.get(0), r2.get(0), map1to2, mapped2)) return (false);
        return (bodiesEqual(r1, r2, 1, map1to2, mapped2, 1L));
    }

    /** Atoms must have same number of components */
    public static boolean unify(int[] atom1, int[] atom2, Int2IntMap map1to2, IntSet mapped2) {
        for (int i = 0; i < atom1.length; i++) {
            if (isVariable(atom1[i])) {
                if (!isVariable(atom2[i])) return (false);
                int x1 = map1to2.getOrDefault(atom1[i], 0);
                if (x1 == 0 && !mapped2.contains(atom2[i])) {
                    map1to2.put(atom1[i], atom2[i]);
                    mapped2.add(atom2[i]);
                    continue;
                }
                if (x1 == atom2[i]) continue;
                return (false);
            }
            if (atom1[i] == atom2[i]) continue;
            return (false);
        }
        return (true);
    }

    public static long setBit(int pos, long mask) {
        return (mask | (1 << pos));
    }
    
    public static boolean getBit(int pos, long mask) {
        return ((mask & (1 << pos)) != 0L);
    }
    
    /** TRUE if two bodies are equal. Bodies must have same length. A rule may not have two equivalent body atoms. */
    public static boolean bodiesEqual(List<int[]> body1, List<int[]> body2, int index, Int2IntMap map1to2, IntSet mapped2, long mask2) {
        if (body1.size() <= index) return (true);
        for (int i = 0; i < body2.size(); i++) {
            if (getBit(i, mask2)) continue;
            Int2IntMap oldMap = new Int2IntOpenHashMap(map1to2);
            IntSet oldMapped2 = new IntOpenHashSet(mapped2);
            long oldMask2 = mask2;
            if (unify(body1.get(index), body2.get(i), map1to2, mapped2)) {
                mask2 = setBit(i, mask2);
                if (bodiesEqual(body1, body2, index+1, map1to2, mapped2, mask2)) {
                    return (true);
                }
            }
            map1to2 = oldMap;
            mapped2 = oldMapped2;
            mask2 = oldMask2;
        }
        return (false);
    }
}
