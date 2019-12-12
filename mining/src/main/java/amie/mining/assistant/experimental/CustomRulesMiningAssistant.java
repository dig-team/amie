/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.mining.assistant.DefaultMiningAssistant;
import amie.mining.assistant.MiningAssistant;
import amie.rules.AMIEParser;
import amie.rules.Rule;
import java.util.ArrayList;
import java.util.Collection;


/**
 *
 * @author jlajus
 */
public class CustomRulesMiningAssistant extends DefaultMiningAssistant {
    
    private static final boolean _debug_ = true;

    protected Rule ruleParser(String s) {
        Rule candidate = AMIEParser.rule(s);
        candidate.setSupport(this.kb.countDistinctPairs(candidate.getHead()[0], candidate.getHead()[2], candidate.getTriples()));
        candidate.setFunctionalVariablePosition(0);
        registerHeadRelation(candidate);
        System.err.println(candidate.toString() + "\t" + Double.toString(candidate.getSupport()));
        return candidate;
    }
    
    public CustomRulesMiningAssistant(KB dataSource) {
        super(dataSource);
        this.maxDepth = 10;
    }
    
    @Override
    public void setMaxDepth(int maxAntecedentDepth) {
	this.maxDepth = Math.min(8, maxAntecedentDepth);
    }
    
    @Override
    public boolean shouldBeOutput(Rule candidate) {
	return _debug_ || candidate.isClosed(false);
    }
    
    @Override
    public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
        Collection<Rule> output = new ArrayList<>();
        output.add(ruleParser(
                "?x skos:prefLabel ?_y & ?x skos:prefLabel ?_z"
                + " & ?_y <label> ?_lbl1 & ?_z <label> ?_lbl2"
                + " & ?_y <lang> ?lg1 & ?_z <lang> ?lg2"
                + " & ?lg1 differentFrom ?lg2"
                + " => ?_lbl1 equals ?_lbl2"));
        output.add(ruleParser(
                "?x skos:prefLabel ?_y & ?x skos:prefLabel ?_z"
                + " & ?_y <label> ?_lbl1"
                + " & ?_y <lang> ?lg1 & ?_z <lang> ?lg2"
                + " & ?lg1 differentFrom ?lg2"
                + " => & ?_z <label> ?_lbl1"));
        output.add(ruleParser(
                "?x skos:prefLabel ?_y"
                + " & ?_y <label> ?_lbl1 & ?_z <label> ?_lbl2"
                + " & ?_y <lang> ?lg1 & ?_z <lang> ?lg2"
                + " & ?lg1 differentFrom ?lg2"
                + " => ?x skos:prefLabel ?_z"));
        output.add(ruleParser(
                "?x rdfs:label ?_y & ?x rdfs:label ?_z"
                + " & ?_y <label> ?_lbl1 & ?_z <label> ?_lbl2"
                + " & ?_y <lang> ?_lg1 & ?_z <lang> ?_lg2"
                + " & ?_lg1 differentFrom ?_lg2"
                + " => ?_lbl1 equals ?_lbl2"));
        output.add(ruleParser(
                "?x rdfs:label ?_y & ?x rdfs:label ?_z"
                + " & ?_y <label> ?_lbl1"
                + " & ?_y <lang> ?_lg1 & ?_z <lang> ?_lg2"
                + " & ?_lg1 differentFrom ?_lg2"
                + " => & ?_z <label> ?_lbl1"));
        output.add(ruleParser(
                "?x rdfs:label ?_y"
                + " & ?_y <label> ?_lbl1 & ?_z <label> ?_lbl2"
                + " & ?_y <lang> ?_lg1 & ?_z <lang> ?_lg2"
                + " & ?_lg1 differentFrom ?_lg2"
                + " => ?x rdfs:label ?_z"));
        return output;
    }
    
}
