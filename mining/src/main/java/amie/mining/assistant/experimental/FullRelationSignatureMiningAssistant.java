package amie.mining.assistant.experimental;

import java.util.Collection;


import amie.data.KB;
import amie.data.tuple.IntArrays;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;
import it.unimi.dsi.fastutil.ints.IntList;

public class FullRelationSignatureMiningAssistant extends DefaultMiningAssistant {

	public FullRelationSignatureMiningAssistant(KB dataSource) {
		super(dataSource);
		bodyExcludedRelations = IntArrays.asList(KB.map("<rdf:type>"));
	}
	
	@Override
	public String getDescription() {
        return "Rules of the form type(x, C) r(x, y) => type(y, C') "
        		+ "or type(y, C) r(x, y) => type(x, C')";
	}
	
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {		
		int[] newEdge = query.fullyUnboundTriplePattern();
		int rdfType = KB.map("rdf:type");
		
		if(query.isEmpty()){
			//Initial case
			newEdge[1] = rdfType;
			Rule candidate = new Rule(newEdge, minCardinality);
			candidate.setFunctionalVariablePosition(0);
			registerHeadRelation(candidate);
			getInstantiatedAtoms(candidate, null, 0, 2, minCardinality, output);
		} else if (query.getLength() == 1) {
			getDanglingAtoms(query, newEdge, minCardinality, output);
		} else if (query.getLength() == 2) {
			IntList variables = query.getOpenVariables();
			// There must be one
			newEdge[0] = variables.get(0);
			newEdge[1] = rdfType;
			Rule candidate = query.addAtom(newEdge, minCardinality);
			getInstantiatedAtoms(candidate, candidate, 0, 2, minCardinality, output);
		}
	}
	
	public void getClosingAtoms(Rule query, double minSupportThreshold, Collection<Rule> output) {}
}
