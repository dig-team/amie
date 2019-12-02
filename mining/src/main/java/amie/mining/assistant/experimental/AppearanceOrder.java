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
public class AppearanceOrder implements VariableOrder {

    public AppearanceOrder() {}
    
    @Override
    public ByteString getFirstCountVariable(Rule r) {
        ByteString[] head = r.getHead();
        return head[KB.firstVariablePos(head)];
    }

    @Override
    public ByteString getSecondCountVariable(Rule r) {
        ByteString[] head = r.getHead();
        return head[KB.secondVariablePos(head)];
    }
}
