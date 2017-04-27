package amie.mining.assistant.experimental;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

public class FullRelationSignatureMiningAssistant extends DefaultMiningAssistant {

	public FullRelationSignatureMiningAssistant(KB dataSource) {
		super(dataSource);
		bodyExcludedRelations = Arrays.asList(ByteString.of("<rdf:type>"));
	}
	
	@Override
	public String getDescription() {
        return "Rules of the form type(x, C) r(x, y) => type(y, C') "
        		+ "or type(y, C) r(x, y) => type(x, C')";
	}
	
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output) {		
		ByteString[] newEdge = query.fullyUnboundTriplePattern();
		ByteString rdfType = ByteString.of("rdf:type");
		
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
			List<ByteString> variables = query.getOpenVariables();
			// There must be one
			newEdge[0] = variables.get(0);
			newEdge[1] = rdfType;
			Rule candidate = query.addAtom(newEdge, minCardinality);
			getInstantiatedAtoms(candidate, candidate, 0, 2, minCardinality, output);
		}
	}
	
	public void getClosingAtoms(Rule query, double minSupportThreshold, Collection<Rule> output) {
		return;
	}
}
