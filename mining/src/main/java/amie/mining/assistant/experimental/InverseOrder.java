/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.rules.Rule;
import javatools.datatypes.ByteString;

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
    public ByteString getFirstCountVariable(Rule r) {
        return originalOrder.getSecondCountVariable(r);
    }

    @Override
    public ByteString getSecondCountVariable(Rule r) {
        return originalOrder.getFirstCountVariable(r);
    }
    
    public static VariableOrder of(VariableOrder order) {
        return new InverseOrder(order);
    }
}
