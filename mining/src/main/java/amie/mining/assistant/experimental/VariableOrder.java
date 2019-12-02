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
public interface VariableOrder {
    public ByteString getFirstCountVariable(Rule r);
    public ByteString getSecondCountVariable(Rule r);
}
