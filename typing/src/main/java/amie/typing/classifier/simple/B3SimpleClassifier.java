/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.Schema;
import amie.data.SimpleTypingKB;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

/**
 *
 * @author jlajus
 */
public class B3SimpleClassifier extends SimpleClassifier {

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        super(db, thresholds, output, outputLock);
        name = "revConf";
    }

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, supportForTarget);
        name = "revConf";
    }

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "revConf";
    }

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Map<ByteString, IntHashMap<ByteString>> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "revConf";
    }

    @Override
    protected boolean meetThreshold(double treeScore, double threshold) {
        return treeScore >= threshold;
    }

    @Override
    public void computeStatistics(ByteString relation, int classSizeThreshold) {
        int bodySize = index.get(Schema.topBS).support;
        for (SimpleTreeNode n : index.values()) {
            n.separationScore = ((double) n.support) / bodySize;
        }
    }
    
    @Override
    /**
     * Compute the lowest classes in the taxonomy meeting the threshold.
     */
    public void computeClassification(ByteString relation) {
        for (SimpleTreeNode n : index.values()) {
            int i = 0;
            while(i < thresholds.length && meetThreshold(n.separationScore, thresholds[i])) { i++; }
            n.thresholdMask = i;
        }
        Queue<SimpleTreeNode> q = new LinkedList<>();
        q.add(index.get(Schema.topBS));
        int i, maxMask;
        while(!q.isEmpty()) {
            SimpleTreeNode n = q.poll();
            if (n.thresholdI > -1 || n.thresholdMask == 0) continue;
            maxMask = 0;
            for (SimpleTreeNode c : n.children) {
                maxMask = Math.max(maxMask, c.thresholdMask);
            }
            for (i = maxMask; i < n.thresholdMask; i++) {
                export(relation, thresholds[i], n.className);
            }
            n.thresholdI = n.thresholdMask;
            if (maxMask > 0) { q.addAll(n.children); }
        }
    }
    
}
