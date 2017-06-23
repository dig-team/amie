/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.evaluation;

import amie.data.KB;
import amie.data.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javatools.datatypes.ByteString;
import javatools.datatypes.Pair;

/**
 *
 * @author jlajus
 */
public class ImpliedFactsEvaluator {

    private KB db;
    public Set<ByteString> queried;
    public BlockingQueue<Pair<ByteString, Double>> queryQ;
    public BlockingQueue<Pair<Pair<ByteString, Double>, ImpliedFacts>> resultQ;

    public static final ByteString gsRelation = ByteString.of("<inGoldStandardOf>");

    public static ByteString resultWithThresholdRelation(double threshold) {
        return ByteString.of("<inResult_" + Double.toString(threshold) + ">");
    }

    public static ByteString rwtr(double t) {
        return resultWithThresholdRelation(t);
    }

    public ImpliedFactsEvaluator(KB db, Map<ByteString, Set<ByteString>> goldStandard,
            Map<ByteString, Map<Double, Set<ByteString>>> results) {
        this.db = db;
        queried = new HashSet<>(results.keySet());
        queried.retainAll(goldStandard.keySet());
        for (ByteString q : queried) {
            for (ByteString gsResult : goldStandard.get(q)) {
                for (ByteString e : Schema.getAllEntitiesForType(db, gsResult)) {
                    db.add(e, gsRelation, q);
                }
            }
            for (Double t : results.get(q).keySet()) {
                queryQ.add(new Pair<>(q, t));
                for (ByteString rtClass : results.get(q).get(t)) {
                    for (ByteString e : Schema.getAllEntitiesForType(db, rtClass)) {
                        db.add(e, rwtr(t), q);
                    }
                }
            }
        }
    }
    
    /**
     * TP: True Positive
     * FP: False Positive
     * FN: False Negative
     * NF: "New facts", i.e number of entities deduced.
     */
    public class ImpliedFacts {
        public long TP, FP, FN, NF;
        public ImpliedFacts(long tp, long fp, long fn, long nf) { TP=tp; FP=fp; FN=fn; NF=nf; }
    }
    
    public ImpliedFacts computeImpliedFacts(ByteString query, double threshold) {
        final ByteString x = ByteString.of("?x");
        List<ByteString[]> gsSizeQ = KB.triples(KB.triple(x, gsRelation, query));
        List<ByteString[]> rtSizeQ = KB.triples(KB.triple(x, rwtr(threshold), query));
        long gsSize = db.countDistinct(x, gsSizeQ);
        long rtSize = db.countDistinct(x, rtSizeQ);
        gsSizeQ.addAll(rtSizeQ);
        long tp = db.countDistinct(x, gsSizeQ);
        String[] q = query.toString().split("-1");
        if (q.length == 1) {
            gsSizeQ.add(KB.triple(x, query, ByteString.of("?y")));
        } else {
            gsSizeQ.add(KB.triple(ByteString.of("?y"), ByteString.of(q[0]), x));
        }
        long oldFacts = db.countDistinct(x, gsSizeQ);
        return new ImpliedFacts(tp, rtSize - tp, gsSize - tp, tp - oldFacts);
    }
    
    public class ImpliedFactsMTEvaluator extends Thread {
        @Override
        public void run() {
            Pair<ByteString, Double> qP;
            while(true) {
                try {
                    qP = queryQ.take();
                    if (qP.first.equals(ByteString.of("STOP"))) break;
                    resultQ.put(new Pair<>(qP, computeImpliedFacts(qP.first, qP.second)));
                } catch (InterruptedException ex) {
                    Logger.getLogger(ImpliedFactsEvaluator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void computeImpliedFactsMT(int nThreads) throws InterruptedException {
        int nProcessors = Runtime.getRuntime().availableProcessors();
        nThreads = Math.max(1, Math.min(nThreads, nProcessors - 1));
        
        List<Thread> threadList = new ArrayList<>(nThreads);
        for (int i = 0; i < nThreads; i++) {
            queryQ.add(new Pair<>(ByteString.of("STOP"), 0.0));
            threadList.add(new ImpliedFactsMTEvaluator());
        }
        
        for (Thread thread : threadList) {
            thread.start();
        }
        
        for (Thread thread : threadList) {
            thread.join();
        }
    }
}
