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


/**
 *
 * @author jlajus
 */
public class B2SimpleClassifier extends SimpleClassifier {

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        super(db, thresholds, output, outputLock);
        name = "stdConf";
    }

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, supportForTarget);
        name = "stdConf";
    }

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "stdConf";
    }

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "stdConf";
    }
    
    @Override
    protected boolean meetThreshold(double treeScore, double threshold) {
        return treeScore >= threshold;
    }

    @Override
    public void computeStatistics(int relation, int classSizeThreshold) {
        for (SimpleTreeNode n : index.values()) {
            n.separationScore = ((double) n.support) / n.bodySize;
            n.thresholdI = -1;
        }
    }
}
