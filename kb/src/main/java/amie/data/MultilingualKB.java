/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data;

/**
 *
 * @author jlajus
 */
public class MultilingualKB extends KB {

    @Override
    protected boolean add(int subject, int relation, int object) {
        String[] split = unmap(object).split("@");
        if (split.length == 2) {
            super.add(object, map("<label>"), map(split[0]));
            super.add(object, map("<lang>"), map(split[1]));
        }
        return super.add(subject, relation, object);
    }
}
