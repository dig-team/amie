/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.Schema;
import amie.data.SimpleTypingKB;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;


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

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize) {
        super(db, thresholds, output, outputLock, classIntersectionSize);
        name = "revConf";
    }

    public B3SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize, boolean supportForTarget) {
        super(db, thresholds, output, outputLock, classIntersectionSize, supportForTarget);
        name = "revConf";
    }

    @Override
    protected boolean meetThreshold(double treeScore, double threshold) {
        return treeScore >= threshold;
    }

    @Override
    public void computeStatistics(int relation, int classSizeThreshold) {
        int bodySize = index.get(Schema.topBS).support;
        for (SimpleTreeNode n : index.values()) {
            n.separationScore = ((double) n.support) / bodySize;
            n.thresholdI = -1;
        }
    }
    
    @Override
    /**
     * Compute the lowest classes in the taxonomy meeting the threshold.
     */
    public void computeClassification(int relation, int classSizeThreshold) {
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
                /**
                 * Should work as revConf can only decrease going down in the taxonomy
                 */
                maxMask = Math.max(maxMask, c.thresholdMask);
            }
            for (i = maxMask; i < n.thresholdMask; i++) {
                export(relation, thresholds[i], n.className, classSizeThreshold);
            }
            n.thresholdI = n.thresholdMask;
            if (maxMask > 0) { q.addAll(n.children); }
        }
    }
    
}
