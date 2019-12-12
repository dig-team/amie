/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier;

import amie.data.KB;
import amie.data.SetU;
import amie.typing.heuristics.TypingHeuristic;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.util.List;

/**
 *
 * @author jlajus
 */
public class SeparationSTreeClassifier extends SeparationTreeClassifier {

    public SeparationSTreeClassifier(KB source, Int2IntMap cS, Int2ObjectMap<Int2IntMap> cIS) {
        super(source, cS, cIS);
    }

    public SeparationSTreeClassifier(KB source, File typeCountFile, File typeIntersectionCountFile) {
        super(source, typeCountFile, typeIntersectionCountFile);
    }

    public SeparationSTreeClassifier(KB source, Int2IntMap cS, Int2ObjectMap<Int2IntMap> cIS, boolean supportForTarget) {
        super(source, cS, cIS, supportForTarget);
    }

    public SeparationSTreeClassifier(KB source, File typeCountFile, File typeIntersectionCountFile, boolean supportForTarget) {
        super(source, typeCountFile, typeIntersectionCountFile, supportForTarget);
    }
    
    public void computeStatistics(List<int[]> query, int variable, int classSizeThreshold) {
        IntSet relevantClasses = index.keySet();
        int relation = (query.get(0)[0] == (variable)) ? query.get(0)[1] : KB.map(KB.unmap(query.get(0)[1]) + "-1");

        for (int class1 : relevantClasses) {
            int c1size = classSize.get(class1);
            IntSet c1phi = null;
            
            if (localdb != null) {
                c1phi = new IntOpenHashSet(localdb.relations.get(relation));
                c1phi.retainAll(localdb.classes.get(class1));
                if (c1phi.size() == c1size) {
                    continue;
                }
            }

            List<int[]> clause = TypingHeuristic.typeL(class1, variable);
            clause.addAll(query);
            IntSet targetClasses = (supportForTarget) ? relevantClasses : classIntersectionSize.get(class1).keySet();

            for (int class2 : targetClasses) {
                assert (clause.size() == query.size() + 1);
                if (class1 == class2) {
                    continue;
                }
                if (classSize.get(class2) < classSizeThreshold) {
                    // Ensure the symmetry of the output.
                    continue;
                }
                if (!classIntersectionSize.containsKey(class1) || !classIntersectionSize.get(class1).containsKey(class2)) {
                    continue;
                }

                int c1c2size = classIntersectionSize.get(class1).get(class2);

                if (c1c2size < classSizeThreshold) {
                    continue;
                } else if (c1size - c1c2size < classSizeThreshold) {
                    continue;
                } else {
                    Double s = (localdb == null) ? getStandardConfidenceWithThreshold(TypingHeuristic.typeL(class2, variable), clause, variable, -1, true) : 1.0 * SetU.countIntersection(c1phi, localdb.classes.get(class2)) / c1phi.size();
                    Double c1c2edge;
                    c1c2edge = Math.log((double) c1c2size / (c1size - c1c2size) * (1.0 - s) / s);
                    if (c1c2edge < 0) {
                        index.get(class1).separationScore = Math.min(index.get(class1).separationScore, c1c2edge);
                    } else {
                        if (index.containsKey(class2)) {
                            index.get(class2).separationScore = Math.min(index.get(class2).separationScore, -c1c2edge);
                        }
                        index.get(class1).separationScore = Math.min(index.get(class1).separationScore, -c1c2edge);
                    }
                }
            }
        }
    }
}
