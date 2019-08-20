/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.typing.evaluation;

import amie.data.KB;
import amie.data.Schema;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import javatools.datatypes.Pair;

/**
 *
 * @author jlajus
 */
public class ClassSizeEvaluator {
    protected KB taxo;
    protected Int2IntMap classSize;
    
    public Pair<Integer, Integer> evaluatePrecisionPair(IntSet answer, IntSet goldStandard) {
        int TP = 0;
        int FP = 0;
        for (int c : answer) {
            int subTP = 0;
            for (int gc : goldStandard) {
                if (gc == (c) || Schema.isTransitiveSuperType(taxo, gc, c)) {
                    subTP = classSize.get(c);
                    break;
                }
                if (Schema.isTransitiveSuperType(taxo, c, gc)) {
                    subTP += classSize.get(gc);
                }
            }
            TP += subTP;
            FP += classSize.get(c) - subTP;
        }
        return new Pair<>(TP, FP);
    }
    
    public Pair<Integer, Integer> evaluateRecallPair(IntSet answer, IntSet goldStandard) {
        return evaluatePrecisionPair(goldStandard, answer);
    }
    
    public double pairToPrecision(Pair<Integer, Integer> TPFP) {
        if (TPFP.first + TPFP.second == 0) return 1.;
        return (double) TPFP.first / (TPFP.first + TPFP.second);
    }
    
    public double evaluatePrecision(IntSet answer, IntSet goldStandard) {
        return pairToPrecision(evaluatePrecisionPair(answer, goldStandard));
    }
    
    public double evaluateRecall(IntSet answer, IntSet goldStandard) {
        return evaluatePrecision(goldStandard, answer);
    }
}
