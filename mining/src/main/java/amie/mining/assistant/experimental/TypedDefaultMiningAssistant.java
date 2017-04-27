/**
 * @author lgalarra
 * @date Nov 25, 2012
 */
package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javatools.datatypes.ByteString;
import javatools.datatypes.IntHashMap;
import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningOperator;
import amie.rules.Rule;

/**
 * This class overrides the default mining assistant and adds to the rule
 * all possible types constraints on the head variables, that is, it mines
 * rules of the form B ^ is(x, C) ^ is(y, C') => rh(x, y).
 * @author lgalarra
 *
 */
public class TypedDefaultMiningAssistant extends DefaultMiningAssistant {

	/**
	 * @param dataSource
	 */
	public TypedDefaultMiningAssistant(KB dataSource) {
		super(dataSource);
	}
	
	@Override
	public String getDescription() {
        return "Counting on both head variables and using "
        		+ "all available data types [EXPERIMENTAL]";
	}
		
	/**
	 * Returns all candidates obtained by adding a new triple pattern to the query
	 * @param query
	 * @param minCardinality
	 * @return
	 */
	@MiningOperator(name="dangling")
	public void getDanglingAtoms(Rule query, double minCardinality, Collection<Rule> output){		
		if(query.getRealLength() == 1){
			//Add the types at the beginning of the query.
			getSpecializationCandidates(query, minCardinality, output);
		} else {
			super.getDanglingAtoms(query, minCardinality, output);	
		}
	}
		
	public void getSpecializationCandidates(Rule query, double minSupportThreshold, Collection<Rule> output) {
		List<Rule> tmpCandidates = new ArrayList<Rule>();
		ByteString[] head = query.getHead();
		
		//Specialization by type
		if(KB.isVariable(head[0])){
			ByteString[] newEdge = query.fullyUnboundTriplePattern();
			newEdge[0] = head[0];
			newEdge[1] = typeString;				
			query.getTriples().add(newEdge);
			IntHashMap<ByteString> subjectTypes = kb.countProjectionBindings(query.getHead(), query.getAntecedent(), newEdge[2]);
			if(!subjectTypes.isEmpty()){
				for(ByteString type: subjectTypes){
					int cardinality = subjectTypes.get(type);
					if(cardinality >= minSupportThreshold){
						Rule newCandidate = new Rule(query, cardinality);
						newCandidate.getLastTriplePattern()[2] = type;
						tmpCandidates.add(newCandidate);
					}
				}
			}
			
			query.getTriples().remove(query.getTriples().size() - 1);
			tmpCandidates.add(query);
		}
		
		if(KB.isVariable(head[2])){
			for(Rule candidate: tmpCandidates){
				ByteString[] newEdge = query.fullyUnboundTriplePattern();
				newEdge[0] = head[2];
				newEdge[1] = typeString;
				candidate.getTriples().add(newEdge);
				IntHashMap<ByteString> objectTypes = kb.countProjectionBindings(candidate.getHead(), candidate.getAntecedent(), newEdge[2]);
				if(!objectTypes.isEmpty()){
					for(ByteString type: objectTypes){
						int cardinality = objectTypes.get(type);
						if(cardinality >= minSupportThreshold){
							Rule newCandidate = new Rule(candidate, cardinality);
							newCandidate.getLastTriplePattern()[2] = type;
							newCandidate.addParent(query);
							output.add(newCandidate);
						}
					}
				}else{
					if(candidate != query){
						output.add(candidate);
						candidate.addParent(query);
					}
				}
				candidate.getTriples().remove(candidate.getTriples().size() - 1);
			}
		}
	}

	@Override
	protected boolean isNotTooLong(Rule candidate) {
		return candidate.getLengthWithoutTypes(typeString) < maxDepth;
	}
}