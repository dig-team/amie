package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;
import amie.data.Schema;
import amie.mining.assistant.MiningAssistant;
import amie.rules.ConfidenceMetric;
import amie.rules.Rule;

/**
 * This mining assistant drives the AMIE algorithm so that it outputs rules of the forms
 * type(x, C) => r(x, y) 
 * type(x, C) => ~r(x, y)
 * @author galarrag
 *
 */
public class ExistentialAndNegatedRulesMiningAssistant extends MiningAssistant {

	public ExistentialAndNegatedRulesMiningAssistant(KB dataSource) {
		super(dataSource);
		this.maxDepth = 2;
		this.allowConstants = false;
		this.headExcludedRelations = Arrays.asList(KB.map(Schema.typeRelationBS));
		this.confidenceMetric = ConfidenceMetric.StandardConfidence;
	}
	
	@Override
	public String getDescription() {
        return "Mining existential rules of the form "
        		+ "type(x, C) => r(x, y) and type(x, C) => ~r(x, y) "
        		+ "[EXPERIMENTAL]";
	}
	
	@Override
	public void setMaxDepth(int maxAntecedentDepth) {};
	
	@Override
	public void setAllowConstants(boolean allowConstants) {};
	
	@Override
	public void setHeadExcludedRelations(java.util.Collection<ByteString> headExcludedRelations) {};
	
	@Override
	protected Collection<Rule> buildInitialQueries(Int2IntMap relations, 
			double minSupportThreshold) {
		// Now we have to take care of the negative ones
		// => ~r(x, y). We will use the keywords NOTEXISTSbs and NOTEXISTSINVbs
		Collection<Rule> output = new ArrayList<>();
		Rule query = new Rule();
		ByteString[] newEdge1 = query.fullyUnboundTriplePattern();
		ByteString[] newEdge2 = query.fullyUnboundTriplePattern();
		for (ByteString relation : relations) {
			if (this.headExcludedRelations != null 
					&& this.headExcludedRelations.contains(relation)) {
				continue;
			}
						
			if (relation.equals(KB.EQUALSbs) || 
					relation.equals(KB.DIFFERENTFROMbs))
				continue;
			
			newEdge1[0] = relation;
			newEdge2[0] = relation;
			
			// Ignore small relations
			if (kb.relationSize(relation) < minSupportThreshold) 
				continue;
			
			if (kb.isFunctional(relation)) {
				newEdge1[1] = KB.NOTEXISTSbs;			
				newEdge2[1] = KB.EXISTSbs;
			} else {
				newEdge1[1] = KB.NOTEXISTSINVbs;
				newEdge2[1] = KB.EXISTSINVbs;
			}
			
			long support = kb.count(newEdge1);
			if (support >= minSupportThreshold) {
				ByteString[] succedent = newEdge1.clone();
				Rule rule = new Rule(succedent, support);
				rule.setFunctionalVariablePosition(2);
				output.add(rule);
			}
			
			support = kb.count(newEdge2);
			if (support >= minSupportThreshold) {
				ByteString[] succedent = newEdge2.clone();
				Rule rule = new Rule(succedent, support);
				rule.setFunctionalVariablePosition(2);
				output.add(rule);
			}
		}
		return output;
	}
	
	@Override
	public void getDanglingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
	}
	
	@Override
	public double computePCAConfidence(Rule rule) {
		// The PCA is not defined for this type of rules
		return -1.0;
	}
	
	@Override
	public void getClosingAtoms(Rule rule, double minSupportThreshold, Collection<Rule> output) {
	}
	
	public void getInstantiatedAtoms(Rule rule, double minSupportThreshold, 
			Collection<Rule> danglingEdges, Collection<Rule> output) {
		if (rule.getLength() == 1) {
			// We enforce the type relationship with the domain or range of the relation
			KB source = null;
			ByteString[] head = rule.getHead();
			if (this.kbSchema != null) {
				source = this.kbSchema;
			} else {
				source = this.kb;
			}
			ByteString typeToEnforce = null;
			if (head[1].equals(KB.NOTEXISTSbs) || head[1].equals(KB.EXISTSbs)) {
				typeToEnforce = Schema.getRelationDomain(source, head[0]);
			} else if (head[1].equals(KB.NOTEXISTSINVbs) || head[1].equals(KB.EXISTSINVbs)) {
				typeToEnforce = Schema.getRelationRange(source, head[0]);
			}
			
			if (typeToEnforce != null) {
				ByteString[] newEdge = rule.fullyUnboundTriplePattern();
				newEdge[0] = rule.getFunctionalVariable();
				newEdge[1] = Schema.typeRelationBS;
				newEdge[2] = typeToEnforce;
				rule.getTriples().add(newEdge);
				long support = kb.countDistinct(rule.getFunctionalVariable(), rule.getTriples());
				rule.getTriples().remove(rule.getTriples().size() - 1);
				if (support >= minSupportThreshold) {
					Rule newRule = rule.addAtom(newEdge, support);
					newRule.addParent(rule);
					output.add(newRule);
				}
			}
		}
	}
}
