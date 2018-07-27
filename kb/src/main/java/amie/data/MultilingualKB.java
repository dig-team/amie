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
public class MultilingualKB extends KB {

    @Override
    protected boolean add(ByteString subject, ByteString relation, ByteString object) {
        String[] split = object.toString().split("@");
        if (split.length == 2) {
            super.add(object, ByteString.of("<label@>"), ByteString.of(split[0]));
            super.add(object, ByteString.of("<@lang>"), ByteString.of(split[1]));
        }
        return super.add(subject, relation, object);
    }
}
