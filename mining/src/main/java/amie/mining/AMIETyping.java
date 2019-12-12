package amie.mining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import javatools.datatypes.MultiMap;
import amie.data.KB;
import amie.mining.AMIE.RDFMinerJob;
import amie.mining.assistant.experimental.TypingMiningAssistant;
import amie.rules.Metric;
import amie.rules.Rule;

public class AMIETyping extends AMIE {
	
	protected TypingMiningAssistant assistant;

	public AMIETyping(TypingMiningAssistant assistant, int minInitialSupport,
			double threshold, Metric metric, int nThreads) {
		super(assistant, minInitialSupport, threshold, metric, nThreads);
		// TODO Auto-generated constructor stub
	}
	
	
    /**
     * This class implements the AMIE algorithm in a single thread.
     * Specialize rules only once.
     *
     * @author jlajus
     */
    protected class TypingRDFMinerJob extends RDFMinerJob implements Runnable {

        /**
         * 
         * @param seedsPool
         * @param outputSet
         * @param resultsLock Lock associated to the output buffer were mined rules are added
         * @param resultsCondition Condition variable associated to the results lock
         * @param sharedCounter Reference to a shared counter that keeps track of the number of threads that are running
         * in the system.
         * @param indexedOutputSet
         */
        public TypingRDFMinerJob(AMIEQueue seedsPool,
				List<Rule> outputSet, Lock resultsLock,
				Condition resultsCondition,
				MultiMap<Integer, Rule> indexedOutputSet) {
			super(seedsPool, outputSet, resultsLock, resultsCondition, indexedOutputSet);
		}

        @Override
        public void run() {
            while (true) {
            	Rule currentRule = null;
				try {
					currentRule = queryPool.dequeue();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

                if (currentRule == null) {
                	this.queryPool.decrementMaxThreads();
                	break;
                } else {

                    //long timeStamp11 = System.currentTimeMillis();
                    double threshold = getCountThreshold(currentRule);
                    List<Rule> temporalOutput = new ArrayList<Rule>();
                    List<Rule> temporalOutputDanglingEdges = new ArrayList<Rule>();

                    // Application of the mining operators
                    assistant.getClosingAtoms(currentRule, threshold, temporalOutput);
                    assistant.getDanglingAtoms(currentRule, threshold, temporalOutputDanglingEdges);
                    assistant.getInstantiatedAtoms(currentRule, threshold, temporalOutputDanglingEdges, temporalOutput);
                    assistant.getTypeSpecializedAtoms(currentRule, threshold, temporalOutput);
                        
                    //long timeStamp21 = System.currentTimeMillis();
                    //this._specializationTime += (timeStamp21 - timeStamp11);

                    temporalOutput.add(currentRule);
                    temporalOutput.addAll(temporalOutputDanglingEdges);
                    
                    for (Rule rule : temporalOutput) {
                        // Check if the rule meets the language bias and confidence thresholds and
                        // decide whether to output it.
                        boolean outputRule = false;
                        if (assistant.shouldBeOutput(rule)) {
                            //long timeStamp1 = System.currentTimeMillis();
                            boolean ruleSatisfiesConfidenceBounds
                                    = assistant.calculateConfidenceBoundsAndApproximations(rule);
                            //this._approximationTime += (System.currentTimeMillis() - timeStamp1);
                            if (ruleSatisfiesConfidenceBounds) {
                                this.resultsLock.lock();
                                assistant.setAdditionalParents(rule, indexedOutputSet);
                                this.resultsLock.unlock();
                                // Calculate the metrics
                                assistant.calculateConfidenceMetrics(rule);
                                // Check the confidence threshold and skyline technique.
                                outputRule = assistant.testConfidenceThresholds(rule);
                            } else {
                                outputRule = false;
                            }
                            //long timeStamp2 = System.currentTimeMillis();
                            //this._scoringTime += (timeStamp2 - timeStamp1);
                        }
                        // Output the rule
                        if (outputRule) {
                            this.resultsLock.lock();
                            //long timeStamp1 = System.currentTimeMillis();
                            Set<Rule> outputQueries = indexedOutputSet.get(rule.alternativeParentHashCode());
                            if (outputQueries != null) {
                                if (!outputQueries.contains(rule)) {
                                    this.outputSet.add(rule);
                                    outputQueries.add(rule);
                                }
                            } else {
                                this.outputSet.add(rule);
                                this.indexedOutputSet.put(rule.alternativeParentHashCode(), rule);
                            }
                            //long timeStamp2 = System.currentTimeMillis();
                            //this._queueingAndDuplicateElimination += (timeStamp2 - timeStamp1);
                            this.resultsCondition.signal();
                            this.resultsLock.unlock();
                        }
                    }   
                }
            }
        }
    }
}
