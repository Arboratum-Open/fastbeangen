package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.ToDoubleFunction;

/**
 * Created by gpicron on 10/08/2016.
 */
public class CommonsMathDecimalDistributionTest {


    @Test
    public void nominal() {
        final ToDoubleFunction<RandomSequence> randomSequenceToDoubleFunction = new CommonsMathDecimalDistribution<Double>(new NormalDistribution(0.0, 1.0), null, Double.TYPE).buildDoubleFunction();
        Assert.assertEquals(0.0, computeFor(randomSequenceToDoubleFunction, 0.5), 1e-20);
        Assert.assertEquals(-0.84162123, computeFor(randomSequenceToDoubleFunction, 0.2), 1e-8);
        Assert.assertEquals(0.84162123, computeFor(randomSequenceToDoubleFunction, 0.8), 1e-8);

    }

    private double computeFor(ToDoubleFunction<RandomSequence> randomSequenceToDoubleFunction, final double v) {
        return randomSequenceToDoubleFunction.applyAsDouble(new RandomSequence(0) {
            @Override
            public double nextDouble() {
                return v;
            }
        });
    }

}