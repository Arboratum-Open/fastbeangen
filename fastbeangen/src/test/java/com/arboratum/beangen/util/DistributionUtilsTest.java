package com.arboratum.beangen.util;

import org.apache.commons.math3.stat.Frequency;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * Created by gpicron on 23/03/2017.
 */
public class DistributionUtilsTest {
    @Test
    public void convert() throws Exception {
        final HashMap<Comparable<?>, Number> counts = new HashMap<>();
        counts.put("ABC", 7);
        counts.put("DEF", 8);
        final Frequency frequency = DistributionUtils.convert(counts);

        Assert.assertEquals(7, frequency.getCount("ABC"));
        Assert.assertEquals(8, frequency.getCount("DEF"));
        Assert.assertEquals(2, frequency.getUniqueCount());
    }

}