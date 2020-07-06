package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import amie.data.KB;
import amie.data.Schema;
import amie.data.tuple.IntArrays;
import amie.data.tuple.IntPair;
import amie.mining.assistant.MiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.QueryEquivalenceChecker3;
import amie.rules.Rule;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javatools.datatypes.MultiMap;

public class CompletenessMiningAssistant extends MiningAssistant {
	public enum Mode {Standard, OnlyStar, OnlyType, OnlyCard};
	
	protected Mode mode;

	public static final String isComplete = "isComplete";
	
	public static final String isIncomplete = "isIncomplete";
	
	public static final String isRelevanthasNumberOfFacts = "<isRelevanthasNumberOfFacts>";
	
	public static final String isRelevanthasWikiLength = "<isRelevanthasWikiLength>";
	
	public static final String isRelevanthasIngoingLinks = "<isRelevanthasIngoingLinks>";
	
	public static final String hasChanged = "<hasChanged>";
	
	public static final String hasNotChanged = "<hasNotChanged>";
	
	public static final int isRelevanthasWikiLengthBS = KB.map(isRelevanthasWikiLength); 
	
	public static final int isRelevanthasIngoingLinksBS = KB.map(isRelevanthasIngoingLinks); 
	
	public static final int hasChangedBS = KB.map(hasChanged);
	
	public static final int hasNotChangedBS = KB.map(hasNotChanged);
	
	public static final int isCompleteBS = KB.map(isComplete);
	
	public static final int isIncompleteBS = KB.map(isIncomplete);
	
	public static final int isRelevanthasNumberOfFactsBS = KB.map(isRelevanthasNumberOfFacts);
	
	private static final IntList functionalExceptions = IntArrays.asList(KB.map("<hasChild>"), 
			KB.map("<child_P40>"), KB.map("<spokenIn>"));
	
	public CompletenessMiningAssistant(KB dataSource) {
		super(dataSource);
		mode = Mode.Standard;
		this.allowConstants = false;
		this.bodyExcludedRelations = IntArrays.asList(Schema.typeRelationBS, 
				Schema.subClassRelationBS, Schema.domainRelationBS, 
				Schema.rangeRelationBS, isCompleteBS, isIncompleteBS,
				isRelevanthasWikiLengthBS, isRelevanthasIngoingLinksBS, isRelevanthasNumberOfFactsBS,
				hasChangedBS, hasNotChangedBS);
		this.recursivityLimit = 1;
	}

	@Override
	public String getDescription() {
        return "Mining completeness rules of the form "
        		+ "B => isComplete(x, relation) or B => isIncomplete(x, relation) "
        		+ "[EXPERIMENTAL]";
	}
	
	@Override
	public void setHeadExcludedRelations(IntCollection headExcludedRelations) {};
	
	@Override
	public void setBodyExcludedRelations(IntCollection bodyExcludedRelations) {};
	
	@Override
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output){}
	
	@Override
	public void setRecursivityLimit(int recursitivityLimit) {
		System.err.println(this.getClass().getName() 
				+ ": The recursivity limit for this class is fixed to 1 and cannot be changed.");
	}
	
	/**
	 * It returns true if the relation must be treated as a functional relation.
	 * @param relation
	 * @return
	 */
	public boolean isFunctional(int relation) {
		return this.kb.isFunctional(relation) || functionalExceptions.contains(relation);
	}
	
	@Override
	protected Collection<Rule> buildInitialQueries(Int2IntMap relations, 
			double minSupportThreshold) {
		Collection<Rule> output = new ArrayList<>();
		Rule query = new Rule();
		
		int[] newEdge = query.fullyUnboundTriplePattern();
		
		for (int relation: relations.keySet()) {		
			if (relation == isCompleteBS ||
					relation == isIncompleteBS) {
				registerHeadRelation(relation, (double) relations.get(relation));
				continue;
			}
			int[] succedent = newEdge.clone();
			succedent[1]  = isCompleteBS;
			succedent[2] = relation;
			int countVarPos = 0;
			long cardinalityComplete = kb.count(succedent);
			
			if (cardinalityComplete >= minSupportThreshold) {
				Rule candidateComplete =  new Rule(succedent, cardinalityComplete);
				candidateComplete.setFunctionalVariablePosition(countVarPos);
				output.add(candidateComplete);
			}
						
			succedent[1] = isIncompleteBS;
			long cardinalityIncomplete = kb.count(succedent);
			if (cardinalityIncomplete >= minSupportThreshold) {
				Rule candidateIncomplete = new Rule(succedent, cardinalityIncomplete);
				candidateIncomplete.setFunctionalVariablePosition(countVarPos);
				output.add(candidateIncomplete);
			}
		}
		
		return output;
	}
	

	@Override
	@MiningOperator(name="specializing")
	public void getTypeSpecializedAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		int[] lastAtom = rule.getLastRealTriplePattern();
                IntPair compositeRelation = KB.parseCardinalityRelation(lastAtom[1]);
		if (compositeRelation == null) {
			super.getTypeSpecializedAtoms(rule, minSupportThreshold, output);
		} else {
			int oldRelation = lastAtom[1];
			// Do not specialize the equals relation
			if (KB.unmap(oldRelation).startsWith(KB.hasNumberOfValuesEquals)) 
				return;
			
			int targetRelation = lastAtom[2];
			int newCard = -1;
			int[] head = rule.getHead(); 
			if (head[1] == isCompleteBS) {
				if (head[2] != lastAtom[2])
					return;
					
				if (this.isFunctional(targetRelation)) {
					newCard = kb.maximalRightCumulativeCardinality(targetRelation, 
						(long)minSupportThreshold, compositeRelation.second);
				} else {
					newCard = kb.maximalRightCumulativeCardinalityInv(targetRelation, 
							(long)minSupportThreshold, compositeRelation.second);					
				}
				if (newCard == -1)
					return;
			} else {
				if (head[2] != lastAtom[2])
					return;
				if (this.isFunctional(targetRelation)) {
					newCard = kb.maximalCardinality(targetRelation, compositeRelation.second);
				} else {
					newCard = kb.maximalCardinalityInv(targetRelation, compositeRelation.second);
				}
				
				if (newCard == compositeRelation.second)
					return;
			}
								
			int newRelation = KB.compose(compositeRelation.first, newCard);
			lastAtom[1] = newRelation;
			long cardinality = kb.countDistinct(rule.getFunctionalVariable(), rule.getTriples());
			lastAtom[1] = oldRelation;
			if (cardinality >= minSupportThreshold) {
				int[] newAtom = lastAtom.clone();
				newAtom[1] = newRelation;				
				Rule candidate = rule.replaceLastAtom(newAtom, cardinality);
				candidate.addParent(rule);
				output.add(candidate);
			}
		}
	}

	
	private void addCardinalityAtom(Rule rule, double minSupportThreshold, 
			int targetRelation,
			Collection<Rule> output) {
		// We'll force a cardinality atom at the end
		int[] head = rule.getHead();
		int startCardinality = -1;		
		int[] newAtom = head.clone();
		if (this.isFunctional(targetRelation)) {
			startCardinality  = 0;
			newAtom[1] = KB.mapComposite(KB.hasNumberOfValuesGreaterThan, startCardinality);
			this.addAtom(rule, newAtom, minSupportThreshold, output);

			startCardinality = kb.maximalCardinality(targetRelation) + 1;
			newAtom[1] = KB.mapComposite(KB.hasNumberOfValuesSmallerThan, startCardinality);
			this.addAtom(rule, newAtom, minSupportThreshold, output);
		} else {
			startCardinality = 0;
			newAtom[1] = KB.mapComposite(KB.hasNumberOfValuesGreaterThanInv, startCardinality);
			this.addAtom(rule, newAtom, minSupportThreshold, output);

			startCardinality = kb.maximalCardinalityInv(targetRelation) + 1;
			newAtom[1] = KB.mapComposite(KB.hasNumberOfValuesSmallerThanInv, startCardinality);
			this.addAtom(rule, newAtom, minSupportThreshold, output);
		}
		
	}
	
	private void addAtom(Rule rule, int[] newAtom, double minSupportThreshold, Collection<Rule> output) {
		rule.getTriples().add(newAtom);
		long cardinality = kb.countDistinct(rule.getFunctionalVariable(), rule.getTriples());
		rule.getTriples().remove(rule.getTriples().size() - 1);			
		if (cardinality >= minSupportThreshold) {
			Rule candidate = rule.addAtom(newAtom, cardinality);
			output.add(candidate);
			candidate.addParent(rule);
		}
	}
	
	@Override
	@MiningOperator(name="instantiated", dependency="dangling")
	public void getInstantiatedAtoms(Rule parentRule, double minSupportThreshold, 
		Collection<Rule> danglingEdges, Collection<Rule> output) {
		boolean extendRule = true;
		
		if (mode == Mode.Standard || mode == Mode.OnlyCard) {
			if (!containsCardinalityAtom(parentRule, parentRule.getHead()[2])) {
				addCardinalityAtom(parentRule, minSupportThreshold, parentRule.getHead()[2], output);
			}
		}
		
		int[] lastAtom = parentRule.getLastRealTriplePattern();
		if (!parentRule.containsRelation(KB.DIFFERENTFROMbs) && 
				extendRule &&(lastAtom[1] == Schema.typeRelationBS)
				&& !KB.isVariable(lastAtom[2])) {
			addTypeNegationAtom(parentRule, minSupportThreshold, output);
		}
		
		if (mode == Mode.Standard || mode == Mode.OnlyType) {
			if (!parentRule.containsRelation(Schema.typeRelationBS) && extendRule) {
				addTypeAtom(parentRule, minSupportThreshold, output);
			}
		}
		
		if (!parentRule.containsRelation(isRelevanthasNumberOfFactsBS) && extendRule) {
			addRelevanceAtom(parentRule, isRelevanthasNumberOfFactsBS, minSupportThreshold, output);
		}
		
		if (!parentRule.containsRelation(isRelevanthasWikiLengthBS) && extendRule) {
			addRelevanceAtom(parentRule, isRelevanthasWikiLengthBS, minSupportThreshold, output);
		}
		
		if (!parentRule.containsRelation(isRelevanthasIngoingLinksBS) && extendRule) {
			addRelevanceAtom(parentRule, isRelevanthasIngoingLinksBS, minSupportThreshold, output);
		}
		
		if (!parentRule.containsRelation(hasNotChangedBS) && extendRule) {
			addChangedAtom(parentRule, hasNotChangedBS, minSupportThreshold, output);
		}
	}
	
	private void addTypeNegationAtom(Rule parentRule, double minSupportThreshold, Collection<Rule> output) {
		// First look for the instantiated atom
		int[] lastAtom = parentRule.getLastRealTriplePattern(); // We assume it is an instantiated type atom
		int[] atom1 = parentRule.fullyUnboundTriplePattern();
		int countVar = parentRule.getFunctionalVariable();
		
		atom1[0] = countVar;
		atom1[1] = Schema.typeRelationBS;
		IntSet subtypes = Schema.getSubTypes(this.kbSchema, lastAtom[2]);
		int[] atom2 = KB.triple(atom1[2], KB.DIFFERENTFROMbs, lastAtom[2]);
		long baseCardinality = Schema.getNumberOfEntitiesForType(this.kb, lastAtom[2]);
		List<int[]> parentRuleTriples = parentRule.getTriples();
		for (int subtype : subtypes) {
			long typeCardinality = Schema.getNumberOfEntitiesForType(this.kb, subtype);
			double ratio = (double) typeCardinality / baseCardinality;
			if (typeCardinality < minSupportThreshold || ratio < 0.05)
				continue;
			
			atom2[2] = subtype;
			IntSet supportSet = new IntOpenHashSet(kb.selectDistinct(countVar, parentRuleTriples));
			IntSet subTypeSet = Schema.getAllEntitiesForType(this.kb, subtype);
			supportSet.removeAll(subTypeSet);
			long cardinality = supportSet.size();
			if (cardinality >= minSupportThreshold) {
				Rule candidate = parentRule.addAtoms(atom1, atom2, cardinality);
				candidate.addParent(parentRule);
				output.add(candidate);			
			}	
		}		
	}
	
	@Override
	public boolean shouldBeOutput(Rule candidate) {
		return candidate.getRealLength() > 1;
	}
	
	public void addDanglingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		if (mode == Mode.OnlyCard || mode == Mode.OnlyType)
			return;
			
		int[] newEdge = rule.fullyUnboundTriplePattern();
		int[] head = rule.getHead();
		//General case
		if(!isNotTooLong(rule))
			return;

		int nPatterns = rule.getTriples().size();		
		newEdge[0] = head[0];
		rule.getTriples().add(newEdge);
		Int2IntMap promisingRelations = kb.frequentBindingsOf(newEdge[1], 
				rule.getFunctionalVariable(), rule.getTriples());
		rule.getTriples().remove(nPatterns);
		
		for(int relation: promisingRelations.keySet()){
			if (this.bodyExcludedRelations != null && 
					this.bodyExcludedRelations.contains(relation))
				continue;
			//Here we still have to make a redundancy check						
			int cardinality = promisingRelations.get(relation);
			if(cardinality >= minSupportThreshold) {
				if(rule.containsRelation(relation) 
						||(relation == head[2])) {
					continue;
				}
				
				newEdge[1] = relation;
				Rule candidate = rule.addAtom(newEdge, cardinality);
				candidate.setHeadCoverage((double)candidate.getSupport() 
						/ headCardinalities.get(candidate.getHeadRelationBS()));
				candidate.setSupportRatio((double)candidate.getSupport() 
						/ (double)getTotalCount(candidate));
				candidate.addParent(rule);
				output.add(candidate);
			}
		}
	}	

	@Override
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
		this.addDanglingAtoms(rule, minSupportThreshold, output);
	}

	private void addTypeAtom(Rule parentRule, double minSupportThreshold, Collection<Rule> output) {
		int[] head = parentRule.getHead();
		int relation = head[2];
		int[] newEdge = head.clone();
		newEdge[1] = Schema.typeRelationBS;
		int domain = 0;
		if (this.kbSchema != null) {
			if (this.isFunctional(relation)) {
				domain = Schema.getRelationDomain(this.kbSchema, relation);
			} else {
				domain = Schema.getRelationRange(this.kbSchema, relation);
			}
		}
		
		if (domain == 0)
			return;
		
		newEdge[2] = domain;
		
		parentRule.getTriples().add(newEdge);
		long cardinalityComplete = kb.countDistinct(head[0], parentRule.getTriples());
		parentRule.getTriples().remove(parentRule.getTriples().size() - 1);
		
		if (cardinalityComplete >= minSupportThreshold) {
			Rule candidateComplete = parentRule.addAtom(newEdge, cardinalityComplete);
			candidateComplete.addParent(parentRule);
			output.add(candidateComplete);
		}
	}

	private void addChangedAtom(Rule parentRule, int changeRelation, double minSupportThreshold,
			Collection<Rule> output) {
		int[] head = parentRule.getHead();
		int targetRelation = head[2];
		
		int[] changedAtom = new int[]{parentRule.getFunctionalVariable(), 
				changeRelation, targetRelation};
		
		parentRule.getTriples().add(changedAtom);
		long support = kb.countDistinct(changedAtom[0], parentRule.getTriples());
		parentRule.getTriples().remove(parentRule.getTriples().size() - 1);
		if (support > minSupportThreshold) {
			Rule candidate = parentRule.addAtom(changedAtom, support);
			candidate.addParent(parentRule);
			output.add(candidate);
		}
		
	}

	private void addRelevanceAtom(Rule parentRule, int relevanceRelation,
			double minSupportThreshold, Collection<Rule> output) {
		int[] relevanceAtom = new int[]{parentRule.getFunctionalVariable(), 
				relevanceRelation, KB.map("TRUE")};
		
		parentRule.getTriples().add(relevanceAtom);
		long support = kb.countDistinct(relevanceAtom[0], parentRule.getTriples());
		parentRule.getTriples().remove(parentRule.getTriples().size() - 1);
		if (support > minSupportThreshold) {
			Rule candidate = parentRule.addAtom(relevanceAtom, support);
			candidate.addParent(parentRule);
			output.add(candidate);
		}
	}
	
	@Override
	protected boolean isNotTooLong(Rule candidate) {
		int maxLength = maxDepth;
		if (candidate.containsRelation(Schema.typeRelationBS)) ++maxLength;
		if (candidate.containsRelation(KB.DIFFERENTFROMbs)) ++maxLength;
		if (candidate.containsRelation(isRelevanthasNumberOfFactsBS)) ++maxLength;
		if (candidate.containsRelation(isRelevanthasWikiLengthBS)) ++maxLength;
		if (candidate.containsRelation(isRelevanthasIngoingLinksBS)) ++maxLength;
		if (containsCardinalityAtom(candidate, candidate.getHead()[2])) ++maxLength;
		
		return candidate.getRealLength() <= maxLength;
	}
	
	
	
	/**
	 * Returns true if the rule contains a cardinality constraint 
	 * atom.
	 * @param rule
	 * @return
	 */
	private boolean containsCardinalityAtom(Rule rule, int targetRelation) {
		int idx = indexOfCardinalityAtom(rule);
		if (idx == -1) {
			return false;
		} else {
			return rule.getTriples().get(idx)[2] == (targetRelation);
		}
	}
	
	@Override
	public void setAdditionalParents(Rule currentRule, MultiMap<Integer, Rule> indexedOutputSet) {
		int idxOfRelationAtom = currentRule.firstIndexOfRelation(Schema.typeRelationBS);
		int idxOfCardinalityRelation = indexOfCardinalityAtom(currentRule);
		if (idxOfRelationAtom == -1 && idxOfCardinalityRelation == -1) {
			super.setAdditionalParents(currentRule, indexedOutputSet);
		}
        
        // First check if there are no a parents of the same size caused by
        // specialization operators. For example if the current rule is
        // A: livesIn(x, Paris), type(x, Architect) => isFamous(x, true) derived from 
        // B: type(x, Architect) => isFamous(x, true) but in other thread we have mined the rule
        // C: livesIn(x, Paris) (x, Person) => isFamous(x, true), then we need to make the bridge between
        // A and C (namely C is also a father of A)
        int offset = 0;
        List<int[]> queryPattern = currentRule.getTriplesWithoutSpecialRelations();
        // Go up until you find a parent that was output
        while (queryPattern.size() - offset > 1) {
        	int currentLength = queryPattern.size() - offset;
            int parentHashCode = Rule.headAndGenerationHashCode(currentRule.getHeadKey(), currentLength);
            // Give all the rules of size 'currentLength' and the same head atom (potential parents)
            Set<Rule> candidateParentsOfCurrentLength = indexedOutputSet.get(parentHashCode);
            
            if (candidateParentsOfCurrentLength != null) {
	            for (Rule parent : candidateParentsOfCurrentLength) {
	            	boolean subsumes = parent.subsumes(currentRule);
	            	boolean subsumesWithSpecialAtoms = subsumesWithSpecialAtoms(parent, currentRule);
	            	
	                if (subsumes || subsumesWithSpecialAtoms) {
	                	currentRule.addParent(parent);	                	
	                }
	            }
        	}
            ++offset;
        }  
    }
	
	private boolean subsumesWithSpecialAtoms(Rule parent, Rule currentRule) {
		int idxOfTypeAtomRule = currentRule.firstIndexOfRelation(Schema.typeRelationBS);
		int idxOfCardinalityAtomRule = indexOfCardinalityAtom(currentRule);
		int idxOfTypeAtomParent = parent.firstIndexOfRelation(Schema.typeRelationBS);
		int idxOfCardinalityAtomParent = indexOfCardinalityAtom(parent);
				
		if (idxOfTypeAtomRule != -1 && idxOfTypeAtomParent != -1) {
			int[] typeAtomInRule = currentRule.getTriples().get(idxOfTypeAtomRule);
			int[] typeAtomInParent = parent.getTriples().get(idxOfTypeAtomParent);
			List<int[]> triplesRule = currentRule.getTriplesCopy();
			List<int[]> triplesParent = parent.getTriplesCopy();
			triplesParent.remove(idxOfTypeAtomParent);
			triplesRule.remove(idxOfTypeAtomRule);
			Rule newRule = new Rule(currentRule.getHead(), triplesRule.subList(1, triplesRule.size()), 0);
			Rule newParent = new Rule(parent.getHead(), triplesParent.subList(1, triplesParent.size()), 0);
			boolean hasSameType = typeAtomInParent[2] == typeAtomInRule[2];
			boolean isSuperType = Schema.isSuperType(this.kbSchema, typeAtomInParent[2], typeAtomInRule[2]);
			if (hasSameType || isSuperType) {
				return subsumesWithSpecialAtoms(newParent, newRule);
			}
		}
		
		if (idxOfCardinalityAtomRule != -1 && idxOfCardinalityAtomParent != -1) {
			int[] cardinalityAtomInRule = currentRule.getTriples().get(idxOfCardinalityAtomRule);
			int[] cardinalityAtomInParent = parent.getTriples().get(idxOfCardinalityAtomParent);
			List<int[]> triplesParent = parent.getTriplesCopy();
			List<int[]> triplesRule = currentRule.getTriplesCopy();
			triplesParent.remove(idxOfCardinalityAtomParent);
			triplesRule.remove(idxOfCardinalityAtomRule);
			Rule newRule = new Rule(currentRule.getHead(), triplesRule.subList(1, triplesRule.size()), 0);
			Rule newParent = new Rule(parent.getHead(), triplesParent.subList(1, triplesParent.size()), 0);
			if (Arrays.equals(cardinalityAtomInParent, cardinalityAtomInRule) || 
					subsumesCardinalityAtom(cardinalityAtomInParent, cardinalityAtomInRule)) {
				return subsumesWithSpecialAtoms(newParent, newRule);
			}
		}
		
		return QueryEquivalenceChecker3.areEquivalent(parent.getTriples(), currentRule.getTriples())
				|| parent.subsumes(currentRule);
	}

	private boolean subsumesCardinalityAtom(int[] triplesParent, int[] triplesRule) {
		
		if (KB.isComposite(triplesParent[1]) && KB.isComposite(triplesRule[1])) {
                    
                    IntPair relationPairParent = KB.uncompose(triplesParent[1]);
                    IntPair relationPairRule = KB.uncompose(triplesRule[1]);
                    
                    IntList gtList = IntArrays.asList(KB.hasNumberOfValuesGreaterThanBS, 
				KB.hasNumberOfValuesGreaterThanInvBS);
		
                    IntList stList = IntArrays.asList(KB.hasNumberOfValuesSmallerThanBS, 
				KB.hasNumberOfValuesSmallerThanInvBS);
                
			if (relationPairParent.first == (relationPairRule.first)) {
				if (gtList.contains(relationPairParent.first)) {
					return relationPairParent.second < relationPairRule.second;
				} else if (stList.contains(relationPairParent.first)) {
					return relationPairParent.second > relationPairRule.second;
				}					
			} else {
				if (relationPairRule.first == (KB.hasNumberOfValuesGreaterThanBS) 
						&& relationPairRule.second == 0) {
					return relationPairParent.second == 0 && 
							relationPairParent.first == (KB.hasNumberOfValuesEqualsBS);
				} else if (relationPairRule.first == (KB.hasNumberOfValuesGreaterThanInvBS) 
						&& relationPairRule.second == 0) {
					return relationPairParent.second == 0 && 
							relationPairParent.first == (KB.hasNumberOfValuesEqualsInvBS);
				}
			}
		}
		
		return false;
	}

	private int indexOfCardinalityAtom(Rule rule) {
		List<int[]> triples = rule.getTriples();
		for (int i = triples.size() - 1; i >= 0; --i) {
			if (KB.parseCardinalityRelation(triples.get(i)[1]) != null)
				return i;
		}
		
		return -1;
	}
	
	@Override
	public void calculateConfidenceMetrics(Rule candidate) {	
		candidate.setBodySize((long)candidate.getSupport());
		computePCAConfidence(candidate);
	}

	@Override
	public double computePCAConfidence(Rule rule) {
		Rule negativeRule = new Rule(rule, rule.getSupport());
		int[] succedent = negativeRule.getHead();
		succedent[1] =(succedent[1] == isCompleteBS)? isIncompleteBS : isCompleteBS;
		long counterEvidence = 0;
		double support = rule.getSupport();
		if (!rule.containsRelation(KB.DIFFERENTFROMbs)) {
			counterEvidence = kb.countDistinct(succedent[0], negativeRule.getTriples());
		} else {
			// We have to calculate the support in a different way
			int differentIdx = negativeRule.firstIndexOfRelation(KB.DIFFERENTFROMbs);
			int typeIdx = differentIdx - 1;
			int[] differentAtom = negativeRule.getTriples().get(differentIdx);
			int[] typeAtom = negativeRule.getTriples().get(typeIdx);
			negativeRule.getTriples().remove(differentAtom);
			negativeRule.getTriples().remove(typeAtom);
			IntSet negativeSupportSet = new IntOpenHashSet(kb.selectDistinct(succedent[0], 
					negativeRule.getTriples()));
			IntSet typeSet = Schema.getAllEntitiesForType(this.kb, differentAtom[2]);
			negativeSupportSet.removeAll(typeSet);
			counterEvidence = negativeSupportSet.size();
		}
		rule.setPcaBodySize(support + counterEvidence);			
		return rule.getPcaConfidence();
	}
	
	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}
	
	public static void main(String args[]) {
		CompletenessMiningAssistant assistant = new CompletenessMiningAssistant(new KB());
		List<int[]> ruleTriples = KB.triples(
				KB.triple("?a", KB.hasNumberOfValuesSmallerThanInv + "1", "hasChild"), 
				KB.triple("?l", "marriedTo", "?a"), KB.triple("?a", "marriedTo", "?l"));
		int[] head = KB.triple("?a", isIncomplete, "hasChild");
		List<int[]> parentTriples = KB.triples(
				KB.triple("?a", KB.hasNumberOfValuesSmallerThanInv + "2", "hasChild"));
		Rule parent = new Rule(head, parentTriples, 1);
		Rule currentRule = new Rule(head, ruleTriples, 1);
		System.out.println(parent.subsumes(currentRule));
		System.out.println(assistant.subsumesWithSpecialAtoms(parent, currentRule));
	}
}