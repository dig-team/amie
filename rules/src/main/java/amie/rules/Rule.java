/**
 * @author lgalarra
 * @date Aug 8, 2012
 */
package amie.rules;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import javatools.datatypes.Pair;
import amie.data.Schema;
import amie.data.U;
import amie.data.KB;
import static amie.data.U.increase;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

/**
 * A class that represents Horn rules of the form A =&gt; B where A is a conjunction of binary atoms
 * of the form r(x, y). Each atom is represented as a triple [x, r, y] (subject, relation, object).
 * @author lgalarra
 *
 */
public class Rule {

    /**
     * The triple patterns
     */
    List<int[]> triples;

    /**
     * ****** Standard Metrics ************
     */
    /**
     * Support normalized w.r.t to the size of the head relation
     */
    double headCoverage;

    /**
     * Support w.r.t the set of all subjects in the database
     */
    double supportRatio;

    /**
     * Absolute number of bindings of the projection variables of the query
     * (positive examples)
     */
    double support;

    /**
     * In AMIE the support may change when the rule is enhanced with
     * "special" atoms such as the DIFFERENTFROMbs database command. Since the
     * cardinality is used in the hashCode function (good to guarantee balanced
     * hash tables), we store the first computed cardinality of the query.
     * Unlike the real cardinality, this values remains constant from the
     * creation of the object.
     */
    long initialSupport;

    /**
     * String unique key for the head of the query
     */
    private int headKey;

    /**
     * Parent query
     */
    private Rule parent;

    /**
     * List of parents: queries that are equivalent to the current query but
     * contain a body atom less.
     */
    private Set<Rule> ancestors;

    /**
     * The position of the counting variable in the head atom
     */
    private int functionalVariablePosition;

    /**
     * The number of instances of the counting variable in the antecedent. (size
     * of B in B => r(x, y))
     */
    private long bodySize;

    /**
     * Integer counter used to guarantee unique variable names
     */
    private static int varsCount = 0;

    /**
     * Body - Head (whatever is false or unknown in the database)
     */
    private long bodyMinusHeadSize;

    /**
     * Body - Head* (existential version of the head)
     */
    private double pcaBodySize;

    /**
     * Highest letter used for variable names
     */
    private int highestVariable;

    /**
     * Highest numerical suffix associated to variables
     */
    private int highestVariableSuffix;

    /**
     * The number of operator calls required to produce this rule.
     */
    private int generation;

    /**
     * ****** End of Standard Metrics ************
     */
    /**
     * ****** AMIE+ and approximations ************
     */
    /**
     * Standard confidence theorethical upper bound for standard confidence
     *
     */
    private double stdConfidenceUpperBound;

    /**
     * PCA confidence theorethical upper bound for PCA confidence
     */
    private double pcaConfidenceUpperBound;

    /**
     * PCA confidence rough estimation for the hard cases
     */
    private double pcaConfidenceEstimation;

    /**
     * Time to run the denominator for the expression of the PCA confidence
     */
    private double _pcaConfidenceRunningTime;

    /**
     * Time to run the denominator for the expression of the standard confidence
     */
    private double _confidenceRunningTime;

    /**
     * ******** Joint Prediction *********
     */
    /**
     * This corresponds to all the fields associated to the project of
     * prediction using rules as multiples sources of evidence
     *
     */
    /**
     * A unique integer identifier for rules.
     */
    private int id;

    private boolean finalized = false;

    /**
     * A map that stores any other measure of interest on the rule
     */
    private Map<String, Double> otherMeasures;

    public void setMeasure(String name, Double value) {
        if (otherMeasures == null) {
            otherMeasures = new HashMap<>();
        }
        otherMeasures.put(name, value);
    }

    /**
     * Returns the value of a measure stored, null if no measure has this name.
     * @param name
     * @return
     */
    public Double getMeasure(String name) {
        Double r = otherMeasures.get(name);
        if (r == null) {
            return -1.0;
        }
        return r;
    }

    public void setFinal() {
    	finalized = true;
    }

    public boolean isFinal() {
    	return finalized;
    }

    /**
     * The regex pattern that defines variables generated by this class.
     */
    private static Pattern variablesRegex = Pattern.compile("\\?([a-z])([0-9]*)");

    /**
     * It puts the arguments in an array.
     *
     * @param sub
     * @param pred
     * @param obj
     * @return
     */
    public static int[] triple(int sub, int pred, int obj) {
        int[] newTriple = new int[3];
        newTriple[0] = sub;
        newTriple[1] = pred;
        newTriple[2] = obj;
        return newTriple;
    }

    public static boolean equal(int[] pattern1, int pattern2[]) {
        return pattern1[0] == pattern2[0]
                && pattern1[1] == pattern2[1]
                && pattern1[2] == pattern2[2];
    }

    /**
     * It creates a new unbound atom with fresh variables for the subject and object
     * and an undefined property, i.e., ?s[n] ?p ?o[n]. n is optional and is always greater
     * than 1.
     * @return
     */
    public int[] fullyUnboundTriplePattern() {
        return new int[] {newVariable(), KB.map("?p9"), newVariable()};
    }

    /** It creates a new unbound atom with fresh variables for the subject and object
    * and an undefined property, i.e., ?s[n] ?p ?o[n]. n is optional and is always greater
    * than 1.
    **/
    public static synchronized int[] fullyUnboundTriplePattern1() {
        int[] result = new int[3];
        ++varsCount;
        result[0] = KB.map("?s" + varsCount);
        result[1] = KB.map("?p" + varsCount);
        result[2] = KB.map("?o" + varsCount);
        return result;
    }

    public static boolean equals(int[] atom1, int[] atom2) {
        return (atom1[0] == atom2[0] &&
        		atom1[1] == atom2[1] &&
        		atom1[2] == atom2[2]);
    }

    /**
     * Instantiates an empty rule.
     */
    public Rule() {
        this.triples = new ArrayList<>();
        this.headKey = 0;
        this.support = -1;
        this.initialSupport = 0;
        this.parent = null;
        this.bodySize = -1;
        this.highestVariable = 0; // The character before letter 'a'
        this.highestVariableSuffix = 0;
        this.pcaBodySize = 0.0;
        this.stdConfidenceUpperBound = 0.0;
        this.pcaConfidenceUpperBound = 0.0;
        this.pcaConfidenceEstimation = 0.0;
        this.ancestors = new HashSet<>();
        this.generation = -1;
    }

    /**
     * Instantiates a rule of the form [] =&gt; r(?a, ?b) with empty body
     * and the given pattern as rule.
     * @param headAtom The head atom as an array of the form [?a, r, ?b].
     * @param cardinality
     */
    public Rule(int[] headAtom, double cardinality) {
        this.triples = new ArrayList<>();
        this.support = cardinality;
        this.initialSupport = (int) cardinality;
        this.parent = null;
        this.triples.add(headAtom.clone());
        this.functionalVariablePosition = 0;
        this.bodySize = 0;
        this.highestVariable = 0; // The character before letter 'a'
        this.highestVariableSuffix = 0;
        computeHeadKey();
        parseVariables();
        this.stdConfidenceUpperBound = 0.0;
        this.pcaConfidenceUpperBound = 0.0;
        this.pcaConfidenceEstimation = 0.0;
        this.ancestors = new HashSet<>();
        this.generation = -1;
    }

    /**
     * Creates a new query as a clone of the query sent as argument with the given
     * support.
     * @param otherQuery
     * @param support
     */
    public Rule(Rule otherQuery, double support) {
        this.triples = U.deepCloneInt(otherQuery.triples);
        this.support = support;
        this.initialSupport = (int) support;
        this.pcaBodySize = otherQuery.pcaBodySize;
        this.bodySize = otherQuery.bodySize;
        this.bodyMinusHeadSize = otherQuery.bodyMinusHeadSize;
        this.functionalVariablePosition = otherQuery.functionalVariablePosition;
        computeHeadKey();
        this.parent = null;
        this.bodySize = -1;
        this.highestVariable = otherQuery.highestVariable;
        this.highestVariableSuffix = otherQuery.highestVariableSuffix;
        this.stdConfidenceUpperBound = 0.0;
        this.pcaConfidenceUpperBound = 0.0;
        this.pcaConfidenceEstimation = 0.0;
        this.ancestors = new HashSet<>();
        this.generation = -1;
    }

    public Rule(int[] head, List<int[]> body, double cardinality) {
        triples = new ArrayList<int[]>();
        triples.add(head.clone());
        triples.addAll(amie.data.U.deepCloneInt(body));
        this.highestVariable = 0; // The character before letter 'a'
        this.highestVariableSuffix = 0;
        parseVariables();
        computeHeadKey();
        this.support = cardinality;
        this.initialSupport = (int) cardinality;
        this.functionalVariablePosition = 0;
        this.parent = null;
        this.bodySize = -1;
        this.stdConfidenceUpperBound = 0.0;
        this.pcaConfidenceUpperBound = 0.0;
        this.pcaConfidenceEstimation = 0.0;
        this.ancestors = new HashSet<>();
        this.generation = -1;
    }

    /**
     * It adjusts the rule so that new generated variables do not conflict
     * with the given atom.
     * @param headAtom
     */
    private void parseVariables() {
    	this.highestVariable = 0;
    	for (int[] atom : this.triples) {
	    	for (int particle : atom) {
	    		if (KB.isVariable(particle) && particle < this.highestVariable) {
	    			this.highestVariable = particle;
	    		}
	    	}
        }
    }

    /**
     * Get the two components of a variable as an array (letter and suffix)
     * @param var
     * @return
     */
    private String[] parseVariable(String var) {
    	Matcher matcher = variablesRegex.matcher(var);
    	boolean m = matcher.matches();
    	if (!m) return null;
    	MatchResult mr = matcher.toMatchResult();
		return new String[]{ mr.group(1), mr.group(2) };
    }

    /**
     * Like String.compareTo at the level of rule variables.
     * 1 = first is greater, 0 = they are equal, -1 = first is smaller
     * e.g., ?a < ?b, ?a0 < ?a1, ?x0 < ?s1
     * @param v1
     * @param v2
     * @return
     */
	private int compareVariables(int v1, int v2) {
		return Integer.compare(v2, v1);
	}

	/**
     * It returns a new fresh variable for the rule.
     * @return
     */
    private int newVariable() {
    	return --this.highestVariable;
    }


    /**
     * Calculate a simple hash key based on the constant arguments of the head
     * variables.
     */
    private void computeHeadKey() {
        headKey = triples.get(0)[1];
        if (!KB.isVariable(triples.get(0)[2])) {
            headKey ^= triples.get(0)[2];
        } else if (!KB.isVariable(triples.get(0)[0])) {
            headKey ^= triples.get(0)[0];
        }
    }

    /**
     * Return the list of triples of the query. Modifications to this
     * list alter the query.
     * @return
     */
    public List<int[]> getTriples() {
        return triples;
    }

    public List<int[]> getTriplesCopy() {
    	return U.deepCloneInt(getTriples());
    }

    /**
     * Returns the triples of a query except for those containing DIFFERENTFROM
     * constraints. Modifications to this list do not alter the query. However, modifications
     * to the atoms do alter the query.
     * @return
     */
    public List<int[]> getTriplesWithoutSpecialRelations() {
        List<int[]> resultList = new ArrayList<>();
        for (int[] triple : triples) {
            if (triple[1] != KB.DIFFERENTFROMbs) {
                resultList.add(triple);
            }
        }

        return resultList;
    }

    /**
     * Returns the head of a query B =&gt; r(a, b) as a triple [?a, r, ?b].
     * @return
     */
    public int[] getHead() {
        return triples.get(0);
    }

    /**
     * Returns the head of a query B =&gt; r(a, b) as a triple [?a, r, ?b].
     * Alias for the method getHead().
     * @return
     */
    public int[] getSuccedent() {
        return triples.get(0);
    }

    /**
     * Returns the list of triples in the body of the rule.
     * @return Non-modifiable list of atoms.
     */
    public List<int[]> getBody() {
        return triples.subList(1, triples.size());
    }

    /**
     * Returns the list of triples in the body of the rule. It is an alias
     * for the method getBody()
     * @return Non-modifiable list of atoms.
     */
    public List<int[]> getAntecedent() {
        return triples.subList(1, triples.size());
    }

    /**
     * Returns a list with copies of the triples of the rule.
     * Modifications to either the list or the atoms do not alter the rule.
     *
     * @return
     */
    public List<int[]> getAntecedentClone() {
        List<int[]> cloneList = new ArrayList<>();
        for (int[] triple : getAntecedent()) {
            cloneList.add(triple.clone());
        }

        return cloneList;
    }


    protected void setTriples(ArrayList<int[]> triples) {
        this.triples = triples;
    }

    /**
     * @return the mustBindVariables
     */
    public IntList getOpenVariables() {
        Int2IntMap histogram = variablesHistogram(true);
        IntList variables = new IntArrayList();
        for (int var : histogram.keySet()) {
            if (histogram.get(var) < 2 && KB.isOpenableVariable(var)) {
                variables.add(var);
            }
        }

        return variables;
    }

    public double getHeadCoverage() {
        return headCoverage;
    }

    public void setHeadCoverage(double headCoverage) {
        this.headCoverage = headCoverage;
    }

    /**
     * @return the support
     */
    public double getSupportRatio() {
        return supportRatio;
    }

    /**
     * @param support the support to set
     */
    public void setSupportRatio(double support) {
        this.supportRatio = support;
    }

    /**
     * @return the headBodyCount
     */
    public double getSupport() {
        return support;
    }

    /**
     * The cardinality number used to hash the rule.
     *
     * @return
     */
    public long getHashCardinality() {
        return initialSupport;
    }

    /**
     * @param cardinality the headBodyCount to set
     */
    public void setSupport(double cardinality) {
        this.support = cardinality;
    }

    /**
     * The support of the body of the rule. If the rule has the form B =&gt; r(x,y)
     * then the body size is support(B).
     *
     * @return
     */
    public long getBodySize() {
        return bodySize;
    }

    /**
     *
     * @param bodySize
     */
    public void setBodySize(long bodySize) {
        this.bodySize = bodySize;
    }

    /**
     * @return the confidence
     */
    public double getStdConfidence() {
        return (double) support / bodySize;
    }

    /**
     * @return the pcaConfidence
     */
    public double getPcaConfidence() {
        return support / pcaBodySize;
    }

    public double getConfidenceRunningTime() {
        return _confidenceRunningTime;
    }

    public void setConfidenceRunningTime(double confidenceRunningTime) {
        this._confidenceRunningTime = confidenceRunningTime;
    }

    public double getPcaConfidenceRunningTime() {
        return _pcaConfidenceRunningTime;
    }

    public void setPcaConfidenceRunningTime(double pcaConfidenceRunningTime) {
        this._pcaConfidenceRunningTime = pcaConfidenceRunningTime;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = generation;
	}

	/**
     * Returns the last triple pattern added to this rule.
     *
     * @return
     */
    public int[] getLastTriplePattern() {
        if (triples.isEmpty()) {
            return null;
        } else {
            return triples.get(triples.size() - 1);
        }
    }

    /**
     * Return the last triple pattern which is not the a pseudo-atom.
     *
     * @return
     */
    public int[] getLastRealTriplePattern() {
        int index = getLastRealTriplePatternIndex();
        if (index == -1) {
            return null;
        } else {
            return triples.get(index);
        }
    }

    /**
     * Return the index of the last triple pattern which is not the a
     * pseudo-atom.
     *
     * @return
     */
    public int getLastRealTriplePatternIndex() {
        if (triples.isEmpty()) {
            return -1;
        } else {
            int index = triples.size() - 1;
            int[] last = null;
            while (index >= 0) {
                last = triples.get(index);
                if (last[1] != KB.DIFFERENTFROMbs) {
                    break;
                }
                --index;
            }

            return index;
        }
    }

    /**
     * Return the index of the last triple pattern which is not the a
     * type.
     *
     * @return
     */
    public int getLastNotTypeTriplePatternIndex() {
        if (triples.isEmpty()) {
            return -1;
        } else {
            int index = triples.size() - 1;
            int[] last = null;
            while (index >= 0) {
                last = triples.get(index);
                if (last[1] != Schema.typeRelationBS) {
                    break;
                }
                --index;
            }

            return index;
        }
    }

    public Pair<Integer, Integer> getLastCoordinatesOf(int bs) {
    	if (triples.isEmpty()) {
            return null;
        } else {
            int index = triples.size() - 1;
            int[] last = null;
            while (index >= 0) {
                last = triples.get(index);
                if (last[0] == bs)
                    return new Pair<Integer,Integer>(index, 0);
                if (last[1] == bs)
                	return new Pair<Integer,Integer>(index, 1);
                if (last[2] == bs)
                    return new Pair<Integer,Integer>(index, 2);
                --index;
            }

            return null;
        }
    }

    /**
     * Returns the number of times the relation occurs in the atoms of the query
     *
     * @return
     */
    public int cardinalityForRelation(int relation) {
        int count = 0;
        for (int[] triple : triples) {
            if (triple[1] == relation) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Returns true if the triple pattern contains constants in all its
     * positions
     *
     * @param pattern
     * @return
     */
    public static boolean isGroundAtom(int[] pattern) {
        // TODO Auto-generated method stub
        return !KB.isVariable(pattern[0])
                && !KB.isVariable(pattern[1])
                && !KB.isVariable(pattern[2]);
    }

    /**
     * Look for the redundant atoms with respect to a reference atom
     * @param withRespectToIdx The index of the reference atom
     * @return
     */
    public List<int[]> getRedundantAtoms(int withRespectToIdx) {
        int[] newAtom = triples.get(withRespectToIdx);
        List<int[]> redundantAtoms = new ArrayList<int[]>();
        for (int[] pattern : triples) {
            if (pattern != newAtom) {
                if (isUnifiable(pattern, newAtom)
                        || isUnifiable(newAtom, pattern)) {
                    redundantAtoms.add(pattern);
                }
            }
        }

        return redundantAtoms;
    }

    /**
     * Checks whether the last atom in the query is redundant.
     * @return
     */
    public List<int[]> getRedundantAtoms() {
        int[] newAtom = getLastTriplePattern();
        List<int[]> redundantAtoms = new ArrayList<int[]>();
        for (int[] pattern : triples) {
            if (pattern != newAtom) {
                if (isUnifiable(pattern, newAtom) || isUnifiable(newAtom, pattern)) {
                    redundantAtoms.add(pattern);
                }
            }
        }

        return redundantAtoms;
    }

    public int getFunctionalVariable() {
        return getHead()[functionalVariablePosition];
    }

    public int getNonFunctionalVariable() {
        return triples.get(0)[getNonFunctionalVariablePosition()];
    }

    public int getFunctionalVariablePosition() {
        return functionalVariablePosition;
    }

    public void setFunctionalVariablePosition(int functionalVariablePosition) {
        this.functionalVariablePosition = functionalVariablePosition;
    }

    public int getNonFunctionalVariablePosition() {
        return functionalVariablePosition == 0 ? 2 : 0;
    }

    /**
     * Determines if the second argument is unifiable to the first one.
     * Unifiable means there is a valid unification mapping (variable -&gt;
     * variable, variable -&gt; constant) between the components of the triple
     * pattern
     *
     * @param pattern
     * @param newAtom
     * @return boolean
     */
    public static boolean isUnifiable(int[] pattern, int[] newAtom) {
        // TODO Auto-generated method stub
        boolean unifiesSubject =(pattern[0] == newAtom[0])|| KB.isVariable(pattern[0]);
        if (!unifiesSubject) {
            return false;
        }

        boolean unifiesPredicate =(pattern[1] == newAtom[1])|| KB.isVariable(pattern[1]);
        if (!unifiesPredicate) {
            return false;
        }

        boolean unifiesObject =(pattern[2] == newAtom[2])|| KB.isVariable(pattern[2]);
        if (!unifiesObject) {
            return false;
        }

        return true;
    }


    /**
     * It returns true if the atom contains the a term (variable or constant)
     * more than once.
     * @param literal
     * @return
     */
	public static boolean isReflexive(int[] atom) {
		// TODO Auto-generated method stub
		return(atom[0] == atom[1] || atom[1] == atom[2]
				|| atom[2] == atom[0]);
	}

    public static boolean areEquivalent(int[] pattern, int[] newAtom) {
        boolean unifiesSubject =(pattern[0] == newAtom[0])
                || (KB.isVariable(pattern[0]) && KB.isVariable(newAtom[0]));
        if (!unifiesSubject) {
            return false;
        }

        boolean unifiesPredicate =(pattern[1] == newAtom[1])
                || (KB.isVariable(pattern[1]) && KB.isVariable(newAtom[1]));
        if (!unifiesPredicate) {
            return false;
        }

        boolean unifiesObject =(pattern[2] == newAtom[2])
                || (KB.isVariable(pattern[2]) && KB.isVariable(newAtom[2]));
        if (!unifiesObject) {
            return false;
        }

        return true;
    }

    /**
     * Determines if the first argument is unifiable to at least one atom in the second argument.
     * Unifiable means there is a valid unification mapping (variable -&gt;
     * variable, variable -&gt; constant) between the components of the triple
     * pattern
     *
     * @param test
     * @param query
     * @return boolean
     */
    public static boolean unifies(int[] test, List<int[]> query) {
        for (int[] pattern : query) {
            if (isUnifiable(pattern, test)) {
                return true;
            }
        }
        return false;
    }

    /**
     * It returns a list with all the redundant atoms contained in the first
     * list, i.e., atoms whose removal does not affect the results of the query
     * defined in the second list.
     *
     * @param test
     * @param query
     * @return
     */
    public static List<int[]> redundantAtoms(int[] test, List<int[]> query) {
        List<int[]> redundantAtoms = new ArrayList<int[]>();
        for (int[] pattern : query) {
            if (isUnifiable(pattern, test)) {
                redundantAtoms.add(pattern);
            }

        }

        return redundantAtoms;
    }

    /**
     * Determines whether the last atom of the query.
     *
     * @return boolean
     */
    public boolean containsUnifiablePatterns() {
        int nPatterns = triples.size();
        for (int i = 0; i < nPatterns; ++i) {
            for (int j = i + 1; j < nPatterns; ++j) {
                if (isUnifiable(triples.get(j), triples.get(i))
                        || isUnifiable(triples.get(i), triples.get(j))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Simple string representation of the rule. Check the methods
     * getRuleString, getFullRuleString and getBasicRuleString.
     */
    public String toString() {
        return getRuleString();
    }

    /**
     * Returns a list with all the different variables in the query.
     *
     * @return
     */
    public IntList getVariables() {
        IntList variables = new IntArrayList();
        for (int[] triple : triples) {
            if (KB.isVariable(triple[0])) {
                if (!variables.contains(triple[0])) {
                    variables.add(triple[0]);
                }
            }

            if (KB.isVariable(triple[2])) {
                if (!variables.contains(triple[2])) {
                    variables.add(triple[2]);
                }
            }
        }

        return variables;
    }

    public IntList getOpenableVariables() {
        IntList variables = new IntArrayList();
        for (int[] triple : triples) {
            if (KB.isOpenableVariable(triple[0])) {
                if (!variables.contains(triple[0])) {
                    variables.add(triple[0]);
                }
            }

            if (KB.isOpenableVariable(triple[2])) {
                if (!variables.contains(triple[2])) {
                    variables.add(triple[2]);
                }
            }
        }

        return variables;
    }

    /**
     * It returns the variables of an atom as a collection.
     * @param atom
     * @return
     */
    public static IntCollection getVariables(int[] atom) {
    	IntCollection result = new IntArrayList(4);
    	for (int i = 0; i < atom.length; ++i) {
    		if (KB.isVariable(atom[i])) {
    			result.add(atom[i]);
    		}
    	}

    	return result;
    }

    /**
     * Determines if a pattern contains repeated components, which are
     * considered hard to satisfy (i.e., ?x somePredicate ?x)
     *
     * @return boolean
     */
    public boolean containsRepeatedVariablesInLastPattern() {
        // TODO Auto-generated method stub
        int[] triple = getLastTriplePattern();
        return(triple[0] == triple[1])||
       (		triple[0] == triple[2])||
        		(triple[1] == triple[2]);
    }

    /**
     * Returns true if the rule contains redundant recursive atoms, i.e., atoms
     * with a relation that occurs more than once AND that do not have any
     * effect on the query result.
     *
     * @return
     */
    public boolean isRedundantRecursive() {
        List<int[]> redundantAtoms = getRedundantAtoms();
        int[] lastPattern = getLastTriplePattern();
        for (int[] redundantAtom : redundantAtoms) {
            if (equals(lastPattern, redundantAtom)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return boolean True if the rule has atoms.
     */
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return triples.isEmpty();
    }

    /**
     * Returns a histogram with the number of different atoms variables occur in.
     * @param ignoreSpecialAtoms discards pseudo-atoms containing the keyword DIFFERENTFROM.
     * @return
     */
    private Int2IntMap variablesHistogram(boolean ignoreSpecialAtoms) {
        Int2IntMap varsHistogram = new Int2IntOpenHashMap();
        for (int triple[] : triples) {
            if (triple[1] == KB.DIFFERENTFROMbs
            		&& ignoreSpecialAtoms) {
                continue;
            }

            if (KB.isVariable(triple[0])) {
                increase(varsHistogram, triple[0]);
            }
            // Do not count twice if a variable occurs twice in the atom, e.g., r(x, x)
            if (triple[0] != triple[2]) {
                if (KB.isVariable(triple[2])) {
                    increase(varsHistogram, triple[2]);
                }
            }
        }

        return varsHistogram;
    }

    /**
     *
     * @return
     */
    private Int2IntMap alternativeHistogram() {
        Int2IntMap hist = new Int2IntOpenHashMap(triples.size(), 1.0f);
        for (int i = 1; i < triples.size(); ++i) {
            int[] triple = triples.get(i);
            if (triple[1] == KB.DIFFERENTFROMbs) {
                continue;
            }

            if (KB.isVariable(triple[0])) {
                increase(hist, triple[0]);
            }
            // Do not count twice if a variable occurs twice in the atom, e.g., r(x, x)
            if (triple[0] != triple[2]) {
                if (KB.isVariable(triple[2])) {
                    increase(hist, triple[2]);
                }
            }
        }

        return hist;
    }

    /**
     *
     * @param ignoreSpecialAtoms If true, atoms special atoms such as DIFFERENTFROM are ignored
     * in the count.
     * @return boolean True if the rule is closed, i.e., each variable in the
     * rule occurs at least in two atoms.
     *
     */
    public boolean isClosed(boolean ignoreSpecialAtoms) {
        if (triples.isEmpty()) {
            return false;
        }

        Int2IntMap varsHistogram = variablesHistogram(ignoreSpecialAtoms);

        for (int variable : varsHistogram.keySet()) {
            if (varsHistogram.get(variable) < 2) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return boolean. True if the rule has PCA confidence 1.0
     */
    public boolean isPerfect() {
        return getPcaConfidence() == 1.0;
    }

    /**
     * Return a key for the rule based on the constant arguments of the head
     * atom. It can be used as a hash key.
     *
     * @return
     */
    public int getHeadKey() {
        if (headKey == 0) {
            computeHeadKey();
        }

        return headKey;
    }

    /**
     * Returns the rule's head relation as a String
     *
     * @return
     */
    public String getHeadRelation() {
        return KB.unmap(triples.get(0)[1]);
    }

    /**
     * Returns the rule's head relation as Integer.
     *
     * @return
     */
    public int getHeadRelationBS() {
        return triples.get(0)[1];
    }

    /**
     * Returns the number of atoms of the rule.
     *
     * @return
     */
    public int getLength() {
        return triples.size();
    }

    /**
     * Returns the number of atoms of the rule that are not pseudo-atoms
     * Pseudo-atoms contain the Database keywords "DIFFERENTFROM" and "EQUALS"
     *
     * @return
     */
    public int getRealLength() {
        int length = 0;
        for (int[] triple : triples) {
            if (!KB.specialRelations.contains(triple[1])) {
                ++length;
            }
        }

        return length;
    }

    /**
     * Returns the number of atoms of the rule that are neither pseudo-atoms nor
     * type constraints. Pseudo-atoms contain the Database keywords
     * "DIFFERENTFROM"
     *
     * @return
     */
    public int getRealLengthWithoutTypes(int typeString) {
        int length = 0;
        for (int[] triple : triples) {
            if (triple[1] != KB.DIFFERENTFROMbs
                    && (triple[1] != typeString
                    		|| KB.isVariable(triple[2]))) {
                ++length;
            }
        }
        return length;
    }

    /**
     * Returns the number of atoms of the rule that are not type constraints of
     * the form rdf:type(?x, C) where C is a class, i.e., Person.
     *
     * @param typeString
     * @return
     */
    public int getLengthWithoutTypes(int typeString) {
        int size = 0;
        for (int[] triple : triples) {
            if (triple[1] != typeString
                    || KB.isVariable(triple[2])) {
                ++size;
            }
        }

        return size;
    }

    /**
     * Returns the number of atoms of the rule that are neither type constraints
     * of the form rdf:type(?x, C) or linksTo atoms.
     *
     * @param typeString
     * @return
     */
    public int getLengthWithoutTypesAndLinksTo(int typeString, int linksString) {
        int size = 0;
        for (int[] triple : triples) {
            if ((triple[1] != typeString
            		|| KB.isVariable(triple[2]))
                    && triple[1] != linksString) {
                ++size;
            }
        }

        return size;
    }

    /**
     * Adds a parent to the rule.
     *
     * @param parent
     */
    public void addParent(Rule parent) {
        this.ancestors.add(parent);
    }

    public boolean containsParent(Rule parent) {
    	return this.ancestors.contains(parent);
    }

    /**
     * Returns a new rule that contains all the atoms of the current rule plus
     * the atom provided as argument.
     *
     * @param newAtom The new atom.
     * @param cardinality The support of the new rule.
     * @param joinedVariable The position of the common variable w.r.t to the
     * rule in the new atom, i.e., 0 if the new atoms joins on the subject or 2
     * if it joins on the object.
     * @param danglingVariable The position of the fresh variable in the new
     * atom.
     * @return
     */
    public Rule addAtom(int[] newAtom,
            double cardinality, int joinedVariable, int danglingVariable) {
        Rule newQuery = new Rule(this, cardinality);
        int[] copyNewEdge = newAtom.clone();
        newQuery.triples.add(copyNewEdge);
        return newQuery;
    }

    public Rule addAtoms(int[] atom1, int[] atom2, double cardinality) {
		Rule newQuery = new Rule(this, cardinality);
		newQuery.triples.add(atom1.clone());
		newQuery.triples.add(atom2.clone());
		return newQuery;
	}

    public Rule addAtoms(int[] atom1, int[] atom2, int[] atom3, double cardinality) {
		Rule newQuery = new Rule(this, cardinality);
		newQuery.triples.add(atom1.clone());
		newQuery.triples.add(atom2.clone());
		newQuery.triples.add(atom3.clone());
		return newQuery;
	}

    public Rule addAtom(int[] newAtom, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        int[] copyNewEdge = newAtom.clone();
        newQuery.triples.add(copyNewEdge);
        return newQuery;
    }

    /**
     * Constructs a new rule identical to the calling one except that
     * the last atom is replaced by the argument.
     * @param newAtom
     * @return
     */
	public Rule replaceLastAtom(int[] newAtom, double cardinality) {
		Rule newRule = new Rule(this, cardinality);
		int ruleSize = newRule.getLength();
		newRule.getTriples().set(ruleSize - 1, newAtom.clone());
		return newRule;
	}

    /**
     * It returns a new rule where the last atom is replaced by a
     * type constraint with the given subtype.
     * @param subtype
     * @param cardinality
     * @return
     */
    public Rule specializeTypeAtom(int subtype, double cardinality) {
		Rule newRule = new Rule(this.getHead(), this.getBody(), cardinality);
		int[] lastTriple = newRule.getLastTriplePattern();
		lastTriple[1] = Schema.typeRelationBS;
		lastTriple[2] = subtype;
		newRule.setFunctionalVariablePosition(functionalVariablePosition);
		return newRule;
	}

    /**
     * The alternative hash code for the parents of the rule. The alternative
     * hash code if a small variant of the hashCode method that does not use the
     * support of the rule.
     *
     * @return
     */
    public int alternativeParentHashCode() {
    	int hk = getHeadKey();
        return headAndGenerationHashCode(hk, generation);
    }

    /**
     * Returns a hash code that depends on the given arguments.
     * @param headKey
     * @param generation
     * @return
     */
    public static int headAndGenerationHashCode(int headKey, int generation) {
        final int prime = 31;
        int result = 1;
        result = prime * result + headKey;
        result = prime * result + generation;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        if (generation > 0) {
	        result = prime * result + (int) initialSupport;
	        result = prime * result + (int) generation;
	        result = prime * result + headKey;
        } else {
	        result = prime * result + (int) initialSupport;
	        result = prime * result + (int) getRealLength();
	        result = prime * result + headKey;
       }
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Rule other = (Rule) obj;
        if (getLength() != other.getLength()) {
            return false;
        }

        if (getHeadKey() == 0) {
            if (other.getHeadKey() != 0) {
                return false;
            }
        } else if (getHeadKey() != other.getHeadKey()) {
            return false;
        }

        if (initialSupport != other.initialSupport) {
        	return false;
        }

        if (((long)support) != ((long)other.support)) {
            return false;
        }
        return QueryEquivalenceChecker3.areEquivalent(getTriples(), other.getTriples());
    }

    public String getRuleString() {
        StringBuilder strBuilder = new StringBuilder();
        for (int[] pattern : sortBody()) {
            if (pattern[1] == KB.DIFFERENTFROMbs) {
            	strBuilder.append(KB.unmap(pattern[0]));
                strBuilder.append("!=");
                strBuilder.append(KB.unmap(pattern[2]));
                strBuilder.append(" ");
                continue;
            }
            strBuilder.append(KB.unmap(pattern[0]));
            strBuilder.append("  ");
            strBuilder.append(KB.unmap(pattern[1]));
            strBuilder.append("  ");
            strBuilder.append(KB.unmap(pattern[2]));
            strBuilder.append("  ");
        }

        strBuilder.append(" => ");
        int[] head = triples.get(0);
        strBuilder.append(KB.unmap(head[0]));
        strBuilder.append("  ");
        strBuilder.append(KB.unmap(head[1]));
        strBuilder.append("  ");
        strBuilder.append(KB.unmap(head[2]));

        return strBuilder.toString();
    }

    public String getRuleRawString() {
        StringBuilder strBuilder = new StringBuilder();
        for (int[] pattern : getBody()) {
            strBuilder.append(KB.unmap(pattern[0]));
            strBuilder.append("  ");
            strBuilder.append(KB.unmap(pattern[1]));
            strBuilder.append("  ");
            strBuilder.append(KB.unmap(pattern[2]));
            strBuilder.append("  ");
        }

        strBuilder.append(" => ");
        int[] head = triples.get(0);
        strBuilder.append(KB.unmap(head[0]));
        strBuilder.append("  ");
        strBuilder.append(KB.unmap(head[1]));
        strBuilder.append("  ");
        strBuilder.append(KB.unmap(head[2]));

        return strBuilder.toString();
    }

    public Collection<int[]> sortBody() {
    	   //Guarantee that atoms in rules are output in the same order across runs of the program
        class TripleComparator implements Comparator<int[]> {

            public int compare(int[] t1, int[] t2) {
                int predicateCompare = KB.unmap(t1[1]).compareTo(KB.unmap(t2[1]));
                if (predicateCompare == 0) {
                    int objectCompare = KB.unmap(t1[2]).compareTo(KB.unmap(t2[2]));
                    if (objectCompare == 0) {
                        return KB.unmap(t1[0]).compareTo(KB.unmap(t2[0]));
                    }
                    return objectCompare;
                }
                return predicateCompare;
            }
        }

        TreeSet<int[]> sortedBody = new TreeSet<int[]>(new TripleComparator());
        sortedBody.addAll(getAntecedent());
        return sortedBody;
    }

    public String getDatalogRuleString(Metric... metrics2Ommit) {
        StringBuilder strBuilder = new StringBuilder();
        for (int[] pattern : sortBody()) {
            if (pattern[1] == KB.DIFFERENTFROMbs) {
                strBuilder.append(KB.unmap(pattern[0]));
                strBuilder.append("!=");
                strBuilder.append(KB.unmap(pattern[2]));
                strBuilder.append(" ");
                continue;
            }
            strBuilder.append(KB.unmap(pattern[1]));
            strBuilder.append("(");
            strBuilder.append(KB.unmap(pattern[0]));
            strBuilder.append(",");
            strBuilder.append(KB.unmap(pattern[2]));
            strBuilder.append(") ");
        }

        strBuilder.append(" => ");
        int[] head = triples.get(0);
        strBuilder.append(KB.unmap(head[1]));
        strBuilder.append("(");
        strBuilder.append(KB.unmap(head[0]));
        strBuilder.append(",");
        strBuilder.append(KB.unmap(head[2]));
        strBuilder.append(")");

        return strBuilder.toString();
    }

    public String getDatalogBasicRuleString(Metric... metrics2Ommit) {
    	StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getDatalogRuleString(metrics2Ommit));
        addBasicFields(strBuilder);
        return strBuilder.toString();
	}


    private void addFullFields(StringBuilder strBuilder, Metric... metrics2Ommit) {
    	DecimalFormat df = new DecimalFormat("#.#########");
        DecimalFormat df1 = new DecimalFormat("#.##");
        if (metrics2Ommit.length == 0) {
	        strBuilder.append("\t" + df.format(getHeadCoverage()));
	        strBuilder.append("\t" + df.format(getStdConfidence()));
	        strBuilder.append("\t" + df.format(getPcaConfidence()));
	        strBuilder.append("\t" + df1.format(getSupport()));
	        strBuilder.append("\t" + df1.format(getBodySize()));
	        strBuilder.append("\t" + df1.format(getPcaBodySize()));
	        strBuilder.append("\t" + KB.unmap(getFunctionalVariable()));
	        strBuilder.append("\t" + stdConfidenceUpperBound);
	        strBuilder.append("\t" + pcaConfidenceUpperBound);
	        strBuilder.append("\t" + pcaConfidenceEstimation);
        } else {
        	List<Metric> metricsList = Arrays.asList(metrics2Ommit);
        	if (!metricsList.contains(Metric.HeadCoverage))
        		strBuilder.append("\t" + df.format(getHeadCoverage()));
	        if (!metricsList.contains(Metric.StandardConfidence))
	        	strBuilder.append("\t" + df.format(getStdConfidence()));
	        if (!metricsList.contains(Metric.PCAConfidence))
	        	strBuilder.append("\t" + df.format(getPcaConfidence()));
	        if (!metricsList.contains(Metric.Support))
	        	strBuilder.append("\t" + df1.format(getSupport()));
	        if (!metricsList.contains(Metric.BodySize))
	        	strBuilder.append("\t" + df1.format(getBodySize()));
	        if (!metricsList.contains(Metric.PCABodySize))
	        	strBuilder.append("\t" + df1.format(getPcaBodySize()));
	        strBuilder.append("\t" + KB.unmap(getFunctionalVariable()));
	        strBuilder.append("\t" + stdConfidenceUpperBound);
	        strBuilder.append("\t" + pcaConfidenceUpperBound);
	        strBuilder.append("\t" + pcaConfidenceEstimation);
        }
    }

    private void addBasicFields(StringBuilder strBuilder, Metric... metrics2Ommit) {
    	DecimalFormat df = new DecimalFormat("#.#########");
    	if (metrics2Ommit.length == 0) {
	        strBuilder.append("\t" + df.format(getHeadCoverage()));
	        strBuilder.append("\t" + df.format(getStdConfidence()));
	        strBuilder.append("\t" + df.format(getPcaConfidence()));
	        strBuilder.append("\t" + df.format(getSupport()));
	        strBuilder.append("\t" + getBodySize());
	        strBuilder.append("\t" + df.format(getPcaBodySize()));
	        strBuilder.append("\t" + KB.unmap(getFunctionalVariable()));
    	} else {
        	List<Metric> metricsList = Arrays.asList(metrics2Ommit);
        	if (!metricsList.contains(Metric.HeadCoverage))
        		strBuilder.append("\t" + df.format(getHeadCoverage()));
	        if (!metricsList.contains(Metric.StandardConfidence))
	        	strBuilder.append("\t" + df.format(getStdConfidence()));
	        if (!metricsList.contains(Metric.PCAConfidence))
	        	strBuilder.append("\t" + df.format(getPcaConfidence()));
	        if (!metricsList.contains(Metric.Support))
	        	strBuilder.append("\t" + df.format(getSupport()));
	        if (!metricsList.contains(Metric.BodySize))
	        	strBuilder.append("\t" + getBodySize());
	        if (!metricsList.contains(Metric.PCABodySize))
	        	strBuilder.append("\t" + df.format(getPcaBodySize()));
	        strBuilder.append("\t" + KB.unmap(getFunctionalVariable()));
    	}
    }

    public String getDatalogFullRuleString(Metric... metrics2Ommit) {
    	StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getDatalogRuleString(metrics2Ommit));
        addFullFields(strBuilder, metrics2Ommit);
        return strBuilder.toString();
    }

    public String getFullRuleString(Metric... metrics2Ommit) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getRuleString());
        addFullFields(strBuilder, metrics2Ommit);
        return strBuilder.toString();
    }

    public String getBasicRuleString(Metric... metrics2Ommit) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(getRuleString());
        addBasicFields(strBuilder, metrics2Ommit);
        return strBuilder.toString();
    }

    public static String toDatalog(int[] atom) {
        return KB.unmap(atom[1]).replace("<", "").replace(">", "")
                + "(" + KB.unmap(atom[0]) + ", " + KB.unmap(atom[2]) + ")";
    }

    public String getDatalogString() {
        StringBuilder builder = new StringBuilder();

        builder.append(Rule.toDatalog(getHead()));
        builder.append(" <=");
        for (int[] atom : getBody()) {
            builder.append(" ");
            builder.append(Rule.toDatalog(atom));
            builder.append(",");
        }

        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    /**
     * Returns a new query where the variable at the dangling position of the
     * last atom has been unified to the provided constant.
     *
     * @param danglingPosition
     * @param constant
     * @param cardinality
     * @return
     */
    public Rule instantiateConstant(int danglingPosition, int constant, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        int[] lastNewPattern = newQuery.getLastTriplePattern();
        lastNewPattern[danglingPosition] = constant;
        newQuery.computeHeadKey();
        return newQuery;
    }

    /**
     * Return a new query where the variable at position danglingPosition in
     * triple at position triplePos is bound to constant.
     *
     * @param triplePos
     * @param danglingPosition
     * @param constant
     * @param cardinality
     * @return
     */
    public Rule instantiateConstant(int triplePos, int danglingPosition, int constant, double cardinality) {
        Rule newQuery = new Rule(this, cardinality);
        int[] targetEdge = newQuery.getTriples().get(triplePos);
        targetEdge[danglingPosition] = constant;
        newQuery.cleanInequalityConstraints();
        return newQuery;
    }

    private void cleanInequalityConstraints() {
        List<int[]> toRemove = new ArrayList<>();
        Int2IntMap varHistogram = variablesHistogram(true);
        for (int[] triple : triples) {
            if (triple[1] == KB.DIFFERENTFROMbs) {
                int varPos = KB.firstVariablePos(triple);
                // Check if the variable became orphan
                if (!varHistogram.containsKey(triple[varPos])) {
                    toRemove.add(triple);
                }
            }
        }

        triples.removeAll(toRemove);
    }

    public Set<Rule> getAncestors() {
        return ancestors;
    }

    /**
     * It gathers ancestors in a recursive fashion
     * @param q
     * @param output
     */
    private void gatherAncestors(Rule q, Set<Rule> output) {
        if (q.ancestors == null
                || q.ancestors.isEmpty()) {
            return;
        } else {
            // Let's do depth search
            for (Rule ancestor : q.ancestors) {
                output.add(ancestor);
                gatherAncestors(ancestor, output);
            }
        }
    }

    public List<Rule> getAllAncestors() {
        Set<Rule> output = new LinkedHashSet<>();
        for (Rule ancestor : ancestors) {
            output.add(ancestor);
            gatherAncestors(ancestor, output);
        }
        return new ArrayList<>(output);
    }

    public void setBodyMinusHeadSize(int size) {
        bodyMinusHeadSize = size;
    }

    public long getBodyMinusHeadSize() {
        return bodyMinusHeadSize;
    }

    public void setPcaBodySize(double size) {
        pcaBodySize = size;
    }

    public double getPcaBodySize() {
        return pcaBodySize;
    }

    public Rule rewriteQuery(int[] remove, int[] target, int victimVar, int targetVar) {
        List<int[]> newTriples = new ArrayList<int[]>();
        for (int[] t : triples) {
            if (t != remove) {
                int[] clone = t.clone();
                for (int i = 0; i < clone.length; ++i) {
                    if (clone[i] == victimVar) {
                        clone[i] = targetVar;
                    }
                }

                newTriples.add(clone);
            }
        }

        Rule result = new Rule();
        //If the removal triple is the head, make sure the target is the new head
        if (remove == triples.get(0)) {
            for (int i = 0; i < newTriples.size(); ++i) {
                if (Arrays.equals(target, newTriples.get(i))) {
                    int tmp[] = newTriples.get(0);
                    newTriples.set(0, newTriples.get(i));
                    newTriples.set(i, tmp);
                }
            }
        }

        result.triples.addAll(newTriples);

        return result;
    }

    private static boolean addToVariableMap(int[] atom, Int2ObjectMap<IntSet> s) {
        if (!KB.isVariable(atom[0]) || !KB.isVariable(atom[2])) {
            return false;
        }
        IntSet r = s.get(atom[0]);
        if (r == null) { s.put(atom[0], r = new IntOpenHashSet()); }
        r.add(atom[2]);
        r = s.get(atom[2]);
        if (r == null) { s.put(atom[2], r = new IntOpenHashSet()); }
        r.add(atom[0]);
        return true;
    }

    private List<int[]> injectiveOf(List<int[]> antecedent) {
        List<int[]> body = new ArrayList<>(antecedent);
        int[] head = getHead();
        Int2ObjectMap<IntSet> connectedVariables = new Int2ObjectOpenHashMap<>();
        if (head[1] == KB.EQUALSbs) {
            addToVariableMap(head, connectedVariables);
        } else {
            for (int i = 0; i < 3; i+= 2) {
                if (KB.isVariable(head[i])) {
                    connectedVariables.put(head[i], new IntOpenHashSet());
                }
            }
        }
        for (int[] atom : body) {
            addToVariableMap(atom, connectedVariables);
        }
        for (int v : connectedVariables.keySet()) {
            for(int ov : connectedVariables.keySet()) {
                if (v > ov && !connectedVariables.get(v).contains(ov)) {
                    body.add(KB.triple(v, KB.DIFFERENTFROMbs, ov));
                }
            }
        }
        return body;
    }

    public List<int[]> injectiveBody() {
        return injectiveOf(getAntecedent());
    }

    public List<int[]> injectiveTriples() {
        return injectiveOf(getTriples());
    }

    public boolean variableCanBeDeleted(int triplePos, int varPos) {
        int variable = triples.get(triplePos)[varPos];
        for (int i = 0; i < triples.size(); ++i) {
            if (i != triplePos) {
                if (KB.varpos(variable, triples.get(i)) != -1) {
                    return false;
                }
            }
        }
        //The variable does not appear anywhere else (no constraints)
        return true;
    }

    public static int findFunctionalVariable(Rule q, KB d) {
        int[] head = q.getHead();
        if (KB.numVariables(head) == 1) {
            return KB.firstVariablePos(head);
        }
        return d.functionality(head[1]) > d.inverseFunctionality(head[1]) ? 0 : 2;
    }

    public void setConfidenceUpperBound(double stdConfUpperBound) {
        this.stdConfidenceUpperBound = stdConfUpperBound;
    }

    public void setPcaConfidenceUpperBound(double pcaConfUpperBound) {
        // TODO Auto-generated method stub
        this.pcaConfidenceUpperBound = pcaConfUpperBound;
    }

    /**
     * @return the pcaEstimation
     */
    public double getPcaEstimation() {
        return pcaConfidenceEstimation;
    }

    /**
     * @param pcaEstimation the pcaEstimation to set
     */
    public void setPcaEstimation(double pcaEstimation) {
        this.pcaConfidenceEstimation = pcaEstimation;
    }

    /**
     * For rules with an even number of atoms (n &gt; 2), it checks if it contains
     * level 2 redundant subgraphs, that is, each relation occurs exactly twice
     * in the rule.
     *
     * @return
     */
    public boolean containsLevel2RedundantSubgraphs() {
        if (!isClosed(true) || triples.size() < 4
        		|| triples.size() % 2 == 1) {
            return false;
        }

        Int2IntMap relationCardinalities = new Int2IntOpenHashMap();
        for (int[] pattern : triples) {
            increase(relationCardinalities, pattern[1]);
        }

        for (int relation : relationCardinalities.keySet()) {
            if (relationCardinalities.get(relation) != 2) {
                return false;
            }
        }

        return true;
    }

    public boolean containsDisallowedDiamond() {
        if (!isClosed(true) || triples.size() < 4 || triples.size() % 2 == 1) {
            return false;
        }

        // Calculate the relation count
        Int2ObjectMap<List<int[]>> subgraphs = new Int2ObjectOpenHashMap<List<int[]>>();
        for (int[] pattern : triples) {
            List<int[]> subgraph = subgraphs.get(pattern[1]);
            if (subgraph == null) {
                subgraph = new ArrayList<int[]>();
                subgraphs.put(pattern[1], subgraph);
                if (subgraphs.size() > 2) {
                    return false;
                }
            }
            subgraph.add(pattern);

        }

        if (subgraphs.size() != 2) {
            return false;
        }

        int[] relations = subgraphs.keySet().toIntArray();
        List<int[]> joinInfoList = new ArrayList<int[]>();
        for (int[] p1 : subgraphs.get(relations[0])) {
            int[] bestJoinInfo = null;
            int bestCount = -1;
            int[] bestMatch = null;
            for (int[] p2 : subgraphs.get(relations[1])) {
                int[] joinInfo = Rule.doTheyJoin(p1, p2);
                if (joinInfo != null) {
                    int joinCount = joinCount(joinInfo);
                    if (joinCount > bestCount) {
                        bestCount = joinCount;
                        bestJoinInfo = joinInfo;
                        bestMatch = p2;
                    }
                }
            }
            subgraphs.get(relations[1]).remove(bestMatch);
            joinInfoList.add(bestJoinInfo);
        }

        int[] last = joinInfoList.get(0);
        for (int[] joinInfo : joinInfoList.subList(1, joinInfoList.size())) {
            if (!Arrays.equals(last, joinInfo) || (last[1] == 1 && joinInfo[1] == last[1])) {
                return false;
            }
        }

        return true;
    }

    private int joinCount(int[] vector) {
        int count = 0;
        for (int v : vector) {
            count += v;
        }

        return count;
    }

    private static int[] doTheyJoin(int[] p1, int[] p2) {
        int subPos = KB.varpos(p1[0], p2);
        int objPos = KB.varpos(p1[2], p2);

        if (subPos != -1 || objPos != -1) {
            int[] result = new int[3];
            result[0] = (subPos == 0 ? 1 : 0); //subject-subject
            result[1] = (subPos == 2 ? 1 : 0);
            result[1] += (objPos == 0 ? 1 : 0); //subject-object
            result[2] = (objPos == 2 ? 1 : 0);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Applies the mappings provided as first argument to the subject and object
     * positions of the query included in the second argument.
     *
     * @param mappings
     * @param inputTriples
     */
    public static void bind(Int2IntMap mappings,
            List<int[]> inputTriples) {
        for (int[] triple : inputTriples) {
            int binding = mappings.get(triple[0]);
            if (binding != 0) {
                triple[0] = binding;
            }
            binding = mappings.get(triple[2]);
            if (binding != 0) {
                triple[2] = binding;
            }
        }
    }

    /**
     * Replaces all occurrences of oldVal with newVal in the subject and object
     * positions of the input query.
     *
     * @param oldVal
     * @param newVal
     * @param query
     */
    public static void bind(int oldVal,
            int newVal, List<int[]> query) {
        for (int[] triple : query) {
            if (triple[0] == oldVal) {
                triple[0] = newVal;
            }

            if (triple[2] == oldVal) {
                triple[2] = newVal;
            }
        }
    }

    /**
     * Verifies if the given rule has higher confidence that its parent rules.
     * The parent rules are those rules that were refined in previous stages of
     * the AMIE algorithm and led to the construction of the current rule.
     *
     * @return true if the rule has better confidence that its parent rules.
     */
    public boolean hasConfidenceGain() {
        if (isClosed(true)) {
            if (parent != null && parent.isClosed(true)) {
                return getPcaConfidence() > parent.getPcaConfidence();
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * It returns the query expression corresponding to the normalization value
     * used to calculate the PCA confidence.
     *
     * @return
     */
    public List<int[]> getPCAQuery() {
        if (isEmpty()) {
            return Collections.emptyList();
        }

        List<int[]> newTriples = new ArrayList<>();
        for (int[] triple : triples) {
            newTriples.add(triple.clone());
        }
        int[] existentialTriple = newTriples.get(0);
        existentialTriple[getNonFunctionalVariablePosition()] = KB.map("?x");
        return newTriples;
    }

    /**
     * Given a list of rules A1 =&gt; X1, ... An =&gt; Xn, having the same head
     * relation, it returns the combined rule A1,..., An =&gt; X', where X' is the
     * most specific atom. For example given the rules A1 =&gt; livesIn(x, y) and
     * A2 =&gt; livesIn(x, USA), the method returns A1, A2 =&gt; livesIn(x, USA).
     *
     * @param rules
     * @return
     */
    public static Rule combineRules(List<Rule> rules) {
        if (rules.size() == 1) {
            return new Rule(rules.get(0), rules.get(0).getSupport());
        }

        // Look for the most specific head
        Rule canonicalHeadRule = rules.get(0);
        for (int i = 0; i < rules.size(); ++i) {
            int nVariables = KB.numVariables(rules.get(i).getHead());
            if (nVariables == 1) {
                canonicalHeadRule = rules.get(i);
            }
        }

        // We need to rewrite the rules
        int[] canonicalHead = canonicalHeadRule.getHead().clone();
        int canonicalSubjectExp = canonicalHead[0];
        int canonicalObjectExp = canonicalHead[2];
        IntSet nonHeadVariables = new IntOpenHashSet();
        int varCount = 0;
        List<int[]> commonAntecendent = new ArrayList<>();

        for (Rule rule : rules) {
            List<int[]> antecedentClone = rule.getAntecedentClone();

            IntSet otherVariables = rule.getNonHeadVariables();
            for (int var : otherVariables) {
                Rule.bind(var, KB.map("?v" + varCount), antecedentClone);
                ++varCount;
                nonHeadVariables.add(var);
            }

            int[] head = rule.getHead();
            Int2IntMap mappings = new Int2IntOpenHashMap();
            mappings.put(head[0], canonicalSubjectExp);
            mappings.put(head[2], canonicalObjectExp);
            Rule.bind(mappings, antecedentClone);

            for (int[] atom : antecedentClone) {
            	boolean repeated = false;
            	for (int[] otherAtom : commonAntecendent) {
            		if (equals(atom, otherAtom)) {
            			repeated = true;
            			break;
            		}
            	}
            	if (!repeated) {
            		commonAntecendent.add(atom);
            	}
            }
        }

        Rule resultRule = new Rule(canonicalHead, commonAntecendent, 0.0);
        return resultRule;
    }

    /**
     * The set of variables that are not in the conclusion of the rule.
     */
    private IntSet getNonHeadVariables() {
        int[] head = getHead();
        IntSet nonHeadVars = new IntOpenHashSet();
        for (int[] triple : getAntecedent()) {
            if (KB.isVariable(triple[0])
                    && KB.varpos(triple[0], head) == -1) {
                nonHeadVars.add(triple[0]);
            }

            if (KB.isVariable(triple[2])
                    && KB.varpos(triple[2], head) == -1) {
                nonHeadVars.add(triple[2]);
            }
        }

        return nonHeadVars;
    }

    public boolean containsRelation(int relation) {
        return Rule.containsRelation(triples, relation);
    }

    public int firstIndexOfRelation(int relation) {
    	return firstIndexOfRelation(triples, relation);
    }

    public boolean containsAtom(int[] atom) {
		for (int[] triple : triples) {
			if (Arrays.equals(triple, atom))
				return true;
		}

		return false;
	}

    /**
     * If returns true if the list of triples contains an atom
     * using the given relation.
     * @param triples
     * @param relation
     * @return
     */
    public static boolean containsRelation(List<int[]> triples,
    		int relation) {
    	return firstIndexOfRelation(triples, relation) != -1;
	}

    /**
     * It returns the index of the first atom using the relation.
     * @param triples
     * @param relation
     * @return the index of the atom containing the relation or -1 if such atom
     * does not exist.
     */
    public static int firstIndexOfRelation(List<int[]> triples,
    		int relation) {
    	for (int i = 0; i < triples.size(); ++i) {
            if (triples.get(i)[1] == relation) {
                return i;
            }
        }

        return -1;
    }

    public int numberOfAtomsWithRelation(int relation) {
        int count = 0;
        for (int[] triple : triples) {
            if (triple[1] == relation) {
                ++count;
            }
        }
        return count;
    }

    /**
     * Given a rule as a set of atoms, it
     * returns the combinations of atoms of size 'i' that are "parents" of the
     * current rule, i.e., subsets of atoms of the original rule.
     *
     * @param antecedent
     * @param head
     */
    public static void getParentsOfSize(List<int[]> queryPattern,
            int windowSize, List<List<int[]>> output) {
        List<int[]> antecedent = queryPattern.subList(1, queryPattern.size());
    	int newAntecedentSize = windowSize - 1; // -1 because of the head

        if (queryPattern.size() >= windowSize) {
            return;
        }

        List<int[]> combinations = U.subsetsOfSize(antecedent.size(), newAntecedentSize);
        int[] head = queryPattern.get(0);
        for (int[] combination : combinations) {
            List<int[]> combinationList = new ArrayList<>();
            // Add the head atom.
            combinationList.add(head);
            for (int idx : combination) {
            	combinationList.add(antecedent.get(idx));
            }
            output.add(combinationList);
        }
    }

    /**
     * It determines whether the rule contains a single path that connects the
     * head variables in the body. For example, the rule: - worksAt(x, w),
     * locatedInCity(w, z), locatedInCountry(z, y) =&gt; livesIn(x, y) meets such
     * criteria because there is a single path of variables that connects the
     * head variables in the body: x -&gt; w -&gt; z -&gt; y.
     *
     * @return
     */
    public boolean containsSinglePath() {
        int[] head = getHead();
        if (!KB.isVariable(head[0])
                || !KB.isVariable(head[2])) {
            // We are not interested in rules with a constant in the head.
            return false;
        }

        Int2IntMap histogram = alternativeHistogram();
        for (int i = 1; i < triples.size(); ++i) {
            int[] atom = triples.get(i);
            for (int k : new int[]{0, 2}) {
                if (KB.isVariable(atom[k])) {
                    int freq = histogram.get(atom[k]);
                    if (freq != 0) {
                        if (occursInHead(atom[k])) {
                            if (freq != 1) {
                                return false;
                            }
                        } else {
                            if (freq != 2) {
                                return false;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the given expression (variable or constant) occurs in
     * the rule's head.
     * @param expression
     * @return
     */
    private boolean occursInHead(int expression) {
        int[] head = getHead();
        return (expression == head[0] || expression == head[2]);
    }

    /**
     * Returns an array with the variables that occur in the body but not in the
     * head.
     * @return
     */
    public IntSet getBodyVariables() {
        IntList headVariables = getHeadVariables();
        IntSet result = new IntOpenHashSet();
        for (int[] triple : getBody()) {
            if (KB.isVariable(triple[2])
                    && !headVariables.contains(triple[2])) {
                result.add(triple[2]);
            }

            if (KB.isVariable(triple[2])
                    && !headVariables.contains(triple[2])) {
                result.add(triple[2]);
            }
        }
        return result;
    }

    /**
     * Returns the head variables of the rule.
     *
     * @return
     */
    public IntList getHeadVariables() {
        IntList headVariables = new IntArrayList();
        int[] head = getHead();
        if (KB.isVariable(head[0])) {
            headVariables.add(head[0]);
        }
        if (KB.isVariable(head[2])) {
            headVariables.add(head[2]);
        }
        return headVariables;
    }

    /**
     * Given a rule that contains a single variables path for the head variables
     * in the body (the method containsSinglePath returns true), it returns the
     * atoms sorted so that the path can be reproduced.
     *
     * @return
     */
    public List<int[]> getCanonicalPath() {
        // First check the most functional variable
        int funcVar = getFunctionalVariable();
        int nonFuncVar = getNonFunctionalVariable();
        List<int[]> body = getBody();
        Int2ObjectMap<List<int[]>> variablesToAtom = new Int2ObjectOpenHashMap<>(triples.size(), 1.0f);
        List<int[]> path = new ArrayList<>();
        // Build a multimap, variable -> {atoms where the variable occurs}
        for (int[] bodyAtom : body) {
            if (KB.isVariable(bodyAtom[0])) {
                U.addToMap(variablesToAtom, bodyAtom[0], bodyAtom);
            }

            if (KB.isVariable(bodyAtom[2])) {
                U.addToMap(variablesToAtom, bodyAtom[2], bodyAtom);
            }
        }

        // Start with the functional variable.
        int joinVariable = funcVar;
        int[] lastAtom = null;
        while (true) {
            List<int[]> atomsList = variablesToAtom.get(joinVariable);
            // This can be only the head variable
            if (atomsList.size() == 1) {
                lastAtom = atomsList.get(0);
            } else {
                for (int[] atom : atomsList) {
                    if (atom != lastAtom) {
                        // Bingo
                        lastAtom = atom;
                        break;
                    }
                }
            }
            path.add(lastAtom);
            joinVariable =(lastAtom[0] == joinVariable)?
                    		lastAtom[2] : lastAtom[0];
            if (joinVariable == nonFuncVar) {
                break;
            }
        }

        return path;
    }

    /**
     * Given 2 atoms joining in at least one variable, it returns the first
     * pairs of variable positions of each atom.
     *
     * @param a1
     * @param a2
     * @return
     */
    public static int[] joinPositions(int[] a1, int[] a2) {
        if (a1[0] == a2[0]) {
            return new int[]{0, 0};
        } else if (a1[2] == a2[2]) {
            return new int[]{2, 2};
        } else if (a1[0] == a2[2]) {
            return new int[]{0, 2};
        } else if (a1[2] == a2[0]) {
            return new int[]{2, 0};
        } else {
            return null;
        }
    }

    /**
     * Returns a new rule that is a copy of the current rules plus the two edges
     * sent as arguments.
     *
     * @param newEdge1
     * @param newEdge2
     * @return
     */
    public Rule addEdges(int[] newEdge1, int[] newEdge2) {
        Rule newQuery = new Rule(this, (int) this.support);
        newQuery.triples.add(newEdge1.clone());
        newQuery.triples.add(newEdge2.clone());
        return newQuery;
    }

    /**
     * Returns a list of the relations that occur in the body. The list contains
     * no duplicates.
     * @return
     */
    public IntList getBodyRelationsBS() {
        IntList bodyRelations = new IntArrayList();
        for (int[] atom : getBody()) {
            if (!bodyRelations.contains(atom[1])) {
                bodyRelations.add(atom[1]);
            }
        }
        return bodyRelations;
    }

	public IntList getAllRelationsBS() {
		IntList relations = new IntArrayList();
        for (int[] atom : triples) {
            if (!relations.contains(atom[1])) {
                relations.add(atom[1]);
            }
        }
        return relations;
	}


    /**
     * It returns true if the atoms of the current are a superset for the
     * atoms of the rule sent as argument.
     * @param someRule
     * @return
     */
    public boolean subsumes(Rule someRule) {
		if (someRule.getLength() <= getLength())
			return false;

		if (this.getAntecedent().isEmpty()) {
			return Rule.areEquivalent(someRule.getHead(), getHead());
		}

		List<int[]> combinations = U.subsetsOfSize(someRule.getLength() - 1, getLength() - 1);
		List<int[]> targetAntecedent = someRule.getAntecedent();
		for (int[] cmb : combinations) {
			List<int[]> subsetOfAtoms = new ArrayList<>();
			subsetOfAtoms.add(someRule.getHead());
			for (int idx : cmb) {
				subsetOfAtoms.add(targetAntecedent.get(idx));
			}
			if (QueryEquivalenceChecker3.areEquivalent(subsetOfAtoms, triples))
				return true;
		}

		return false;
	}

    /**
     * Return an equivalent rule (body atoms rotation) with no parent.
     *
     * Useful for debugging.
     * @return
     */
    public Rule getAlternativeEquivalent() {
        List<int[]> body = this.getBody();
        List<int[]> newBody = new ArrayList<>(body.size());
        for (int i = 1; i < body.size(); i++) {
            newBody.add(body.get(i));
        }
        if (!body.isEmpty()) {
            newBody.add(body.get(0));
        }
        Rule result = new Rule(getHead(), newBody, this.support);
        result.setGeneration(generation);
        return result;
    }


    public static void main(String[] args) {
    	Rule rule1 = new Rule(KB.triple("?a", "livesIn", "?x"),
    			KB.triples(KB.triple("?a", "wasBornIn", "?x"), KB.triple("?a", "diedIn", "?x")), 4);

    	Rule rule2 = new Rule(KB.triple("?a", "livesIn", "?x"),
    			KB.triples(KB.triple("?a", "diedIn", "?x")), 1);

    	Rule rule3 = new Rule(KB.triple("?x", "livesIn", "?z"),
    			KB.triples(KB.triple("?x", "wasBornIn", "?z")), 1);

    	Rule rule4 = new Rule(KB.triple("?x", "livesIn", "?z"),
    			KB.triples(KB.triple("?x", "wasBorn", "?h")), 1);

    	Rule rule5 = new Rule(KB.triple("?s1", "livesIn", "?x0"),
    			KB.triples(KB.triple("?x0", "wasBorn", "?v9")), 1);
    	System.out.println(rule2.subsumes(rule1));
    	System.out.println(rule3.subsumes(rule1));
    	System.out.println(rule4.subsumes(rule1));
    	System.out.println(rule5);
    	System.out.println(variablesRegex.matcher("?c").matches());
    }

}
