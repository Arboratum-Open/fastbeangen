package com.arboratum.beangen.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gpicron on 13/02/2017.
 */
public class RandomSequenceTest {
    @Test
    public void nextInt() throws Exception {
        RandomSequence s = new RandomSequence(0);
        Assert.assertEquals(0, s.nextInt(1));
        Assert.assertEquals(0, s.nextInt(1));
        Assert.assertEquals(0, s.nextInt(1));
    }

}