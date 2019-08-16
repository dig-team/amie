/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.classifier;

import amie.data.KB;
import amie.data.Schema;
import amie.data.SetU;
import amie.data.SimpleTypingKB;
import amie.typing.heuristics.TypingHeuristic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;

/**
 *
 * @author jlajus
 */
public class SeparationTreeClassifier extends SeparationClassifier {
    
    public Int2ObjectMap<SeparationTreeNode> index = new Int2ObjectOpenHashMap<>();
    public boolean supportForTarget;
    
    public SimpleTypingKB localdb = null;

    public SeparationTreeClassifier(KB source, Int2IntMap cS, Int2ObjectMap<Int2IntMap> cIS) {
        super(source, cS, cIS);
        supportForTarget = false;
        if (db instanceof SimpleTypingKB) {
            localdb = (SimpleTypingKB) db;
        }
    }

    public SeparationTreeClassifier(KB source, File typeCountFile, File typeIntersectionCountFile) {
        super(source, typeCountFile, typeIntersectionCountFile);
        supportForTarget = false;
        if (db instanceof SimpleTypingKB) {
            localdb = (SimpleTypingKB) db;
        }
    }
    
    public SeparationTreeClassifier(KB source, Int2IntMap cS, Int2ObjectMap<Int2IntMap> cIS, boolean supportForTarget) {
        super(source, cS, cIS);
        this.supportForTarget = supportForTarget;
        if (db instanceof SimpleTypingKB) {
            localdb = (SimpleTypingKB) db;
        }
    }

    public SeparationTreeClassifier(KB source, File typeCountFile, File typeIntersectionCountFile, boolean supportForTarget) {
        super(source, typeCountFile, typeIntersectionCountFile);
        this.supportForTarget = supportForTarget;
        if (db instanceof SimpleTypingKB) {
            localdb = (SimpleTypingKB) db;
        }
    }
    
    public void classify(List<ByteString[]> query, ByteString variable, int classSizeThreshold, int supportThreshold, double[] thresholds) throws IOException {
        if (getStandardConfidenceWithThreshold(TypingHeuristic.typeL(Schema.topBS, variable), query, variable, supportThreshold, true) != 0) {
            SeparationTreeNode root = new SeparationTreeNode(Schema.topBS);
            index.put(Schema.topBS, root);
            root.generate(supportThreshold, classSizeThreshold, query, variable);
            if (index.size() < 2) {
                return;
            }
            computeStatistics(query, variable, classSizeThreshold);
            //printTree();
            createFiles(query, variable, thresholds);
            computeClassification(thresholds);
            close();
        }
    }

    private ArrayList<BufferedWriter> writers = new ArrayList<>();
    
    private void createFiles(List<ByteString[]> query, ByteString variable, double[] thresholds) throws IOException {
        if (query.size() > 1) throw new UnsupportedOperationException("Not supported yet.");
        ByteString[] singleton = query.get(0);
        String fnb = singleton[1].toString().substring(1, singleton[1].toString().length()-1) + ((singleton[2].equals(variable)) ? "-1" : "") + "_";
        // Hack for sexism query
        if (!KB.isVariable(singleton[2])) { fnb = singleton[2].toString().substring(1, singleton[2].toString().length()-1) + "_"; }
        for (int i = 0; i < thresholds.length; i++) {
            writers.add(new BufferedWriter(new FileWriter(fnb + Double.toString(Math.exp(-thresholds[i])))));
        }
    }
    
    protected class SeparationTreeNode {
    
        public SeparationTreeNode(ByteString classNameP) {
            className = classNameP;
        }
    
        public ByteString className;
        public Double separationScore = 0.0;
        public int thresholdI = -1;
        public int thresholdMask = 0;
        public Collection<SeparationTreeNode> children = new LinkedList<>();
    
        public void generate(int supportThreshold, int classSizeThreshold, List<ByteString[]> query, ByteString variable) {
            for (ByteString subClass : Schema.getSubTypes(db, className)) {
                SeparationTreeNode stc = index.get(subClass);
                if (stc != null) {
                    children.add(stc);
                } else if (classSize.get(subClass) > classSizeThreshold
                        && getStandardConfidenceWithThreshold(TypingHeuristic.typeL(subClass, variable), query, variable, supportThreshold, true) != 0) {
                    stc = new SeparationTreeNode(subClass);
                    index.put(subClass, stc);
                    children.add(stc);
                    stc.generate(supportThreshold, classSizeThreshold, query, variable);
                }
            }
        }
        
        public void propagateMask(int mask) {
            //System.err.println(className.toString());
            for (SeparationTreeNode c : children) {
                c.thresholdMask = Math.max(c.thresholdMask, mask);
                c.propagateMask(mask);
            }
        }
        
        public void resetMask() {
           for (SeparationTreeNode c : children) {
                c.thresholdMask = 0;
                c.resetMask();
            } 
        }
    }
    
    /**
     * Prints class into files
     * Class of node n should be printed in file i if n.thresholdI > -1 and i \in [n.thresholdMask + 1; n.thresholdI]
     */
    private void export(int i, ByteString className) throws IOException {
        writers.get(i).write(className.toString() + "\n");
    }
    
    private void close() throws IOException {
        for (BufferedWriter w : writers) { w.close(); }
    }
    
    /**
     * 
     * @param thresholds log-thresholds (negative) ordered increasingly (more relax to stricter)
     */
    public void computeClassification(double[] thresholds) throws IOException {
        Queue<SeparationTreeNode> q = new LinkedList<>();
        q.add(index.get(Schema.topBS));
        int i;
        while(!q.isEmpty()) {
            SeparationTreeNode n = q.poll();
            //if (n.className.equals(Schema.topBS)) {
            //    System.err.println("TOP sc: " + Double.toString(n.separationScore) + "\t" + Integer.toString(index.size()));
            //}
            if (n.thresholdI > -1) continue;
            for(i = n.thresholdMask; i < thresholds.length; i++) {
                if(n.separationScore < thresholds[i]) break;
                export(i, n.className);
            }
            n.thresholdI = i;
            n.propagateMask(i);
            if (i < thresholds.length) {
                q.addAll(n.children);
            }
        }
    }
    
    public void printTree() {
        Deque<SeparationTreeNode> q = new LinkedList<>();
        q.add(index.get(Schema.topBS));
        String str = "";
        int i;
        while(!q.isEmpty()) {
            str = "";
            SeparationTreeNode n = q.pollLast();
            if (n.thresholdMask > 80) System.exit(2);
            for (i = 0; i < n.thresholdMask; i++) {
                str += " ";
            }
            str += "|" + n.className.toString() + ": " + Double.toString(n.separationScore) + "\n";
            System.err.println(str);
            for (SeparationTreeNode c : n.children) {
                c.thresholdMask = n.thresholdMask + 1;
            }
            //n.propagateMask(n.thresholdMask + 1);
            q.addAll(n.children);
        }
        index.get(Schema.topBS).resetMask();
    }
    
    public void computeStatistics(List<ByteString[]> query, ByteString variable, int classSizeThreshold) {
        IntSet relevantClasses = index.keySet();
        ByteString relation = (query.get(0)[0].equals(variable)) ? query.get(0)[1] : KB.map(query.get(0)[1].toString() + "-1");

        for (ByteString class1 : relevantClasses) {
            int c1size = classSize.get(class1);
            IntSet c1phi = null;
            
            if (localdb != null) {
                c1phi = new IntOpenHashSet(localdb.relations.get(relation));
                c1phi.retainAll(localdb.classes.get(class1));
                if (c1phi.size() == c1size) {
                    continue;
                }
            }

            List<ByteString[]> clause = TypingHeuristic.typeL(class1, variable);
            clause.addAll(query);
            IntSet targetClasses = (supportForTarget) ? relevantClasses : classIntersectionSize.get(class1);

            for (ByteString class2 : targetClasses) {
                assert (clause.size() == query.size() + 1);
                if (class1 == class2) {
                    continue;
                }
                if (classSize.get(class2) < classSizeThreshold) {
                    // Ensure the symmetry of the output.
                    continue;
                }
                if (!classIntersectionSize.containsKey(class1) || !classIntersectionSize.get(class1).contains(class2)) {
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
                    } else if (index.containsKey(class2)) {
                        index.get(class2).separationScore = Math.min(index.get(class2).separationScore, -c1c2edge);
                    }
                }
            }
        }
    }
}
