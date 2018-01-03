/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.CardinalitySimpleTypingKB;
import amie.data.SimpleTypingKB;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import javatools.datatypes.Pair;

/**
 *
 * @author jlajus
 */
public abstract class SeparationSimpleClassifier extends SimpleClassifier {
    
    public SeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        super(db, thresholds, output, outputLock);
    }

    public SeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, supportForTarget);
    }

    public SeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
    }

    public SeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
    }
    
    @Override
    protected boolean meetThreshold(double treeScore, double threshold) {
        return treeScore >= -Math.abs(Math.log(threshold));
    }
    
    private int getIntersectionSize(ByteString class1, ByteString class2) {
        if (classIntersectionSize != null) {
            if (!classIntersectionSize.containsKey(class1) || !classIntersectionSize.get(class1).contains(class2)) {
                return 0;
            }
            return classIntersectionSize.get(class1).get(class2);
        }
        return (int) SimpleTypingKB.countIntersection(db.classes.get(class1), db.classes.get(class2));
    }
    
    private int cs(ByteString class1) { return db.classes.get(class1).size(); }
    
    protected abstract Pair<Double, Double> classesScore(int c1, int c1c2, int c1phi, int c1c2phi);
    
    @Override
    public void computeStatistics(ByteString relation, int classSizeThreshold) {
        Set<ByteString> relevantClasses = index.keySet();

        for (ByteString class1 : relevantClasses) {
            if (db instanceof CardinalitySimpleTypingKB) {
                String[] s = class1.toString().split("_");
                if (s.length == 2 && s[0].equals(relation.toString())) {
                    index.get(class1).separationScore = Double.NEGATIVE_INFINITY;
                    continue;
                }
            }  
            int c1size = cs(class1);
            Set<ByteString> c1phi = new HashSet<>(db.relations.get(relation));
            c1phi.retainAll(db.classes.get(class1));
            if (c1phi.size() == c1size) {
                index.get(class1).thresholdI = -1;
                continue;
            }
            double conf = ((double) index.get(class1).support) / index.get(class1).bodySize;
           
            Set<ByteString> targetClasses = (supportForTarget || classIntersectionSize == null) ? relevantClasses : classIntersectionSize.get(class1);
            if (targetClasses == null) {
                continue;
            }

            for (ByteString class2 : targetClasses) {
                if (class1 == class2) {
                    continue;
                }
                if (db instanceof CardinalitySimpleTypingKB) {
                    String[] s = class2.toString().split("_");
                    if (s.length == 2 && s[0].equals(relation.toString())) { 
                        continue; 
                    }
                }
                
                int c1c2size = getIntersectionSize(class1, class2);
                /**
                 * TODO: what happens for subclasses ? 
                 * what happens if a class is always skipped ?
                 * separationScore = 0 and is always true if support > st.
                 */
                if (classSizeThreshold >= 0) {
                    if (cs(class2) < classSizeThreshold) {
                        continue;
                    }
                    if (c1c2size < classSizeThreshold) {
                        continue;
                    } else if (c1size - c1c2size < classSizeThreshold) {
                        continue;
                    }
                }
                if (true) {
                    int c1c2phisize = (int) SimpleTypingKB.countIntersection(c1phi, db.classes.get(class2));
                    /**
                     * If classSizeThreshold is relative we don't consider 
                     * intersections nor differences such that:
                     * * Support less than threshold and,
                     * * Expected support less than threshold.
                     */
                    /*
                    if (classSizeThreshold < 0 && ( //relative
                               (c1c2size == 0)
                            || (c1c2size == c1size)
                            || (c1c2phisize < -classSizeThreshold && conf*c1c2size < -classSizeThreshold)
                            // Condition for intersection
                            || (c1phi.size() - c1c2phisize < -classSizeThreshold && conf*(c1size - c1c2size) < -classSizeThreshold)
                            // Condition for difference
                            )) {
                        continue;
                    }
                    */
                    
                    if (classSizeThreshold < 0 && ( //relative
                               (c1c2size == 0)
                            || (c1c2size == c1size)
                            || (c1c2phisize < -classSizeThreshold && (c1phi.size() - c1c2phisize) * c1c2size / (c1size - c1c2size) < -classSizeThreshold)
                            // Condition for intersection
                            || (c1phi.size() - c1c2phisize < -classSizeThreshold && c1c2phisize * (c1size - c1c2size) / c1c2size < -classSizeThreshold)
                            // Condition for difference
                            )) {
                        continue;
                    }
                    
                    
                    Pair<Double, Double> s = classesScore(c1size, c1c2size, c1phi.size(), c1c2phisize);
                    //System.err.println(class1.toString() + "\t" + class2.toString() + "\t" + Double.toString(s.first) + "\t" + Double.toString(s.second));
                    index.get(class1).separationScore = Math.min(index.get(class1).separationScore, s.first);
                    index.get(class1).thresholdI = -1;
                    if (index.containsKey(class2)) {
                        index.get(class2).separationScore = Math.min(index.get(class2).separationScore, s.second);
                        index.get(class2).thresholdI = -1;
                    }
                }
            }
        }
    }
}
