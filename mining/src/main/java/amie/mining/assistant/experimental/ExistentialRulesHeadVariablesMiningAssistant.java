package amie.mining.assistant.experimental;

import java.util.ArrayList;
import java.util.List;

import javatools.datatypes.ByteString;
import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.rules.Rule;

/**
 * Extension of the default mining assistant that also reports existential rules,
 * i.e., rules where one of the head variables is allowed to be non-closed.
 * 
 * @author galarrag
 *
 */
public class ExistentialRulesHeadVariablesMiningAssistant extends
		DefaultMiningAssistant {

	public ExistentialRulesHeadVariablesMiningAssistant(KB dataSource) {
		super(dataSource);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getDescription() {
        return "Reporting also existential rules. "
        		+ "Counting on both head variables.";	
	}
	
	@Override
	public void calculateConfidenceMetrics(Rule candidate) {
		// TODO Auto-generated method stub
		List<ByteString[]> antecedent = new ArrayList<ByteString[]>();
		antecedent.addAll(candidate.getAntecedent());
		List<ByteString[]> succedent = new ArrayList<ByteString[]>();
		succedent.addAll(candidate.getTriples().subList(0, 1));
		double pcaDenominator = 0.0;
		double denominator = 0.0;
		ByteString[] head = candidate.getHead();
		ByteString[] existentialTriple = head.clone();
		int freeVarPos, countVarPos;
				
		if (!antecedent.isEmpty()){
			try{
				if(KB.numVariables(head) == 2){
					ByteString var1, var2;
					var1 = head[KB.firstVariablePos(head)];
					var2 = head[KB.secondVariablePos(head)];
					denominator = (double) computeBodySize(var1, var2, candidate);
				} else {					
					denominator = (double) kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
				}				
				candidate.setBodySize((long)denominator);
			}catch(UnsupportedOperationException e){
				
			}
			
			// In this case, still report the PCA.
			if (candidate.isClosed(true)) {				
				countVarPos = candidate.getFunctionalVariablePosition();
				if(KB.numVariables(existentialTriple) == 1){
					freeVarPos = KB.firstVariablePos(existentialTriple) == 0 ? 2 : 0;
				}else{
					freeVarPos = existentialTriple[0].equals(candidate.getFunctionalVariable()) ? 2 : 0;
				}
				existentialTriple[freeVarPos] = ByteString.of("?x");
				
				try{
					List<ByteString[]> redundantAtoms = Rule.redundantAtoms(existentialTriple, antecedent);
					boolean existentialQueryRedundant = false;
					
					//If the counting variable is in the same position of any of the unifiable patterns => redundant
					for(ByteString[] atom: redundantAtoms){
						if(existentialTriple[countVarPos].equals(atom[countVarPos]))
							existentialQueryRedundant = true;
					}
						
					if(existentialQueryRedundant){
						pcaDenominator = denominator;
					}else{
						if(KB.numVariables(head) == 2){
							ByteString var1, var2;
							var1 = head[KB.firstVariablePos(head)];
							var2 = head[KB.secondVariablePos(head)];
							pcaDenominator = (double) computePcaBodySize(var1, 
									var2, candidate, antecedent, existentialTriple, candidate.getFunctionalVariablePosition());
						}else{
							antecedent.add(existentialTriple);
							pcaDenominator = (double)kb.countDistinct(candidate.getFunctionalVariable(), antecedent);
						}			
					}
					
					candidate.setPcaBodySize((long)pcaDenominator);
				}catch(UnsupportedOperationException e){
					
				}
			}
		}
	}
	
	@Override
	public boolean testConfidenceThresholds(Rule candidate) {
		boolean addIt = true;
		
		if (candidate.getLength() == 1) {
			return false;
		}
		
		if (candidate.containsLevel2RedundantSubgraphs()) {
			return false;
		}
		
		calculateConfidenceMetrics(candidate);
		
		if (candidate.getStdConfidence() >= minStdConfidence && candidate.getPcaConfidence() >= minPcaConfidence) {
			//Now check the confidence with respect to its ancestors
			List<Rule> ancestors = candidate.getAncestors();			
			for (int i = ancestors.size() - 2; i >= 0; --i) {
				if (ancestors.get(i).isClosed(true) 
						&& 
						(candidate.getStdConfidence() <= ancestors.get(i).getStdConfidence() 
						|| candidate.getPcaConfidence() <= ancestors.get(i).getPcaConfidence())) {
					addIt = false;
					break;
				}
			}
		}else{
			return false;
		}
		
		return addIt;
	}
}
