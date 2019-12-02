/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.experimental;

import amie.data.KB;
import amie.rules.Rule;
import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class FunctionalOrder implements VariableOrder {
    
    public FunctionalOrder() {}

    @Override
    public ByteString getFirstCountVariable(Rule r) {
        return r.getFunctionalVariable();
    }

    @Override
    public ByteString getSecondCountVariable(Rule r) {
        return r.getNonFunctionalVariable();
    }
    
}
