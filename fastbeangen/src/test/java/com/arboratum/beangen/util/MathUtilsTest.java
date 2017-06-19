package com.arboratum.beangen.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by gpicron on 17/06/2017.
 */
public class MathUtilsTest {

    @Test
    public void testRandomPermutation() {
        int[] p = new int[] {1,2,3};

        RandomSequence randomSequence = new RandomSequence(31);

        MathUtils.randomPermutation(p, randomSequence);
        assertThat(p, is(new int[] {2,3,1}));
        MathUtils.randomPermutation(p, randomSequence);
        assertThat(p, is(new int[] {2,3,1}));
        MathUtils.randomPermutation(p, randomSequence);
        assertThat(p, is(new int[] {1,2,3}));
        MathUtils.randomPermutation(p, randomSequence);
        assertThat(p, is(new int[] {3,2,1}));
        MathUtils.randomPermutation(p, randomSequence);
        assertThat(p, is(new int[] {1,2,3}));
    }

}