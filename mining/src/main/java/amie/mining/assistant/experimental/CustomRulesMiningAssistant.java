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
import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class CustomRulesMiningAssistant extends DefaultMiningAssistant {

    protected Rule ruleParser(String s) {
        Rule candidate = AMIEParser.rule(s);
        candidate.setSupport(this.kb.countDistinctPairs(candidate.getHead()[0], candidate.getHead()[2], candidate.getTriples()));
        candidate.setFunctionalVariablePosition(0);
        registerHeadRelation(candidate);
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
	return candidate.isClosed(false);
    }
    
    @Override
    public Collection<Rule> getInitialAtoms(double minSupportThreshold) {
        Collection<Rule> output = new ArrayList<>();
        output.add(ruleParser(
                "skos:prefLabel(?x, ?_y) & skos:prefLabel(?x, ?_z)"
                + " & <label@>(?_y, ?_lbl1) & <label@>(?_z, ?_lbl2)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => equals(?_lbl1, ?_lbl2)"));
        output.add(ruleParser(
                "skos:prefLabel(?x, ?_y) & skos:prefLabel(?x, ?_z)"
                + " & <label@>(?_y, ?_lbl1)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => <label@>(?_z, ?_lbl1)"));
        output.add(ruleParser(
                "skos:prefLabel(?x, ?_y)"
                + " & <label@>(?_y, ?_lbl1) & <label@>(?_z, ?_lbl1)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => skos:prefLabel(?x, ?_z)"));
        output.add(ruleParser(
                "rdfs:label(?x, ?_y) & rdfs:label(?x, ?_z)"
                + " & <label@>(?_y, ?_lbl1) & <label@>(?_z, ?_lbl2)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => equals(?_lbl1, ?_lbl2)"));
        output.add(ruleParser(
                "rdfs:label(?x, ?_y) & rdfs:label(?x, ?_z)"
                + " & <label@>(?_y, ?_lbl1)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => <label@>(?_z, ?_lbl1)"));
        output.add(ruleParser(
                "rdfs:label(?x, ?_y)"
                + " & <label@>(?_y, ?_lbl1) & <label@>(?_z, ?_lbl1)"
                + " & <@lang>(?_y, ?lg1) & <@lang>(?_z, ?lg2)"
                + " & differentFrom(?lg1, ?lg2)"
                + " => rdfs:label(?x, ?_z)"));
        return output;
    }
    
}
