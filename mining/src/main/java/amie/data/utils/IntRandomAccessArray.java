/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package amie.data.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author jlajus
 */
public class IntRandomAccessArray implements IntIterator {
    private IntArrayList array;
    private int i, size, ssize;
    private Random r;

    public IntRandomAccessArray(IntCollection c) {
        this(c, ThreadLocalRandom.current());
    }
    
    public IntRandomAccessArray(IntCollection c, int sampleSize) {
        this(c, sampleSize, ThreadLocalRandom.current());
    }
    
    public IntRandomAccessArray(IntCollection c, double sampleRatio) {
        this(c, sampleRatio, ThreadLocalRandom.current());
    }

    public IntRandomAccessArray(IntCollection c, Random r) {
        array = new IntArrayList(c);
        i = 0;
        size = array.size();
        ssize = size;
        this.r = r;
    }

    public IntRandomAccessArray(IntCollection c, int sampleSize, Random r) {
        this(c, r);
        ssize = sampleSize;
    }

    public IntRandomAccessArray(IntCollection c, double sampleRatio, Random r) {
        this(c, r);
        if (sampleRatio >= 0 && sampleRatio <= 1) {
            ssize = (int) Math.ceil(sampleRatio * size);
        } else {
            throw new IllegalArgumentException("The sample size ratio is expected to be between 0 and 1");
        }
    }

    public boolean hasNext() {
        return i < ssize;
    }

    public int pickOne() {
        if (i >= size) {
            return 0;
        }
        Collections.swap(array, i, i + r.nextInt(size - i));
        i += 1;
        return array.get(i - 1);
    }

    public void reset() {
        i = 0;
    }

    @Override
    public int nextInt() {
        if (hasNext()) {
            return pickOne();
        }
        throw new NoSuchElementException();
    }

}
