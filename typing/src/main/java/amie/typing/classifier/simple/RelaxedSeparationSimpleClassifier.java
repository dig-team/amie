/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.SimpleTypingKB;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import javatools.datatypes.Pair;

/**
 *
 * @author jlajus
 */
public class RelaxedSeparationSimpleClassifier extends SeparationSimpleClassifier {

    public RelaxedSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        super(db, thresholds, output, outputLock);
        name = "relaxedCR";
    }

    public RelaxedSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, supportForTarget);
        name = "relaxedCR";
    }

    public RelaxedSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "relaxedCR";
    }

    public RelaxedSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "relaxedCR";
    }
    
    @Override
    protected Pair<Double, Double> classesScore(int c1, int c1c2, int c1phi, int c1c2phi) {
        double s = ((double) c1c2phi) / c1phi;
        double edge = Math.log((double) c1c2 / (c1 - c1c2) * (1.0 - s) / s);
        return new Pair<>(edge, -edge);
    }
}
