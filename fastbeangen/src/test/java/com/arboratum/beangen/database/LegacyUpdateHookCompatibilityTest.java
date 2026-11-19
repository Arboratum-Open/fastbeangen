package com.arboratum.beangen.database;

import com.arboratum.beangen.util.RandomSequence;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LegacyUpdateHookCompatibilityTest {
    @Test
    public void referenceAwareDefaultRetainsTheExistingOneArgumentOverride() {
        AtomicInteger calls = new AtomicInteger();
        UpdateGenerator<Integer> legacy = new UpdateGenerator<Integer>() {
            @Override
            public boolean canApplyOn(Integer value) {
                assertEquals(Integer.valueOf(17), value);
                calls.incrementAndGet();
                return false;
            }

            @Override
            public UpdateOf<Integer> generate(Integer previous, RandomSequence random) {
                throw new AssertionError("Rejected updates must not be generated");
            }
        };
        assertFalse(legacy.canApplyOn(17, null));
        assertEquals(1, calls.get());
    }
}
