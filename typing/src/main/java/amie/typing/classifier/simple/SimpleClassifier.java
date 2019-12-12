/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier.simple;

import amie.data.Schema;
import amie.data.SetU;
import amie.data.SimpleTypingKB;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;


/**
 *
 * @author jlajus
 */
public abstract class SimpleClassifier {
    
    public Int2ObjectMap<SimpleTreeNode> index = new Int2ObjectOpenHashMap<>();
    public boolean supportForTarget;
    
    public SimpleTypingKB db;
    public Int2ObjectMap<Int2IntMap> classIntersectionSize;
    public String name = "";
    protected double[] thresholds;
    
    protected Queue<SimpleClassifierOutput> bufferQueue;
    protected Queue<SimpleClassifierOutput> outputQueue;
    protected Lock outputLock;
    
    protected int supportThreshold;
    
    public SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock) {
        this.db = db;
        this.classIntersectionSize = null;
        this.supportForTarget = false;
        this.outputQueue = output;
        this.bufferQueue = new LinkedList<>();
        this.outputLock = outputLock;
        this.thresholds = thresholds;
    }
    
    public SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, boolean supportForTarget) {
        this(db, thresholds, output, outputLock);
        this.supportForTarget = supportForTarget;
    }
    
    public SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize) {
        this(db, thresholds, output, outputLock);
        this.classIntersectionSize = classIntersectionSize;
    }
    
    public SimpleClassifier(SimpleTypingKB db, double[] thresholds, Queue<SimpleClassifierOutput> output, Lock outputLock, Int2ObjectMap<Int2IntMap> classIntersectionSize,
            boolean supportForTarget) {
        this(db, thresholds, output, outputLock);
        this.classIntersectionSize = classIntersectionSize;
        this.supportForTarget = supportForTarget;
    }
    
    public class SimpleClassifierOutput {
        public String method;
        public double threshold;
        public int relation;
        public int resultClass;
        public int classSizeThreshold;
        public SimpleClassifierOutput(String method, double threshold, int relation, int resultClass, int classSizeThreshold) {
            this.method = method;
            this.threshold = threshold;
            this.relation = relation;
            this.resultClass = resultClass;
            this.classSizeThreshold = classSizeThreshold;
        }
    }
    
    protected class SimpleTreeNode {
    
        public SimpleTreeNode(int classNameP) {
            className = classNameP;
        }
        
        public SimpleTreeNode(int supportThreshold, int classSizeThreshold, int relation) {
            className = Schema.topBS;
            if ((bodySize = db.classes.get(className).size()) > classSizeThreshold
                   && (support = (int) SetU.countIntersection(db.classes.get(Schema.topBS), db.relations.get(relation))) > supportThreshold) {
                index.put(Schema.topBS, this);
            }
        }
    
        public int className;
        public Double separationScore = 0.0;
        public int thresholdI = 0;
        public int thresholdMask = 0;
        public Collection<SimpleTreeNode> children = new LinkedList<>();
        
        // As we compute this anyway in generate, store it as well.
        public int bodySize;
        public int support;
    
        public void generate(int supportThreshold, int classSizeThreshold, int relation) {
            int bs, s;
            for (int subClass : Schema.getSubTypes(db, className)) {
                if (!db.classes.containsKey(subClass)) { continue; }
                SimpleTreeNode stc = index.get(subClass);
                if (stc != null) {
                    children.add(stc);
                } else if ((bs = db.classes.get(subClass).size()) > classSizeThreshold
                        && (s = (int) SetU.countIntersection(db.classes.get(subClass), db.relations.get(relation))) > supportThreshold) {
                    stc = new SimpleTreeNode(subClass);
                    stc.bodySize = bs;
                    stc.support = s;
                    index.put(subClass, stc);
                    children.add(stc);
                    stc.generate(supportThreshold, classSizeThreshold, relation);
                }
            }
        }
        
        public void propagateMask(int mask) {
            //System.err.println(className.toString());
            for (SimpleTreeNode c : children) {
                c.thresholdMask = Math.max(c.thresholdMask, mask);
                c.propagateMask(mask);
            }
        }
        
        public void resetMask() {
           for (SimpleTreeNode c : children) {
                c.thresholdMask = 0;
                c.resetMask();
            } 
        }
    }
    
    protected abstract boolean meetThreshold(double treeScore, double threshold);
    
    public void classify(int relation, int classSizeThreshold, 
            int supportThreshold) throws IOException {
        index.clear();
        this.supportThreshold = supportThreshold;
        SimpleTreeNode root = new SimpleTreeNode(supportThreshold, classSizeThreshold, relation);
        if (index.containsKey(Schema.topBS)) {
            root.generate(supportThreshold, classSizeThreshold, relation);
            //System.err.println(index.size());
            if (index.size() > 1) {
                computeStatistics(relation, classSizeThreshold);
                //printTree();
                computeClassification(relation, classSizeThreshold);
                flush();
            }
        }
    }
    
    protected void export(int relation, double threshold, int className, int classSizeThreshold) {
        this.bufferQueue.add(new SimpleClassifierOutput(name + ((supportForTarget)? "-sft" : ""), threshold, relation, className, classSizeThreshold));
    }
    
    protected void flush() {
        this.outputLock.lock();
        try {
            outputQueue.addAll(bufferQueue);
        } finally {
            this.outputLock.unlock();
        }
    }
    
    /**
     * Standard top-bottom threshold comparison
     * Thresholds must be ordered from more laxist to stricter according to meetThreshold method.
     * @param relation
     */
    public void computeClassification(int relation, int classSizeThreshold) {
        Queue<SimpleTreeNode> q = new LinkedList<>();
        q.add(index.get(Schema.topBS));
        int i;
        while(!q.isEmpty()) {
            SimpleTreeNode n = q.poll();
            if (n.thresholdI > -1) continue;
            for(i = n.thresholdMask; i < thresholds.length; i++) {
                //System.err.println(n.className.toString() + "\n" + Double.toString(n.separationScore) + "\t" + Double.toString(thresholds[i]) + "\t" + Integer.toString(i));
                if(!meetThreshold(n.separationScore, thresholds[i])) break;
                export(relation, thresholds[i], n.className, classSizeThreshold);
            }
            n.thresholdI = i;
            n.propagateMask(i);
            if (i < thresholds.length) {
                q.addAll(n.children);
            }
        }
    }
    
    public abstract void computeStatistics(int relation, int classSizeThreshold);
}
