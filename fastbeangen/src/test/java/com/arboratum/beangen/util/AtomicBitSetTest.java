package com.arboratum.beangen.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author gpicron.
 */
public class AtomicBitSetTest {

    @Test
    public void simple() {
        AtomicBitSet atomicBitSet = new AtomicBitSet();

        atomicBitSet.set(2);
        atomicBitSet.set(10234);

        assertTrue(atomicBitSet.get(2));
        assertTrue(atomicBitSet.get(10234));
        assertFalse(atomicBitSet.get(10235));
        assertEquals(2, atomicBitSet.countSet());
        assertEquals(Integer.valueOf(2), atomicBitSet.getAllSetBits().get(0));
        assertEquals(Integer.valueOf(10234), atomicBitSet.getAllSetBits().get(1));
    }

}