/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data.tuple;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 *
 * @author jlajus
 */
public class IntArrays {
    public static IntList asList(int... ar) {
        return new IntArrayList(ar);
    }
}
