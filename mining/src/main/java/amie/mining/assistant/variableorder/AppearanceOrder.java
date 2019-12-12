/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.mining.assistant.variableorder;

import amie.data.KB;
import amie.rules.Rule;

/**
 *
 * @author jlajus
 */
public class AppearanceOrder implements VariableOrder {

    public AppearanceOrder() {}
    
    @Override
    public int getFirstCountVariable(Rule r) {
        int[] head = r.getHead();
        return head[KB.firstVariablePos(head)];
    }

    @Override
    public int getSecondCountVariable(Rule r) {
        int[] head = r.getHead();
        return head[KB.secondVariablePos(head)];
    }
}
