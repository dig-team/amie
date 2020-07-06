/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import static amie.data.U.decreasingKeys;
import amie.data.tuple.IntPair;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author jlajus
 */
public class InjectiveMappingsAssistant extends LazyMiningAssistant {

    public InjectiveMappingsAssistant(KB dataSource) {
        super(dataSource);
    }

    @Override
    public String getDescription() {
        return "Computes all measures using injective mappings";
    }

    /**
     * Returns all candidates obtained by adding a closing edge (an edge with
     * two existing variables).
     *
     * @param rule
     * @param minSupportThreshold
     * @param output
     */
    @MiningOperator(name = "closing")
    public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
        if (this.enforceConstants) {
            return;
        }

        int nPatterns = rule.getTriples().size();

        if (rule.isEmpty()) {
            return;
        }

        if (!isNotTooLong(rule)) {
            return;
        }

        IntList sourceVariables = null;
        IntList targetVariables = null;
        IntList openVariables = rule.getOpenVariables();
        IntList allVariables = rule.getOpenableVariables();

        if (allVariables.size() < 2) {
            return;
        }

        if (rule.isClosed(false)) {
            sourceVariables = allVariables;
            targetVariables = allVariables;
        } else {
            sourceVariables = openVariables;
            if (sourceVariables.size() > 1) {
                if (this.exploitMaxLengthOption) {
                    // Pruning by maximum length for the \mathcal{O}_C operator.
                    if (sourceVariables.size() > 2
                            && rule.getRealLength() == this.maxDepth - 1) {
                        return;
                    }
                }
                targetVariables = sourceVariables;
            } else {
                targetVariables = allVariables;
            }
        }

        IntPair[] varSetups = new IntPair[2];
        varSetups[0] = new IntPair(0, 2);
        varSetups[1] = new IntPair(2, 0);
        int[] newEdge = rule.fullyUnboundTriplePattern();
        int relationVariable = newEdge[1];

        for (IntPair varSetup : varSetups) {
            int joinPosition = varSetup.first;
            int closeCirclePosition = varSetup.second;
            int joinVariable = newEdge[joinPosition];
            int closeCircleVariable = newEdge[closeCirclePosition];

            for (int sourceVariable : sourceVariables) {
                newEdge[joinPosition] = sourceVariable;

                for (int variable : targetVariables) {
                    if (variable != sourceVariable) {
                        newEdge[closeCirclePosition] = variable;

                        rule.getTriples().add(newEdge);
                        Int2IntMap promisingRelations = null;
                        if (this.enabledFunctionalityHeuristic && this.enableQueryRewriting) {
                            Rule rewrittenQuery = rewriteProjectionQuery(rule, nPatterns, closeCirclePosition);
                            if (rewrittenQuery == null) {
                                long t1 = System.currentTimeMillis();
                                promisingRelations = kb.countProjectionBindings(rule.getHead(), rule.injectiveBody(), newEdge[1]);
                                long t2 = System.currentTimeMillis();
                                if ((t2 - t1) > 20000 && this.verbose) {
                                    System.err.println("countProjectionBindings var=" + newEdge[1] + " " + rule + " has taken " + (t2 - t1) + " ms");
                                }
                            } else {
                                System.out.println(rewrittenQuery + " is a rewrite of " + rule);
                                long t1 = System.currentTimeMillis();
                                promisingRelations = kb.countProjectionBindings(rewrittenQuery.getHead(), rewrittenQuery.injectiveBody(), newEdge[1]);
                                long t2 = System.currentTimeMillis();
                                if ((t2 - t1) > 20000 && this.verbose) {
                                    System.err.println("countProjectionBindings on rewritten query var=" + newEdge[1] + " " + rewrittenQuery + " has taken " + (t2 - t1) + " ms");
                                }
                            }
                        } else {
                            promisingRelations = this.kb.countProjectionBindings(rule.getHead(), rule.injectiveBody(), newEdge[1]);
                        }
                        rule.getTriples().remove(nPatterns);
                        IntList listOfPromisingRelations = decreasingKeys(promisingRelations);
                        for (int relation : listOfPromisingRelations) {
                            int cardinality = promisingRelations.get(relation);
                            if (cardinality < minSupportThreshold) {
                                break;
                            }

                            // Language bias test
                            if (rule.cardinalityForRelation(relation) >= this.recursivityLimit) {
                                continue;
                            }

                            if (this.bodyExcludedRelations != null
                                    && this.bodyExcludedRelations.contains(relation)) {
                                continue;
                            }

                            if (this.bodyTargetRelations != null
                                    && !this.bodyTargetRelations.contains(relation)) {
                                continue;
                            }

                            //Here we still have to make a redundancy check
                            newEdge[1] = relation;
                            Rule candidate = rule.addAtom(newEdge, cardinality);
                            if (!candidate.isRedundantRecursive()) {
                                candidate.setHeadCoverage((double) cardinality / getHeadCardinality(candidate));
                                candidate.setSupportRatio((double) cardinality / (double) this.kb.size());
                                candidate.addParent(rule);
                                output.add(candidate);
                            }
                        }
                    }
                    newEdge[1] = relationVariable;
                }
                newEdge[closeCirclePosition] = closeCircleVariable;
                newEdge[joinPosition] = joinVariable;
            }
        }
    }

    /**
     * Returns all candidates obtained by adding a new triple pattern to the
     * query
     *
     * @param query and will therefore predict too many new facts with scarce
     * evidence,
     * @param minCardinality
     * @param output
     */
    @MiningOperator(name = "dangling")
    public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {
        int[] newEdge = query.fullyUnboundTriplePattern();

        if (query.isEmpty()) {
            throw new IllegalArgumentException("This method expects a non-empty query");
        }

        if (!isNotTooLong(query)) {
            return;
        }

        // Pruning by maximum length for the \mathcal{O}_D operator.
        if (query.getRealLength() == this.maxDepth - 1) {
            if (this.exploitMaxLengthOption) {
                if (!query.getOpenVariables().isEmpty()
                        && !this.allowConstants
                        && !this.enforceConstants) {
                    return;
                }
            }
        }

        getDanglingAtoms(query, newEdge, minCardinality, output);
    }

    /**
     * It adds to the output all the rules resulting from adding dangling atom
     * instantiation of "edge" to the query.
     *
     * @param query
     * @param edge
     * @param minSupportThreshold Minimum support threshold.
     * @param output
     */
    protected void getDanglingAtoms(Rule query, int[] edge, double minSupportThreshold, Collection<Rule> output) {
        IntList joinVariables = null;
        IntList openVariables = query.getOpenVariables();

        //Then do it for all values
        if (query.isClosed(true)) {
            joinVariables = query.getOpenableVariables();
        } else {
            joinVariables = openVariables;
        }

        int nPatterns = query.getLength();

        for (int joinPosition = 0; joinPosition <= 2; joinPosition += 2) {
            for (int joinVariable : joinVariables) {
                int[] newEdge = edge.clone();

                newEdge[joinPosition] = joinVariable;
                query.getTriples().add(newEdge);
                Int2IntMap promisingRelations = null;
                Rule rewrittenQuery = null;
                if (this.enableQueryRewriting) {
                    rewrittenQuery = rewriteProjectionQuery(query, nPatterns, joinPosition == 0 ? 0 : 2);
                }

                if (rewrittenQuery == null) {
                    long t1 = System.currentTimeMillis();
                    promisingRelations = this.kb.countProjectionBindings(query.getHead(), query.injectiveBody(), newEdge[1]);
                    long t2 = System.currentTimeMillis();
                    if ((t2 - t1) > 20000 && this.verbose) {
                        System.err.println("countProjectionBindings var=" + newEdge[1] + " " + query + " has taken " + (t2 - t1) + " ms");
                    }
                } else {
                    long t1 = System.currentTimeMillis();
                    promisingRelations = this.kb.countProjectionBindings(rewrittenQuery.getHead(), rewrittenQuery.injectiveBody(), newEdge[1]);
                    long t2 = System.currentTimeMillis();
                    if ((t2 - t1) > 20000 && this.verbose) {
                        System.err.println("countProjectionBindings on rewritten query var=" + newEdge[1] + " " + rewrittenQuery + " has taken " + (t2 - t1) + " ms");
                    }
                }

                query.getTriples().remove(nPatterns);
                IntList listOfPromisingRelations = decreasingKeys(promisingRelations);
                // The relations are sorted by support, therefore we can stop once we have reached
                // the minimum support.
                for (int relation : listOfPromisingRelations) {
                    int cardinality = promisingRelations.get(relation);

                    if (cardinality < minSupportThreshold) {
                        break;
                    }

                    // Language bias test
                    if (query.cardinalityForRelation(relation) >= recursivityLimit) {
                        continue;
                    }

                    if (bodyExcludedRelations != null
                            && bodyExcludedRelations.contains(relation)) {
                        continue;
                    }

                    if (bodyTargetRelations != null
                            && !bodyTargetRelations.contains(relation)) {
                        continue;
                    }

                    newEdge[1] = relation;
                    //Before adding the edge, verify whether it leads to the hard case
                    //if (containsHardCase(query, newEdge)) {
                    //    continue;
                    //}

                    Rule candidate = query.addAtom(newEdge, cardinality);
                    candidate.setHeadCoverage(candidate.getSupport() / getHeadCardinality(candidate));
                    candidate.setSupportRatio(candidate.getSupport() / this.kb.size());
                    candidate.addParent(query);
                    output.add(candidate);
                }
            }
        }
    }

    @Override
    @MiningOperator(name = "specializing")
    public void getTypeSpecializedAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
    }

    /**
     * Application of the "Add instantiated atom" operator. It takes a rule of
     * the form r(x, w) ^ ..... =&gt; rh(x, y), where r(x, w) is recently added
     * atom and adds to the output all the derived rules where "w" is bound to a
     * constant that keeps the whole pattern above the minCardinality threshold.
     *
     * @param query
     * @param parentQuery
     * @param bindingTriplePos
     * @param danglingPosition
     * @param minSupportThreshold
     * @param output
     */
    protected void getInstantiatedAtoms(Rule query, Rule parentQuery,
            int bindingTriplePos, int danglingPosition, double minSupportThreshold, Collection<Rule> output) {
        int[] danglingEdge = query.getTriples().get(bindingTriplePos);
        Rule rewrittenQuery = null;
        if (!query.isEmpty() && this.enableQueryRewriting) {
            rewrittenQuery = rewriteProjectionQuery(query, bindingTriplePos, danglingPosition == 0 ? 2 : 0);
        }

        Int2IntMap constants = null;
        if (rewrittenQuery != null) {
            long t1 = System.currentTimeMillis();
            constants = this.kb.countProjectionBindings(rewrittenQuery.getHead(), rewrittenQuery.injectiveBody(), danglingEdge[danglingPosition]);
            long t2 = System.currentTimeMillis();
            if ((t2 - t1) > 20000 && this.verbose) {
                System.err.println("countProjectionBindings var=" + danglingEdge[danglingPosition] + " in " + query + " (rewritten to " + rewrittenQuery + ") has taken " + (t2 - t1) + " ms");
            }
        } else {
            long t1 = System.currentTimeMillis();
            constants = this.kb.countProjectionBindings(query.getHead(), query.injectiveBody(), danglingEdge[danglingPosition]);
            long t2 = System.currentTimeMillis();
            if ((t2 - t1) > 20000 && this.verbose) {
                System.err.println("countProjectionBindings var=" + danglingEdge[danglingPosition] + " in " + query + " has taken " + (t2 - t1) + " ms");
            }
        }

        int joinPosition = (danglingPosition == 0 ? 2 : 0);
        for (int constant : constants.keySet()) {
            int cardinality = constants.get(constant);
            if (cardinality >= minSupportThreshold) {
                int[] targetEdge = danglingEdge.clone();
                targetEdge[danglingPosition] = constant;
                assert (KB.isVariable(targetEdge[joinPosition]));

                Rule candidate = query.instantiateConstant(bindingTriplePos, danglingPosition, constant, cardinality);
                // Do this checking only for non-empty queries
                //If the new edge does not contribute with anything
                if (!query.isEmpty()) {
                    long cardLastEdge = this.kb.countDistinct(targetEdge[joinPosition], candidate.injectiveTriples());
                    if (cardLastEdge < 2) {
                        continue;
                    }
                }

                if (candidate.getRedundantAtoms().isEmpty()) {
                    candidate.setHeadCoverage((double) cardinality / (double) getHeadCardinality(candidate));
                    candidate.setSupportRatio((double) cardinality / (double) kb.size());
                    candidate.addParent(parentQuery);
                    output.add(candidate);
                }
            }
        }
    }

    @Override
    public double computeCardinality(Rule rule) {
        if (rule.isEmpty()) {
            rule.setSupport(0l);
            rule.setHeadCoverage(0.0);
            rule.setSupportRatio(0.0);
        } else {
            int[] head = rule.getHead();
            if (KB.numVariables(head) == 2) {
                rule.setSupport(this.kb.countDistinctPairs(
                        order.getFirstCountVariable(rule),
                        order.getSecondCountVariable(rule), rule.injectiveTriples()));
            } else {
                rule.setSupport(this.kb.countDistinct(rule.getFunctionalVariable(), rule.injectiveTriples()));
            }
            rule.setSupportRatio((double) rule.getSupport() / this.kb.size());
            Double relationSize = new Double(this.getHeadCardinality(rule));
            if (relationSize != null) {
                rule.setHeadCoverage(rule.getSupport() / relationSize.doubleValue());
            }
        }
        return rule.getSupport();
    }

    @Override
    public boolean calculateConfidenceBoundsAndApproximations(Rule candidate) {
        return true;
    }

    @Override
    public double computeStandardConfidence(Rule candidate) {
        if (candidate.isEmpty()) {
            return candidate.getStdConfidence();
        }
        // TODO Auto-generated method stub
        List<int[]> antecedent = candidate.getAntecedent();
        double denominator = 0.0;
        int[] head = candidate.getHead();

        if (!antecedent.isEmpty()) {
            //Confidence
            try {
                if (KB.numVariables(head) == 2) {
                    int var1, var2;
                    var1 = order.getFirstCountVariable(candidate);
                    var2 = order.getSecondCountVariable(candidate);
                    denominator = (double) computeBodySize(var1, var2, candidate);
                } else {
                    denominator = (double) this.kb.countDistinct(candidate.getFunctionalVariable(), candidate.injectiveBody());
                }
                candidate.setBodySize((long) denominator);
            } catch (UnsupportedOperationException e) {

            }
        }

        return candidate.getStdConfidence();
    }

    /**
     * Returns the number of distinct bindings of the given variables in the
     * body of the rule.
     *
     * @param var1
     * @param var2
     * @param query
     * @return
     */
    @Override
    protected long computeBodySize(int var1, int var2, Rule query) {
        long t1 = System.currentTimeMillis();
        long result;
        if (this.minStdConfidence > 0.0) {
            result = this.kb.countDistinctPairsUpTo((long) Math.ceil(query.getSupport() / this.minStdConfidence) + 1, var1, var2, query.injectiveBody());
        } else {
            result = this.kb.countDistinctPairs(var1, var2, query.injectiveBody());
        }
        long t2 = System.currentTimeMillis();
        query.setConfidenceRunningTime(t2 - t1);
        if ((t2 - t1) > 20000 && this.verbose) {
            System.err.println("countPairs vars " + var1 + ", " + var2 + " in " + KB.toString(query.injectiveBody()) + " has taken " + (t2 - t1) + " ms");
        }
        return result;
    }

    public double computePCAConfidence(Rule rule) {
        /*
         * e.g Injective PCA.
         */
        if (rule.isEmpty()) {
            return 0;
        }

        List<int[]> antecedent = rule.getAntecedentClone();
        int[] succedent = rule.getTriples().get(0);
        double pcaDenominator;
        int[] existentialTriple = succedent.clone();
        int freeVarPos;
        int noOfHeadVars = KB.numVariables(succedent);

        if (noOfHeadVars == 1) {
            freeVarPos = KB.firstVariablePos(succedent) == 0 ? 2 : 0;
        } else {
            if (existentialTriple[0] == rule.getFunctionalVariable()) {
                freeVarPos = 2;
            } else {
                freeVarPos = 0;
            }
        }

        existentialTriple[freeVarPos] = KB.map("?x9");
        if (!antecedent.isEmpty()) {

            try {
                if (noOfHeadVars == 1) {
                    antecedent = rule.injectiveBody();
                    antecedent.add(existentialTriple);
                    pcaDenominator = (double) this.kb.countDistinct(rule.getFunctionalVariable(), antecedent);
                } else {
                    pcaDenominator = (double) computePcaBodySize(
                            order.getFirstCountVariable(rule),
                            order.getSecondCountVariable(rule), rule, rule.injectiveBody(), existentialTriple, freeVarPos);
                }
                rule.setPcaBodySize(pcaDenominator);
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        return rule.getPcaConfidence();
    }
}
