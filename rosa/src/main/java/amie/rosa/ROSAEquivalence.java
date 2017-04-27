package amie.rosa;

import amie.rules.Rule;
import javatools.datatypes.Pair;

public class ROSAEquivalence {

	public Rule rule1;
	
	public Rule rule2;
	
	public long intersection;
	
	public long union;
	
	public String prefix1;
	
	public String prefix2;
	
	public ROSAEquivalence(Rule rule1, Rule rule2, long intersection, long union) {
		this.rule1 = rule1;
		this.rule2 = rule2;
		this.intersection = intersection;
		this.union = union;
	}
	
	public double getJaccard() {
		return (double) intersection / union;
	}
	
	public double getConfidence() {
		return Math.min(rule1.getPcaConfidence(), rule2.getPcaConfidence());
	}
	
	public Pair<String, String> getRelations() {
		String relation1, relation2;
		if (rule1.getHeadRelation().startsWith(prefix1) 
				&& rule1.getBody().get(0)[1].toString().startsWith(prefix2)) {
			relation1 = rule1.getHeadRelation();			
			relation2 = rule1.getBody().get(0)[1].toString();
		} else {
			relation1 = rule1.getBody().get(0)[1].toString();
			relation2 = rule1.getHeadRelation();			
		}
		
		return new Pair<>(relation1, relation2);
	}
	
	
	public String toString() {
		Pair<String, String> relations = getRelations();
		return relations.second + "\t" + relations.first + "\t" + intersection 
				+ "\t" + union + "\t" + rule1.getPcaConfidence() + "\t" + rule2.getPcaConfidence()
				+ "\t" + getJaccard() + "\t" + getConfidence();
	}
	
	public String toShortString() {
		Pair<String, String> relations = getRelations();
		return relations.second + "\t" + relations.first + "\t" + intersection + "\t" + getConfidence();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
