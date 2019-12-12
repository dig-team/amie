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
public interface VariableOrder {
    public int getFirstCountVariable(Rule r);
    public int getSecondCountVariable(Rule r);
}
