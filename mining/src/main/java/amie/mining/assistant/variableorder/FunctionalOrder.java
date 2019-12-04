/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.variableorder;

import amie.rules.Rule;

/**
 *
 * @author jlajus
 */
public class FunctionalOrder implements VariableOrder {
    
    public FunctionalOrder() {}

    @Override
    public int getFirstCountVariable(Rule r) {
        return r.getFunctionalVariable();
    }

    @Override
    public int getSecondCountVariable(Rule r) {
        return r.getNonFunctionalVariable();
    }
    
}
