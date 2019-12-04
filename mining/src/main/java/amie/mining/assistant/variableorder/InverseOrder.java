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
public class InverseOrder implements VariableOrder {
    
    private final VariableOrder originalOrder;
    private InverseOrder(VariableOrder order) {
        originalOrder = order;
    }

    @Override
    public int getFirstCountVariable(Rule r) {
        return originalOrder.getSecondCountVariable(r);
    }

    @Override
    public int getSecondCountVariable(Rule r) {
        return originalOrder.getFirstCountVariable(r);
    }
    
    public static VariableOrder of(VariableOrder order) {
        return new InverseOrder(order);
    }
}
