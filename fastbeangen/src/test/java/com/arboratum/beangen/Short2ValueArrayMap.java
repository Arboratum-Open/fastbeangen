package com.arboratum.beangen;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Created by gpicron on 16/12/2016.
 */
public class Short2ValueArrayMap<VALUE> {
    private int size = 0;
    private int capacity = 1024;
    private short[] index = new short[capacity];
    private VALUE[] childs = (VALUE[]) new Object[capacity];

    protected VALUE getValue(int id) {
        final short l1 = (short) (id >> 16);
        final int i = Arrays.binarySearch(index, 0, size, l1);
        final VALUE child;
        if (i < 0) {
            child = null;
        } else {
            child = childs[i];
        }
        return child;
    }

    protected VALUE getOrInsert(short l1, Supplier<VALUE> factory) {
        VALUE child;
        final int i = Arrays.binarySearch(index, 0, size, l1);
        if (i < 0) { // (-(<i>insertion point</i>) - 1)
            final int insertAt = - (i + 1);

            prepareSlot(insertAt);

            index[insertAt] = l1;
            childs[insertAt] = child =  factory.get();

        } else {
            child = childs[i];
        }
        return child;
    }

    private void prepareSlot(int insertAt) {
        if (size == capacity) {
            capacity += 1024;
            final short[] targetIndex = new short[capacity];
            final VALUE[] targetChilds = (VALUE[]) new Object[capacity];
            System.arraycopy(index, 0, targetIndex, 0, insertAt);
            System.arraycopy(childs, 0, targetChilds, 0, insertAt);
            System.arraycopy(index, insertAt, targetIndex, insertAt+1, size - insertAt);
            System.arraycopy(childs, insertAt, targetChilds, insertAt+1, size - insertAt);
            index = targetIndex;
            childs = targetChilds;
        } else {
            System.arraycopy(index, insertAt, index, insertAt+1, size - insertAt);
            System.arraycopy(childs, insertAt, childs, insertAt+1, size - insertAt);
        }
    }

    protected VALUE putValue(short l1, VALUE value) {
        final VALUE child;
        final int i = Arrays.binarySearch(index, 0, size, l1);
        if (i < 0) { // (-(<i>insertion point</i>) - 1)
            final int insertAt = - (i + 1);
            prepareSlot(insertAt);

            index[insertAt] = l1;
            childs[insertAt] = value;
            child = null;
        } else {
            child = childs[i];
            childs[i] = value;
        }
        return child;
    }
}
