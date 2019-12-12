package amie.rules;

import static amie.data.KB.isVariable;
import static amie.data.KB.triple;
import amie.rules.Rule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javatools.datatypes.ByteString;

/**
 * Compares two rules. A rule may not have two equivalent body atoms. 
 * 
 * @author galarrag
 *
 */
public class QueryEquivalenceChecker2 {


    /** TRUE if two rules are equal. A rule may not have two equivalent body atoms. */
    public static boolean areEquivalent(List<ByteString[]> r1, List<ByteString[]> r2) {
        Map<ByteString, ByteString> map1to2 = new HashMap<>();
        if (!unify(r1.get(0), r2.get(0), map1to2)) return (false);
        return (bodiesEqual(r1.subList(1, r1.size()), r2.subList(1, r2.size()), map1to2));
    }

    /** Atoms must have same number of components */
    public static boolean unify(ByteString[] atom1, ByteString[] atom2, Map<ByteString, ByteString> map1to2) {
        for (int i = 0; i < atom1.length; i++) {
            if (isVariable(atom1[i])) {
                if (!isVariable(atom2[i])) return (false);
                ByteString x1 = map1to2.get(atom1[i]);
                if (x1 == null) {
                    map1to2.put(atom1[i], atom2[i]);
                    continue;
                }
                if (x1.equals(atom2[i])) continue;
                return (false);
            }
            if (atom1[i].equals(atom2[i])) continue;
            return (false);
        }
        return (true);
    }

    /** TRUE if two bodies are equal. Bodies must have same length. A rule may not have two equivalent body atoms. */
    public static boolean bodiesEqual(List<ByteString[]> body1, List<ByteString[]> body2, Map<ByteString, ByteString> map1to2) {
        if (body1.isEmpty()) return (true);
        for (int i = 0; i < body2.size(); i++) {
            if (body2.get(i) == null) continue;
            Map<ByteString, ByteString> oldMap = new HashMap<>(map1to2);
            if (unify(body1.get(0), body2.get(i), map1to2)) {
                ByteString[] removedAtom = body2.get(i);
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
