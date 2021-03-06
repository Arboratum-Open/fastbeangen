package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import com.google.common.base.Stopwatch;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * Created by gpicron on 08/08/2016.
 */
public strictfp class RandomSequenceTest {

    public static final int COUNT_ITER = 1000000;

    @Test
    public void testDouble() {
        final RandomSequence randomSequence = new RandomSequence(31L);
        Assert.assertEquals(0.7439010164503713, randomSequence.nextDouble(), 0x1.0p-53);
        Assert.assertEquals(0.433157715909132, randomSequence.nextDouble(), 0x1.0p-53);

        Stopwatch stopwatch = Stopwatch.createStarted();

        Frequency freqs = new Frequency();
        for (int i = 0; i < COUNT_ITER; i++) {
            final double x = randomSequence.nextDouble();
            freqs.addValue(Math.floor(x * 100));
            Assert.assertTrue(x >= 0.0);
            Assert.assertTrue(x < 1.0);
        }
        System.out.println(freqs);
        System.out.println(stopwatch.stop());
    }

    @Test
    public void testIntN() {
        final RandomSequence randomSequence = new RandomSequence(31L);
        Assert.assertEquals(86, randomSequence.nextInt(100));
        Assert.assertEquals(58, randomSequence.nextInt(100));


        Stopwatch stopwatch = Stopwatch.createStarted();
        Frequency freqs = new Frequency();
        for (int i = 0; i < COUNT_ITER; i++) {
            final int x = randomSequence.nextInt(100);
            freqs.addValue(x);
            Assert.assertTrue(x >= 0);
            Assert.assertTrue(x < 100);
        }
        System.out.println(freqs);
        System.out.println(stopwatch.stop());
    }

    @Test
    public void testLongN() {
        final RandomSequence randomSequence = new RandomSequence(31L);
        Assert.assertEquals(69, randomSequence.nextLong(100));
        Assert.assertEquals(57, randomSequence.nextLong(100));

        Stopwatch stopwatch = Stopwatch.createStarted();
        Frequency freqs = new Frequency();
        for (int i = 0; i < COUNT_ITER; i++) {
            final long x = randomSequence.nextLong(16L);
            freqs.addValue(x);
            Assert.assertTrue(x >= 0);
            Assert.assertTrue(x < 16L);
        }
        System.out.println(freqs);
        System.out.println(stopwatch.stop());
    }


    @Test
    public void testRandomWalk() {
        final RandomSequence randomSequence = new RandomSequence(31L);
        final IntStream intStream = randomSequence.randomWalk(10, 15);

        Assert.assertArrayEquals(new int[]{11,13,10,12,14},intStream.toArray());
    }
}