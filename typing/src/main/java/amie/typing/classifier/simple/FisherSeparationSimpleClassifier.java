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
import org.apache.commons.math3.distribution.HypergeometricDistribution;

/**
 *
 * @author jlajus
 */
public class FisherSeparationSimpleClassifier extends SeparationSimpleClassifier {

    public FisherSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        super(db, thresholds, output, outputLock);
        name = "fisher";
    }

    public FisherSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, supportForTarget);
        name = "fisher";
    }

    public FisherSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "fisher";
    }

    public FisherSeparationSimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "fisher";
    }

    private static double phyper(int x, int NR, int NB, int n, boolean tail) {
        if (!tail) {
            int oldNB = NB;
            NB = NR;
            NR = oldNB;
            x = n - x - 1;
        }
        if (x < 0) {
            return 0.0;
        }
        if (x >= n) {
            return 1.0;
        }
        HypergeometricDistribution hg = new HypergeometricDistribution(NR+NB, NR, n);
        return hg.cumulativeProbability(x);
     }
    
    public static double fisherTest(int x, int m, int n, int k, boolean tail) {
        if (!tail) return phyper(x - 1 , m, n, k, tail);
        return phyper(x, m, n, k, tail);
    }
    
    public static double fisherTest(int x, int m, int n, int k) {
        return fisherTest(x, m, n, k, true);
    }
    
    @Override
    protected Pair<Double, Double> classesScore(int c1, int c1c2, int c1phi, int c1c2phi) {
        return new Pair<>(
                Math.log(fisherTest(c1c2phi, c1phi, c1 - c1phi, c1c2, false)),
                Math.log(fisherTest(c1c2phi, c1phi, c1 - c1phi, c1c2, true)));
    }
    
}
