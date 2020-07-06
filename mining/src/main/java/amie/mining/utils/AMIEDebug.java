/**
 * @author lgalarra
 * @date Aug 8, 2012 AMIE Version 0.1
 */
package amie.mining.utils;

import amie.mining.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import amie.data.KB;
import amie.data.MultilingualKB;
import amie.data.Schema;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.RelationSignatureDefaultMiningAssistant;
import amie.mining.assistant.experimental.DefaultMiningAssistantWithOrder;
import amie.mining.assistant.experimental.LazyIteratorMiningAssistant;
import amie.mining.assistant.experimental.LazyMiningAssistant;
import amie.mining.assistant.variableorder.AppearanceOrder;
import amie.mining.assistant.variableorder.FunctionalOrder;
import amie.mining.assistant.variableorder.InverseOrder;
import amie.mining.assistant.variableorder.VariableOrder;
import amie.rules.Metric;
import amie.rules.QueryEquivalenceChecker3;
import amie.rules.Rule;
import amie.rules.eval.TSVRuleDiff;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import java.util.Collections;
import java.util.HashSet;
import javatools.administrative.Announce;

import javatools.datatypes.MultiMap;
import javatools.datatypes.Pair;
import javatools.parsers.NumberFormatter;

/**
 * Main class that implements the AMIE algorithm for rule mining on ontologies.
 * The ontology must be provided as a list of TSV files where each line has the
 * format SUBJECT&lt;TAB&gt;RELATION&lt;TAB&gt;OBJECT.
 *
 * @author lgalarra
 *
 */
public class AMIEDebug {

    /**
     * Default standard confidence threshold
     */
    protected static final double DEFAULT_STD_CONFIDENCE = 0.0;

    /**
     * Default PCA confidence threshold
     */
    protected static final double DEFAULT_PCA_CONFIDENCE = 0.1;

    /**
     * Default Head coverage threshold
     *
     */
    protected static final double DEFAULT_HEAD_COVERAGE = 0.01;

    /**
     * The default minimum size for a relation to be used as a head relation
     */
    protected static final int DEFAULT_INITIAL_SUPPORT = 100;

    /**
     * The default support threshold
     */
    protected static final int DEFAULT_SUPPORT = 100;

    /**
     * It implements all the operators defined for the mining process: ADD-EDGE,
     * INSTANTIATION, SPECIALIZATION and CLOSE-CIRCLE
     */
    protected MiningAssistant assistant;

    /**
     * Support threshold for relations.
     */
    protected double minInitialSupport;

    /**
     * Threshold for refinements. It can hold either an absolute support number
     * or a head coverage threshold.
     */
    private double minSignificanceThreshold;

    /**
     * Metric used to prune the mining tree
     */
    protected Metric pruningMetric;

    /**
     * Preferred number of threads
     */
    protected int nThreads;

    /**
     * If true, print the rules as they are discovered.
     */
    protected boolean realTime;

    /**
     * List of target head relations.
     */
    protected IntCollection seeds;
    
    /**
     * Column headers
     */
    public static final List<String> headers = Arrays.asList("Rule", "Head Coverage", "Std Confidence",
            "PCA Confidence", "Positive Examples", "Body size", "PCA Body size",
            "Functional variable", "Std. Lower Bound", "PCA Lower Bound", "PCA Conf estimation");

    /**
     *
     * @param assistant An object that implements the logic of the mining
     * operators.
     * @param minInitialSupport If head coverage is defined as pruning metric,
     * it is the minimum size for a relation to be considered in the mining.
     * @param threshold The minimum support threshold: it can be either a head
     * coverage ratio threshold or an absolute number depending on the 'metric'
     * argument.
     * @param metric Head coverage or support.
     */
    public AMIEDebug(MiningAssistant assistant, int minInitialSupport, double threshold, Metric metric, int nThreads) {
        this.assistant = assistant;
        this.minInitialSupport = minInitialSupport;
        this.minSignificanceThreshold = threshold;
        this.pruningMetric = metric;
        this.nThreads = nThreads;
        this.realTime = true;
        this.seeds = null;
    }

    public MiningAssistant getAssistant() {
        return assistant;
    }

    public boolean isVerbose() {
        // TODO Auto-generated method stub
        return assistant.isVerbose();
    }

    public boolean isRealTime() {
        return realTime;
    }

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }

    public IntCollection getSeeds() {
    	return seeds;
    }
    
    public void setSeeds(IntCollection seeds) {
    	this.seeds = seeds;
    }

    public double getMinSignificanceThreshold() {
        return minSignificanceThreshold;
    }

    public void setMinSignificanceThreshold(double minSignificanceThreshold) {
        this.minSignificanceThreshold = minSignificanceThreshold;
    }

    public Metric getPruningMetric() {
        return pruningMetric;
    }

    public void setPruningMetric(Metric pruningMetric) {
        this.pruningMetric = pruningMetric;
    }

    public double getMinInitialSupport() {
        return minInitialSupport;
    }

    public void setMinInitialSupport(double minInitialSupport) {
        this.minInitialSupport = minInitialSupport;
    }

    public int getnThreads() {
        return nThreads;
    }

    public void setnThreads(int nThreads) {
        this.nThreads = nThreads;
    }

    /**
     * The key method which returns a set of rules mined from the KB based on
     * the AMIE object's configuration.
     *
     * @return
     * @throws Exception
     */
    public List<Rule> mine() throws Exception {
        List<Rule> result = new ArrayList<>();
        MultiMap<Integer, Rule> indexedResult = new MultiMap<>();
        RuleConsumer consumerObj = null;
        Thread consumerThread = null;
        Lock resultsLock = new ReentrantLock();
        Condition resultsCondVar = resultsLock.newCondition();
        Collection<Rule> seedRules = new ArrayList<>();

        // Queue initialization
        if (seeds == null || seeds.isEmpty()) {
            seedRules = assistant.getInitialAtoms(minInitialSupport);
        } else {
            seedRules = assistant.getInitialAtomsFromSeeds(seeds, minInitialSupport);
        }

        AMIEQueueDebug queue = new AMIEQueueDebug(seedRules, nThreads);

        if (realTime) {
            consumerObj = new RuleConsumer(result, resultsLock, resultsCondVar);
            consumerThread = new Thread(consumerObj);
            consumerThread.start();
        }

        System.out.println("Using " + nThreads + " threads");
        //Create as many threads as available cores
        ArrayList<Thread> currentJobs = new ArrayList<>();
        ArrayList<RDFMinerJob> jobObjects = new ArrayList<>();
        for (int i = 0; i < nThreads; ++i) {
            RDFMinerJob jobObject = new RDFMinerJob(queue, result, resultsLock, resultsCondVar, indexedResult);
            Thread job = new Thread(jobObject);
            currentJobs.add(job);
            jobObjects.add(jobObject);

        }

        for (Thread job : currentJobs) {
            job.start();
        }

        for (Thread job : currentJobs) {
            job.join();
        }

        if (realTime) {
            consumerObj.terminate();
            consumerThread.join();
        }
        
        if (assistant.isVerbose()) queue.printStats();

        return result;
    }

    /**
     * It removes and prints rules from a shared list (a list accessed by
     * multiple threads).
     *
     * @author galarrag
     *
     */
    protected class RuleConsumer implements Runnable {

        protected List<Rule> consumeList;

        protected int lastConsumedIndex;

        protected Lock consumeLock;

        protected Condition conditionVariable;

        public RuleConsumer(List<Rule> consumeList, Lock consumeLock, Condition conditionVariable) {
            this.consumeList = consumeList;
            this.lastConsumedIndex = -1;
            this.consumeLock = consumeLock;
            this.conditionVariable = conditionVariable;
        }

        @Override
        public void run() {
            AMIEDebug.printRuleHeaders(assistant);
            while (!Thread.currentThread().isInterrupted()) {
                consumeLock.lock();
                try {
                    while (lastConsumedIndex == consumeList.size() - 1) {
                        conditionVariable.await();
                        for (int i = lastConsumedIndex + 1; i < consumeList.size(); ++i) {
                            System.out.println(assistant.formatRule(consumeList.get(i)));
                        }
                        lastConsumedIndex = consumeList.size() - 1;
                        if (done) {
                            consumeLock.unlock();
                            return;
                        }
                    }
                } catch (InterruptedException e) {
                    consumeLock.unlock();
                    System.out.flush();
                    break;
                }
            }
        }

        private boolean done = false;

        /**
         * Use to nicely terminate reader thread.
         */
        public void terminate() {
            consumeLock.lock();
            done = true;
            conditionVariable.signalAll();
            consumeLock.unlock();
        }
    }

    /**
     * This class implements the AMIE algorithm in a single thread.
     *
     * @author lgalarra
     */
    protected class RDFMinerJob implements Runnable {
        
        public static final boolean _DEBUG_GENERATION_ = false;
        public static final boolean _DEBUG_OUTPUT_ = true;

        protected List<Rule> outputSet;

        // A version of the output set thought for search.
        protected MultiMap<Integer, Rule> indexedOutputSet;

        protected AMIEQueueDebug queryPool;

        protected Lock resultsLock;

        protected Condition resultsCondition;

        /**
         *
         * @param seedsPool
         * @param outputSet
         * @param resultsLock Lock associated to the output buffer were mined
         * rules are added
         * @param resultsCondition Condition variable associated to the results
         * lock
         * @param sharedCounter Reference to a shared counter that keeps track
         * of the number of threads that are running in the system.
         * @param indexedOutputSet
         */
        public RDFMinerJob(AMIEQueueDebug seedsPool,
                List<Rule> outputSet, Lock resultsLock,
                Condition resultsCondition,
                MultiMap<Integer, Rule> indexedOutputSet) {
            this.queryPool = seedsPool;
            this.outputSet = outputSet;
            this.resultsLock = resultsLock;
            this.resultsCondition = resultsCondition;
            this.indexedOutputSet = indexedOutputSet;
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
                    if (_DEBUG_GENERATION_) {
                        System.err.println("[PopQ] Rule: " + currentRule.getRuleString());
                    }
                    // Check if the rule meets the language bias and confidence thresholds and
                    // decide whether to output it.
                    boolean outputRule = false;
                    if (assistant.shouldBeOutput(currentRule)) {
                        boolean ruleSatisfiesConfidenceBounds
                                = assistant.calculateConfidenceBoundsAndApproximations(currentRule);
                        if (ruleSatisfiesConfidenceBounds) {
                            this.resultsLock.lock();
                            assistant.setAdditionalParents(currentRule, indexedOutputSet);
                            this.resultsLock.unlock();
                            // Calculate the metrics
                            assistant.calculateConfidenceMetrics(currentRule);
                            // Check the confidence threshold and skyline technique.
                            outputRule = assistant.testConfidenceThresholds(currentRule);
                        } else {
                            outputRule = false;
                        }
                    }
                    
                    if (currentRule.getLength() == 5 &&
                            QueryEquivalenceChecker3.areEquivalent(currentRule.getTriples(), 
                                    KB.triples("?a  <hasZ>  ?b  ?g  <connectsTo>  ?a  ?n  <hasZ>  ?b  ?g  <relatesTo>  ?a  ?g  <relatesTo>  ?n"))) {
                        System.err.println("FOUND YA");
                    }
                    
                    if (_DEBUG_OUTPUT_ && currentRule.getLength() > 2) {
                        Rule rule2 = currentRule.getAlternativeEquivalent();
                        if (assistant.shouldBeOutput(currentRule) != assistant.shouldBeOutput(rule2)) {
                            System.err.println("[Output] ERROR shouldBeOutput for rule: " + currentRule.getRuleString());
                        } else if (assistant.shouldBeOutput(currentRule)) {
                            boolean ruleSatisfiesConfidenceBounds
                                = assistant.calculateConfidenceBoundsAndApproximations(currentRule);
                            if (ruleSatisfiesConfidenceBounds != assistant.calculateConfidenceBoundsAndApproximations(rule2)) {
                                System.err.println("[Output] ERROR calculateConfidenceBoundsAndApproximations for rule: " + currentRule.getRuleString());
                            } else if (ruleSatisfiesConfidenceBounds) {
                                this.resultsLock.lock();
                                assistant.setAdditionalParents(currentRule, indexedOutputSet);
                                assistant.setAdditionalParents(rule2, indexedOutputSet);
                                this.resultsLock.unlock();
                                if (!rule2.getAncestors().equals(currentRule.getAncestors())) {
                                    this.resultsLock.lock();
                                    System.err.println("[Output] ERROR ancestors for rule: " + currentRule.getRuleString());
                                    TSVRuleDiff.diff(currentRule.getAncestors(), rule2.getAncestors());
                                    this.resultsLock.unlock();
                                }
                                assistant.calculateConfidenceMetrics(rule2);
                                if (currentRule.getSupport() != rule2.getSupport()) {
                                    System.err.println("[Output] ERROR support for rule: " + currentRule.getRuleString());
                                }
                                if (currentRule.getPcaBodySize() != rule2.getPcaBodySize()) {
                                    System.err.println("[Output] ERROR PCA BS for rule: " + currentRule.getRuleString());
                                }
                                if (currentRule.getBodySize() != rule2.getBodySize()) {
                                    System.err.println("[Output] ERROR BS for rule: " + currentRule.getRuleString());
                                }
                                if (assistant.testConfidenceThresholds(currentRule) != assistant.testConfidenceThresholds(rule2)) {
                                    this.resultsLock.lock();
                                    System.err.println("[Output] ERROR testConfidenceThresholds for rule: " + currentRule.getRuleString());
                                    for (Rule r : currentRule.getAncestors()) {
                                        System.err.println("< " + r.getRuleString() + ": " + 
                                                ((indexedOutputSet.get(r.alternativeParentHashCode()) == null) ? "No alternative Key" : ((indexedOutputSet.get(r.alternativeParentHashCode()).contains(r)) ? "OK" : "Not in")));
                                    }
                                    for (Rule r : rule2.getAncestors()) {
                                        System.err.println("> " + r.getRuleString() + ": " + 
                                                ((indexedOutputSet.get(r.alternativeParentHashCode()) == null) ? "No alternative Key" : ((indexedOutputSet.get(r.alternativeParentHashCode()).contains(r)) ? "OK" : "Not in")));
                                    }
                                    this.resultsLock.unlock();
                                }
                            }
                        }
                    }
                    
                    // Check if we should further refine the rule
                    boolean furtherRefined = !currentRule.isFinal();
                    if (assistant.isEnablePerfectRules()) {
                        furtherRefined = !currentRule.isPerfect();
                    }
                    
                    // If so specialize it
                    if (furtherRefined) {
                        double threshold = getCountThreshold(currentRule);

                        // Application of the mining operators
                        Map<String, Collection<Rule>> temporalOutputMap = null;
                        try {
                            temporalOutputMap = assistant.applyMiningOperators(currentRule, threshold);
                        } catch (IllegalAccessException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IllegalArgumentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        
                        if (_DEBUG_GENERATION_ && currentRule.getLength() > 2) {
                                                    
                            Map<String, Collection<Rule>> temporalOutputMap2 = null;
                            Rule rule2 = currentRule.getAlternativeEquivalent();
                            double threshold2 = getCountThreshold(rule2);
                            try {
                                temporalOutputMap2 = assistant.applyMiningOperators(rule2, threshold2);
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.exit(12);
                            }
                            this.resultsLock.lock();
                            for (String method : temporalOutputMap.keySet()) {
                                System.err.println("[Diff] Rule: " + currentRule.getRuleString() + ", Method: " + method);
                                Set<Rule> rules1 = new HashSet<>(temporalOutputMap.get(method));
                                Set<Rule> rules2 = Collections.EMPTY_SET;
                                Collection<Rule> r2;
                                if ((r2 = temporalOutputMap2.get(method)) != null) {
                                    rules2 = new HashSet<>(r2);
                                }
                                TSVRuleDiff.diff(rules1, rules2);
                            }
                            this.resultsLock.unlock();
                        }

                        for (Map.Entry<String, Collection<Rule>> entry : temporalOutputMap.entrySet()) {
                            String operator = entry.getKey();
                            Collection<Rule> items = entry.getValue();
                            if (!operator.equals("dangling")) {
                                queryPool.queueAll(items);
                                if (_DEBUG_GENERATION_) {
                                    for (Rule r : items) {
                                        System.err.println("[PushQ] Rule: " + r.getRuleString());
                                    }
                                }
                            }
                        }

                        // Addition of the specializations to the queue
                        //queryPool.queueAll(temporalOutput);                            
                        if (currentRule.getRealLength()
                                < assistant.getMaxDepth() - 1) {
                            if (temporalOutputMap.containsKey("dangling")) {
                                queryPool.queueAll(temporalOutputMap.get("dangling"));
                                if (_DEBUG_GENERATION_) {
                                    for (Rule r : temporalOutputMap.get("dangling")) {
                                        System.err.println("[PushQ] Rule: " + r.getRuleString());
                                    }
                                }
                            }
                        }
                    }

                    // Output the rule
                    if (outputRule) {
                        this.resultsLock.lock();
                        Set<Rule> outputQueries = indexedOutputSet.get(currentRule.alternativeParentHashCode());
                        if (outputQueries != null) {
                            if (!outputQueries.contains(currentRule)) {
                                this.outputSet.add(currentRule);
                                outputQueries.add(currentRule);
                            } else {
                                throw new IllegalStateException("A query cannot be added twice");
                            }
                        } else {
                            this.outputSet.add(currentRule);
                            this.indexedOutputSet.put(currentRule.alternativeParentHashCode(), currentRule);
                        }
                        this.resultsCondition.signal();
                        this.resultsLock.unlock();
                    }
                }
            }
        }

        /**
         * Based on AMIE's configuration, it returns the absolute support
         * threshold that should be applied to the rule.
         *
         * @param query
         * @return
         */
        protected double getCountThreshold(Rule query) {
            switch (pruningMetric) {
                case Support:
                    return minSignificanceThreshold;
                case HeadCoverage:
                    return Math.ceil((minSignificanceThreshold
                            * (double) assistant.getHeadCardinality(query)));
                default:
                    return 0;
            }
        }
    }

    /**
     * Returns an instance of AMIE that mines rules on the given KB using the
     * vanilla setting of head coverage 1% and no confidence threshold.
     *
     * @param db
     * @return
     */
    public static AMIEDebug getVanillaSettingInstance(KB db) {
        return new AMIEDebug(new DefaultMiningAssistant(db),
                100, // Do not look at relations smaller than 100 facts 
                0.01, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Factory methods. They return canned instances of AMIE. *
     */
    /**
     * Returns an instance of AMIE that mines rules on the given KB using the
     * vanilla setting of head coverage 1% and a given PCA confidence threshold
     *
     * @param db
     * @return
     */
    public static AMIEDebug getVanillaSettingInstance(KB db, double minPCAConfidence) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        return new AMIEDebug(miningAssistant,
                DEFAULT_INITIAL_SUPPORT, // Do not look at relations smaller than 100 facts 
                DEFAULT_HEAD_COVERAGE, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns an (vanilla setting) instance of AMIE that enables the lossy
     * optimizations, i.e., optimizations that optimize for runtime but that
     * could in principle omit some rules that should be mined.
     *
     * @param db
     * @param minPCAConfidence
     * @param startSupport
     * @return
     */
    public static AMIEDebug getLossyVanillaSettingInstance(KB db, double minPCAConfidence, int startSupport) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        miningAssistant.setEnabledConfidenceUpperBounds(true);
        miningAssistant.setEnabledFunctionalityHeuristic(true);
        return new AMIEDebug(miningAssistant,
                startSupport, // Do not look at relations smaller than 100 facts 
                DEFAULT_HEAD_COVERAGE, // Head coverage 1%
                Metric.HeadCoverage,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns an instance of AMIE that enables the lossy optimizations, i.e.,
     * optimizations that optimize for runtime but that could in principle omit
     * some rules that should be mined.
     *
     * @param db
     * @param minPCAConfidence
     * @param minSupport
     * @return
     */
    public static AMIEDebug getLossyInstance(KB db, double minPCAConfidence, int minSupport) {
        DefaultMiningAssistant miningAssistant = new DefaultMiningAssistant(db);
        miningAssistant.setPcaConfidenceThreshold(minPCAConfidence);
        miningAssistant.setEnabledConfidenceUpperBounds(true);
        miningAssistant.setEnabledFunctionalityHeuristic(true);
        return new AMIEDebug(miningAssistant,
                minSupport, // Do not look at relations smaller than the support threshold 
                minSupport, // Head coverage 1%
                Metric.Support,
                Runtime.getRuntime().availableProcessors());
    }

    /**
     * Gets an instance of AMIE configured according to the command line
     * arguments.
     *
     * @param args
     * @return
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static AMIEDebug getInstance(String[] args)
            throws IOException, InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        List<File> dataFiles = new ArrayList<File>();
        List<File> targetFiles = new ArrayList<File>();
        List<File> schemaFiles = new ArrayList<File>();

        CommandLine cli = null;
        double minStdConf = DEFAULT_STD_CONFIDENCE;
        double minPCAConf = DEFAULT_PCA_CONFIDENCE;
        int minSup = DEFAULT_SUPPORT;
        int minInitialSup = DEFAULT_INITIAL_SUPPORT;
        double minHeadCover = DEFAULT_HEAD_COVERAGE;
        int maxDepth = 3;
        int recursivityLimit = 3;
        boolean realTime = true;
        boolean datalogOutput = true;
        boolean countAlwaysOnSubject = false;
        double minMetricValue = 0.0;
        boolean allowConstants = false;
        boolean enableConfidenceUpperBounds = true;
        boolean enableFunctionalityHeuristic = true;
        boolean verbose = false;
        boolean enforceConstants = false;
        boolean avoidUnboundTypeAtoms = true;
        boolean ommitStdConfidence = false;
        boolean adaptiveInstantiations = false;
        /**
         * System performance measure *
         */
        boolean exploitMaxLengthForRuntime = true;
        boolean enableQueryRewriting = true;
        boolean enablePerfectRulesPruning = true;
        long sourcesLoadingTime = 0l;
        /**
         * ******************************
         */
        int nProcessors = Runtime.getRuntime().availableProcessors();
        String bias = "default"; // Counting support on the two head variables.
        Metric metric = Metric.HeadCoverage; // Metric used to prune the search space.
        VariableOrder variableOrder = new FunctionalOrder();
        MiningAssistant mineAssistant = null;
        IntCollection bodyExcludedRelations = null;
        IntCollection headExcludedRelations = null;
        IntCollection headTargetRelations = null;
        IntCollection bodyTargetRelations = null;
        KB targetSource = null;
        KB schemaSource = null;
        int nThreads = nProcessors; // By default use as many threads as processors.
        HelpFormatter formatter = new HelpFormatter();

        // create the command line parser
        CommandLineParser parser = new PosixParser();
        // create the Options
        Options options = new Options();

        Option supportOpt = OptionBuilder.withArgName("min-support")
                .hasArg()
                .withDescription("Minimum absolute support. Default: 100 positive examples")
                .create("mins");

        Option initialSupportOpt = OptionBuilder.withArgName("min-initial-support")
                .hasArg()
                .withDescription("Minimum size of the relations to be considered as head relations. "
                        + "Default: 100 (facts or entities depending on the bias)")
                .create("minis");

        Option headCoverageOpt = OptionBuilder.withArgName("min-head-coverage")
                .hasArg()
                .withDescription("Minimum head coverage. Default: 0.01")
                .create("minhc");

        Option pruningMetricOpt = OptionBuilder.withArgName("pruning-metric")
                .hasArg()
                .withDescription("Metric used for pruning of intermediate queries: "
                        + "support|headcoverage. Default: headcoverage")
                .create("pm");

        Option realTimeOpt = OptionBuilder.withArgName("output-at-end")
                .withDescription("Print the rules at the end and not while they are discovered. "
                        + "Default: false")
                .create("oute");

        Option datalogNotationOpt = OptionBuilder.withArgName("datalog-output")
                .withDescription("Print rules using the datalog notation "
                        + "Default: false")
                .create("datalog");

        Option bodyExcludedOpt = OptionBuilder.withArgName("body-excluded-relations")
                .hasArg()
                .withDescription("Do not use these relations as atoms in the body of rules."
                        + " Example: <livesIn>,<bornIn>")
                .create("bexr");

        Option headExcludedOpt = OptionBuilder.withArgName("head-excluded-relations")
                .hasArg()
                .withDescription("Do not use these relations as atoms in the head of rules "
                        + "(incompatible with head-target-relations). Example: <livesIn>,<bornIn>")
                .create("hexr");

        Option headTargetRelationsOpt = OptionBuilder.withArgName("head-target-relations")
                .hasArg()
                .withDescription("Mine only rules with these relations in the head. "
                        + "Provide a list of relation names separated by commas "
                        + "(incompatible with head-excluded-relations). "
                        + "Example: <livesIn>,<bornIn>")
                .create("htr");

        Option bodyTargetRelationsOpt = OptionBuilder.withArgName("body-target-relations")
                .hasArg()
                .withDescription("Allow only these relations in the body. Provide a list of relation "
                        + "names separated by commas (incompatible with body-excluded-relations). "
                        + "Example: <livesIn>,<bornIn>")
                .create("btr");

        Option maxDepthOpt = OptionBuilder.withArgName("max-depth")
                .hasArg()
                .withDescription("Maximum number of atoms in the antecedent and succedent of rules. "
                        + "Default: 3")
                .create("maxad");

        Option pcaConfThresholdOpt = OptionBuilder.withArgName("min-pca-confidence")
                .hasArg()
                .withDescription("Minimum PCA confidence threshold. "
                        + "This value is not used for pruning, only for filtering of the results. "
                        + "Default: 0.0")
                .create("minpca");

        Option allowConstantsOpt = OptionBuilder.withArgName("allow-constants")
                .withDescription("Enable rules with constants. Default: false")
                .create("const");

        Option enforceConstantsOpt = OptionBuilder.withArgName("only-constants")
                .withDescription("Enforce constants in all atoms. Default: false")
                .create("fconst");

        Option assistantOp = OptionBuilder.withArgName("e-name")
                .hasArg()
                .withDescription("Syntatic/semantic bias: oneVar|default|lazy|lazit|[Path to a subclass of amie.mining.assistant.MiningAssistant]"
                        + "Default: default (defines support and confidence in terms of 2 head variables given an order, cf -vo)")
                .create("bias");

        Option countOnSubjectOpt = OptionBuilder.withArgName("count-always-on-subject")
                .withDescription("If a single variable bias is used (oneVar), "
                        + "force to count support always on the subject position.")
                .create("caos");

        Option coresOp = OptionBuilder.withArgName("n-threads")
                .hasArg()
                .withDescription("Preferred number of cores. Round down to the actual number of cores "
                        + "in the system if a higher value is provided.")
                .create("nc");

        Option stdConfThresholdOpt = OptionBuilder.withArgName("min-std-confidence")
                .hasArg()
                .withDescription("Minimum standard confidence threshold. "
                        + "This value is not used for pruning, only for filtering of the results. Default: 0.0")
                .create("minc");

        Option confidenceBoundsOp = OptionBuilder.withArgName("optim-confidence-bounds")
                .withDescription("Enable the calculation of confidence upper bounds to prune rules.")
                .create("optimcb");

        Option funcHeuristicOp = OptionBuilder.withArgName("optim-func-heuristic")
                .withDescription("Enable functionality heuristic to identify potential low confident rules for pruning.")
                .create("optimfh");

        Option verboseOp = OptionBuilder.withArgName("verbose")
                .withDescription("Maximal verbosity")
                .create("verbose");

        Option recursivityLimitOpt = OptionBuilder.withArgName("recursivity-limit")
                .withDescription("Recursivity limit")
                .hasArg()
                .create("rl");

        Option avoidUnboundTypeAtomsOpt = OptionBuilder.withArgName("avoid-unbound-type-atoms")
                .withDescription("Avoid unbound type atoms, e.g., type(x, y), i.e., bind always 'y' to a type")
                .create("auta");

        Option doNotExploitMaxLengthOp = OptionBuilder.withArgName("do-not-exploit-max-length")
                .withDescription("Do not exploit max length for speedup "
                        + "(requested by the reviewers of AMIE+). False by default.")
                .create("deml");

        Option disableQueryRewriteOp = OptionBuilder.withArgName("disable-query-rewriting")
                .withDescription("Disable query rewriting and caching.")
                .create("dqrw");

        Option disablePerfectRulesOp = OptionBuilder.withArgName("disable-perfect-rules")
                .withDescription("Disable perfect rules.")
                .create("dpr");

        Option onlyOutputEnhancementOp = OptionBuilder.withArgName("only-output")
                .withDescription("If enabled, it activates only the output enhacements, that is, "
                        + "the confidence approximation and upper bounds. "
                        + " It overrides any other configuration that is incompatible.")
                .create("oout");

        Option fullOp = OptionBuilder.withArgName("full")
                .withDescription("It enables all enhancements: "
                        + "lossless heuristics and confidence approximation and upper bounds"
                        + " It overrides any other configuration that is incompatible.")
                .create("full");

        Option noHeuristicsOp = OptionBuilder.withArgName("noHeuristics")
                .withDescription("Disable functionality heuristic, should be used with the -full option")
                .create("noHeuristics");

        Option noKbRewrite = OptionBuilder.withArgName("noKbRewrite")
                .withDescription("Prevent the KB to rewrite query when counting pairs")
                .create("noKbRewrite");

        Option noKbExistsDetection = OptionBuilder.withArgName("noKbExistsDetection")
                .withDescription("Prevent the KB to detect existential variable on-the-fly "
                        + "and to optimize the query")
                .create("noKbExistsDetection");

        Option variableOrderOp = OptionBuilder.withArgName("variableOrder")
                .withDescription("Define the order of the variable in counting query among: app, fun (default), ifun")
                .hasArg()
                .create("vo");

        Option extraFileOp = OptionBuilder.withArgName("extraFile")
                .withDescription("An additional text file whose interpretation depends "
                        + "on the selected mining assistant (bias)")
                .hasArg()
                .create("ef");

        Option calculateStdConfidenceOp = OptionBuilder.withArgName("ommit-std-conf")
                .withDescription("Do not calculate standard confidence")
                .create("ostd");

        Option enableCountCache = OptionBuilder.withArgName("count-cache")
                .withDescription("Cache count results")
                .create("cc");

        Option optimAdaptiveInstantiations = OptionBuilder.withArgName("adaptive-instantiations")
                .withDescription("Prune instantiated rules that decrease too much the support of their parent rule (ratio 0.2)")
                .create("optimai");

        Option multilingual = OptionBuilder.withArgName("multilingual")
                .withDescription("Parse labels language as new facts")
                .create("mlg");

        Option delimOpt = OptionBuilder.withArgName("delimiter")
                .withDescription("Separator in input files (default: TAB)")
                .hasArg()
                .create("d");

        options.addOption(stdConfThresholdOpt);
        options.addOption(supportOpt);
        options.addOption(initialSupportOpt);
        options.addOption(headCoverageOpt);
        options.addOption(pruningMetricOpt);
        options.addOption(realTimeOpt);
        options.addOption(bodyExcludedOpt);
        options.addOption(headExcludedOpt);
        options.addOption(maxDepthOpt);
        options.addOption(pcaConfThresholdOpt);
        options.addOption(headTargetRelationsOpt);
        options.addOption(bodyTargetRelationsOpt);
        options.addOption(allowConstantsOpt);
        options.addOption(enforceConstantsOpt);
        options.addOption(countOnSubjectOpt);
        options.addOption(assistantOp);
        options.addOption(coresOp);
        options.addOption(confidenceBoundsOp);
        options.addOption(verboseOp);
        options.addOption(funcHeuristicOp);
        options.addOption(recursivityLimitOpt);
        options.addOption(avoidUnboundTypeAtomsOpt);
        options.addOption(doNotExploitMaxLengthOp);
        options.addOption(disableQueryRewriteOp);
        options.addOption(disablePerfectRulesOp);
        options.addOption(onlyOutputEnhancementOp);
        options.addOption(fullOp);
        options.addOption(noHeuristicsOp);
        options.addOption(noKbRewrite);
        options.addOption(noKbExistsDetection);
        options.addOption(variableOrderOp);
        options.addOption(extraFileOp);
        options.addOption(datalogNotationOpt);
        options.addOption(calculateStdConfidenceOp);
        //options.addOption(enableCountCache);
        options.addOption(optimAdaptiveInstantiations);
        options.addOption(multilingual);
        options.addOption(delimOpt);

        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        // These configurations override any other option
        boolean onlyOutput = cli.hasOption("oout");
        boolean full = cli.hasOption("full");
        if (onlyOutput && full) {
            System.err.println("The options only-output and full are incompatible. Pick either one.");
            formatter.printHelp("AMIE [OPTIONS] <TSV FILES>", options);
            System.exit(1);
        }

        if (cli.hasOption("htr") && cli.hasOption("hexr")) {
            System.err.println("The options head-target-relations and head-excluded-relations cannot appear at the same time");
            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        if (cli.hasOption("btr") && cli.hasOption("bexr")) {
            System.err.println("The options body-target-relations and body-excluded-relations cannot appear at the same time");
            formatter.printHelp("AMIE+", options);
            System.exit(1);
        }

        if (cli.hasOption("mins")) {
            String minSupportStr = cli.getOptionValue("mins");
            try {
                minSup = Integer.parseInt(minSupportStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -mins (support threshold) requires an integer as argument");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("minis")) {
            String minInitialSupportStr = cli.getOptionValue("minis");
            try {
                minInitialSup = Integer.parseInt(minInitialSupportStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -minis (initial support threshold) requires an integer as argument");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("minhc")) {
            String minHeadCoverage = cli.getOptionValue("minhc");
            try {
                minHeadCover = Double.parseDouble(minHeadCoverage);
            } catch (NumberFormatException e) {
                System.err.println("The option -minhc (head coverage threshold) requires a real number as argument");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("minc")) {
            String minConfidenceStr = cli.getOptionValue("minc");
            try {
                minStdConf = Double.parseDouble(minConfidenceStr);
            } catch (NumberFormatException e) {
                System.err.println("The option -minc (confidence threshold) requires a real number as argument");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("minpca")) {
            String minicStr = cli.getOptionValue("minpca");
            try {
                minPCAConf = Double.parseDouble(minicStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -minpca (PCA confidence threshold) must be an integer greater than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }

        if (cli.hasOption("bexr")) {
            bodyExcludedRelations = new IntArrayList();
            String excludedValuesStr = cli.getOptionValue("bexr");
            String[] excludedValueArr = excludedValuesStr.split(",");
            for (String excludedValue : excludedValueArr) {
                bodyExcludedRelations.add(KB.map(excludedValue.trim()));
            }
        }

        if (cli.hasOption("btr")) {
            bodyTargetRelations = new IntArrayList();
            String targetBodyValuesStr = cli.getOptionValue("btr");
            String[] bodyTargetRelationsArr = targetBodyValuesStr.split(",");
            for (String targetString : bodyTargetRelationsArr) {
                bodyTargetRelations.add(KB.map(targetString.trim()));
            }
        }

        if (cli.hasOption("htr")) {
            headTargetRelations = new IntArrayList();
            String targetValuesStr = cli.getOptionValue("htr");
            String[] targetValueArr = targetValuesStr.split(",");
            for (String targetValue : targetValueArr) {
                headTargetRelations.add(KB.map(targetValue.trim()));
            }
        }

        if (cli.hasOption("hexr")) {
            headExcludedRelations = new IntArrayList();
            String excludedValuesStr = cli.getOptionValue("hexr");
            String[] excludedValueArr = excludedValuesStr.split(",");
            for (String excludedValue : excludedValueArr) {
                headExcludedRelations.add(KB.map(excludedValue.trim()));
            }
        }

        if (cli.hasOption("maxad")) {
            String maxDepthStr = cli.getOptionValue("maxad");
            try {
                maxDepth = Integer.parseInt(maxDepthStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -maxad (maximum depth) must be an integer greater than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }

            if (maxDepth < 2) {
                System.err.println("The argument for option -maxad (maximum depth) must be greater or equal than 2");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                formatter.printHelp("AMIE+", options);
                System.exit(1);
            }
        }

        if (cli.hasOption("nc")) {
            String nCoresStr = cli.getOptionValue("nc");
            try {
                nThreads = Integer.parseInt(nCoresStr);
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -nc (number of threads) must be an integer");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }

            if (nThreads > nProcessors) {
                nThreads = nProcessors;
            }
        }

        if (cli.hasOption("vo")) {
            switch (cli.getOptionValue("vo")) {
                case "app":
                    variableOrder = new AppearanceOrder();
                    break;
                case "fun":
                    variableOrder = new FunctionalOrder();
                    break;
                case "ifun":
                    variableOrder = InverseOrder.of(new FunctionalOrder());
                    break;
                default:
                    System.err.println("The argument for option -vo must be among \"app\", \"fun\" and \"ifun\".");
                    System.exit(1);
            }
        }

        avoidUnboundTypeAtoms = cli.hasOption("auta");
        exploitMaxLengthForRuntime = !cli.hasOption("deml");
        enableQueryRewriting = !cli.hasOption("dqrw");
        enablePerfectRulesPruning = !cli.hasOption("dpr");
        String[] leftOverArgs = cli.getArgs();

        if (leftOverArgs.length < 1) {
            System.err.println("No input file has been provided");
            System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
            System.exit(1);
        }

        //Load database
        for (int i = 0; i < leftOverArgs.length; ++i) {
            if (leftOverArgs[i].startsWith(":t")) {
                targetFiles.add(new File(leftOverArgs[i].substring(2)));
            } else if (leftOverArgs[i].startsWith(":s")) {
                schemaFiles.add(new File(leftOverArgs[i].substring(2)));
            } else {
                dataFiles.add(new File(leftOverArgs[i]));
            }
        }
        KB dataSource = new KB();
        if (cli.hasOption("mlg")) {
            dataSource = new MultilingualKB();
        }

        if (cli.hasOption("d")) {
            dataSource.setDelimiter(cli.getOptionValue("d"));
        }

        if (cli.hasOption("noKbRewrite")) {
            dataSource.setOptimConnectedComponent(false);
        }

        if (cli.hasOption("noKbExistsDetection")) {
            dataSource.setOptimExistentialDetection(false);
        }

        long timeStamp1 = System.currentTimeMillis();
        dataSource.load(dataFiles);
        long timeStamp2 = System.currentTimeMillis();

        sourcesLoadingTime = timeStamp2 - timeStamp1;

        if (!targetFiles.isEmpty()) {
            targetSource = new KB();
            targetSource.load(targetFiles);
        }

        if (!schemaFiles.isEmpty()) {
            schemaSource = new KB();
            schemaSource.load(schemaFiles);
        }

        if (cli.hasOption("pm")) {
            switch (cli.getOptionValue("pm")) {
                case "support":
                    metric = Metric.Support;
                    System.err.println("Using " + metric + " as pruning metric with threshold " + minSup);
                    minMetricValue = minSup;
                    minInitialSup = minSup;
                    break;
                default:
                    metric = Metric.HeadCoverage;
                    System.err.println("Using " + metric + " as pruning metric with threshold " + minHeadCover);
                    break;
            }
        } else {
            System.out.println("Using " + metric + " as pruning metric with minimum threshold " + minHeadCover);
            minMetricValue = minHeadCover;
        }

        if (cli.hasOption("bias")) {
            bias = cli.getOptionValue("bias");
        }

        verbose = cli.hasOption("verbose");

        if (cli.hasOption("rl")) {
            try {
                recursivityLimit = Integer.parseInt(cli.getOptionValue("rl"));
            } catch (NumberFormatException e) {
                System.err.println("The argument for option -rl (recursivity limit) must be an integer");
                System.err.println("AMIE [OPTIONS] <.tsv INPUT FILES>");
                System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                System.exit(1);
            }
        }
        System.out.println("Using recursivity limit " + recursivityLimit);

        enableConfidenceUpperBounds = cli.hasOption("optimcb");
        if (enableConfidenceUpperBounds) {
            System.out.println("Enabling standard and PCA confidences upper "
                    + "bounds for pruning");
        }

        enableFunctionalityHeuristic = cli.hasOption("optimfh");

        switch (bias) {
            case "oneVar":
                mineAssistant = new MiningAssistant(dataSource);
                break;
            case "default":
                mineAssistant = new DefaultMiningAssistantWithOrder(dataSource, variableOrder);
                break;
            case "signatured":
                mineAssistant = new RelationSignatureDefaultMiningAssistant(dataSource);
                break;
            case "lazy":
                mineAssistant = new LazyMiningAssistant(dataSource, variableOrder);
                break;
            case "lazit":
                mineAssistant = new LazyIteratorMiningAssistant(dataSource, variableOrder);
                break;
            default:
                // To support customized assistant classes
                // The assistant classes must inherit from amie.mining.assistant.MiningAssistant
                // and implement a constructor with the any of the following signatures.
                // ClassName(amie.data.KB), ClassName(amie.data.KB, String), ClassName(amie.data.KB, amie.data.KB) 
                Class<?> assistantClass = null;
                try {
                    assistantClass = Class.forName(bias);
                } catch (Exception e) {
                    System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                    e.printStackTrace();
                    System.exit(1);
                }

                Constructor<?> constructor = null;
                try {
                    // Standard constructor
                    constructor = assistantClass.getConstructor(new Class[]{KB.class});
                    mineAssistant = (MiningAssistant) constructor.newInstance(dataSource);
                } catch (NoSuchMethodException e) {
                    try {
                        // Constructor with additional input            			
                        constructor = assistantClass.getConstructor(new Class[]{KB.class, String.class});
                        System.out.println(cli.getOptionValue("ef"));
                        mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, cli.getOptionValue("ef"));
                    } catch (NoSuchMethodException ep) {
                        // Constructor with schema KB       
                        try {
                            constructor = assistantClass.getConstructor(new Class[]{KB.class, KB.class});
                            mineAssistant = (MiningAssistant) constructor.newInstance(dataSource, schemaSource);
                        } catch (Exception e2p) {
                            e.printStackTrace();
                            System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                            e.printStackTrace();
                        }
                    }
                } catch (SecurityException e) {
                    System.err.println("AMIE+ [OPTIONS] <.tsv INPUT FILES>");
                    e.printStackTrace();
                    System.exit(1);
                }
                if (mineAssistant instanceof DefaultMiningAssistantWithOrder) {
                    ((DefaultMiningAssistantWithOrder) mineAssistant).setVariableOrder(variableOrder);
                }

                break;
        }

        allowConstants = cli.hasOption("const");
        countAlwaysOnSubject = cli.hasOption("caos");
        realTime = !cli.hasOption("oute");
        enforceConstants = cli.hasOption("fconst");
        datalogOutput = cli.hasOption("datalog");
        ommitStdConfidence = cli.hasOption("ostd");
        adaptiveInstantiations = cli.hasOption("optimai");

        // These configurations override others
        if (onlyOutput) {
            System.out.println("Using the only output enhacements configuration.");
            enablePerfectRulesPruning = false;
            enableQueryRewriting = false;
            exploitMaxLengthForRuntime = false;
            enableConfidenceUpperBounds = true;
            enableFunctionalityHeuristic = true;
            minPCAConf = DEFAULT_PCA_CONFIDENCE;
        }

        if (full) {
            System.out.println("Using the FULL configuration.");
            enablePerfectRulesPruning = true;
            enableQueryRewriting = true;
            exploitMaxLengthForRuntime = true;
            enableConfidenceUpperBounds = true;
            enableFunctionalityHeuristic = true;
            minPCAConf = DEFAULT_PCA_CONFIDENCE;
        }

        if (cli.hasOption("noHeuristics")) {
            enableFunctionalityHeuristic = false;
        }

        if (enableFunctionalityHeuristic) {
            System.out.println("Enabling functionality heuristic with ratio "
                        + "for pruning of low confident rules");
            Announce.doing("Building overlap tables for confidence approximation...");
            long time = System.currentTimeMillis();
            dataSource.buildOverlapTables(nThreads);
            Announce.done("Overlap tables computed in " + NumberFormatter.formatMS(System.currentTimeMillis() - time)
                            + " using " + Integer.toString(nThreads) + " threads.");
        }

        mineAssistant.setKbSchema(schemaSource);
        mineAssistant.setEnabledConfidenceUpperBounds(enableConfidenceUpperBounds);
        mineAssistant.setEnabledFunctionalityHeuristic(enableFunctionalityHeuristic);
        mineAssistant.setMaxDepth(maxDepth);
        mineAssistant.setStdConfidenceThreshold(minStdConf);
        mineAssistant.setPcaConfidenceThreshold(minPCAConf);
        mineAssistant.setAllowConstants(allowConstants);
        mineAssistant.setEnforceConstants(enforceConstants);
        mineAssistant.setBodyExcludedRelations(bodyExcludedRelations);
        mineAssistant.setHeadExcludedRelations(headExcludedRelations);
        mineAssistant.setTargetBodyRelations(bodyTargetRelations);
        mineAssistant.setCountAlwaysOnSubject(countAlwaysOnSubject);
        mineAssistant.setRecursivityLimit(recursivityLimit);
        mineAssistant.setAvoidUnboundTypeAtoms(avoidUnboundTypeAtoms);
        mineAssistant.setExploitMaxLengthOption(exploitMaxLengthForRuntime);
        mineAssistant.setEnableQueryRewriting(enableQueryRewriting);
        mineAssistant.setEnablePerfectRules(enablePerfectRulesPruning);
        mineAssistant.setVerbose(verbose);
        mineAssistant.setOmmitStdConfidence(ommitStdConfidence);
        mineAssistant.setDatalogNotation(datalogOutput);
        mineAssistant.setOptimAdaptiveInstantiations(adaptiveInstantiations);

        System.out.println(mineAssistant.getDescription());

        AMIEDebug miner = new AMIEDebug(mineAssistant, minInitialSup, minMetricValue, metric, nThreads);
        miner.setRealTime(realTime);
        miner.setSeeds(headTargetRelations);

        if (minStdConf > 0.0) {
            System.out.println("Filtering on standard confidence with minimum threshold " + minStdConf);
        } else {
            System.out.println("No minimum threshold on standard confidence");
        }

        if (minPCAConf > 0.0) {
            System.out.println("Filtering on PCA confidence with minimum threshold " + minPCAConf);
        } else {
            System.out.println("No minimum threshold on PCA confidence");
        }

        if (enforceConstants) {
            System.out.println("Constants in the arguments of relations are enforced");
        } else if (allowConstants) {
            System.out.println("Constants in the arguments of relations are enabled");
        } else {
            System.out.println("Constants in the arguments of relations are disabled");
        }

        if (exploitMaxLengthForRuntime && enableQueryRewriting && enablePerfectRulesPruning) {
            System.out.println("Lossless (query refinement) heuristics enabled");
        } else {
            if (!exploitMaxLengthForRuntime) {
                System.out.println("Pruning by maximum rule length disabled");
            }

            if (!enableQueryRewriting) {
                System.out.println("Query rewriting and caching disabled");
            }

            if (!enablePerfectRulesPruning) {
                System.out.println("Perfect rules pruning disabled");
            }
        }

        return miner;
    }

    protected static void printRuleHeaders(MiningAssistant assistant) {
        List<String> finalHeaders = new ArrayList<>(headers);
        if (assistant.isOmmitStdConfidence()) {
            finalHeaders.removeAll(Arrays.asList("Std Confidence", "Body size"));
        }

        if (!assistant.isVerbose()) {
            finalHeaders.removeAll(Arrays.asList("Std. Lower Bound", "PCA Lower Bound", "PCA Conf estimation"));
        }

        System.out.println(String.join("\t", finalHeaders));
    }

    /**
     * AMIE's main program
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Schema.loadSchemaConf();
        System.out.println("Assuming " + Schema.typeRelationBS + " as type relation");
        long loadingStartTime = System.currentTimeMillis();
        AMIEDebug miner = AMIEDebug.getInstance(args);
        long loadingTime = System.currentTimeMillis() - loadingStartTime;
        MiningAssistant assistant = miner.getAssistant();

        System.out.println("MRT calls: " + String.valueOf(KB.STAT_NUMBER_OF_CALL_TO_MRT.get()));
        Announce.doing("Starting the mining phase");

        long time = System.currentTimeMillis();
        List<Rule> rules = miner.mine();

        if (!miner.isRealTime()) {
            AMIEDebug.printRuleHeaders(assistant);
            for (Rule rule : rules) {
                System.out.println(assistant.formatRule(rule));
            }
        }

        long miningTime = System.currentTimeMillis() - time;
        System.out.println("Mining done in " + NumberFormatter.formatMS(miningTime));
        Announce.done("Total time " + NumberFormatter.formatMS(miningTime + loadingTime));
        System.out.println(rules.size() + " rules mined.");

//	    if (assistant.kb.countCacheEnabled) {
//                System.out.println("MRT calls: " + String.valueOf(KB.STAT_NUMBER_OF_CALL_TO_MRT.get()));
//	    	System.out.println("Queries: " + String.valueOf(KB.queryCache.queryCount.get()));
//	        System.out.println("Matches: " + String.valueOf(KB.countCacheMatch.get()));
//	    	System.out.println("Collisions: " + String.valueOf(KB.queryCache.collisionCount.get()));
//          }
    }

}
