/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.SimpleTypingKB;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

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

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "stdConf";
    }

    public B2SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "stdConf";
    }
    
    @Override
    protected boolean meetThreshold(double treeScore, double threshold) {
        return treeScore >= threshold;
    }

    @Override
    public void computeStatistics(ByteString relation, int classSizeThreshold) {
        for (SimpleTreeNode n : index.values()) {
            n.separationScore = ((double) n.support) / n.bodySize;
        }
    }
}
