/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

import javatools.datatypes.ByteString;

/**
 *
 * @author jlajus
 */
public class SexismSimpleTypingKB extends SimpleTypingKB {
    @Override
    protected boolean add(int subject, int relation, int object) {
        if (relation.equals(Schema.typeRelationBS) || relation.equals(Schema.subClassRelationBS)) {
            return super.add(subject, relation, object);
        } else if (relation.equals(KB.map("<hasGender>"))) {
            return super.add(subject, object, relation);
        } else {
            return false;
        }
    }
}
