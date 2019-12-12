package amie.rules;

import static amie.data.KB.isVariable;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.List;

/**
 * Compares two rules. A rule may not have two equivalent body atoms. 
 * 
 * @author galarrag
 *
 */
public class QueryEquivalenceChecker2 {


    /** TRUE if two rules are equal. A rule may not have two equivalent body atoms. */
    public static boolean areEquivalent(List<int[]> r1, List<int[]> r2) {
        Int2IntMap map1to2 = new Int2IntOpenHashMap();
        if (!unify(r1.get(0), r2.get(0), map1to2)) return (false);
        return (bodiesEqual(r1.subList(1, r1.size()), r2.subList(1, r2.size()), map1to2));
    }

    /** Atoms must have same number of components */
    public static boolean unify(int[] atom1, int[] atom2, Int2IntMap map1to2) {
        for (int i = 0; i < atom1.length; i++) {
            if (isVariable(atom1[i])) {
                if (!isVariable(atom2[i])) return (false);
                int x1 = map1to2.getOrDefault(atom1[i], 0);
                if (x1 == 0) {
                    map1to2.put(atom1[i], atom2[i]);
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

    /** TRUE if two bodies are equal. Bodies must have same length. A rule may not have two equivalent body atoms. */
    public static boolean bodiesEqual(List<int[]> body1, List<int[]> body2, Int2IntMap map1to2) {
        if (body1.isEmpty()) return (true);
        for (int i = 0; i < body2.size(); i++) {
            if (body2.get(i) == null) continue;
            Int2IntMap oldMap = new Int2IntOpenHashMap(map1to2);
            if (unify(body1.get(0), body2.get(i), map1to2)) {
                int[] removedAtom = body2.get(i);
                body2.set(i, null);
                if (bodiesEqual(body1.subList(1, body1.size()), body2, map1to2)) {
                    body2.set(i, removedAtom);
                    return (true);
                }
                body2.set(i, removedAtom);
            }
            map1to2 = oldMap;
        }
        return (false);
    }
}
