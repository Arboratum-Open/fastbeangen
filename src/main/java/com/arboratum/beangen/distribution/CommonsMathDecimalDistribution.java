package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;

import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;

/**
 * Created by gpicron on 08/08/2016.
 */
public class CommonsMathDecimalDistribution<VALUE extends Number> extends DecimalDistribution<VALUE> {

    private final Double tabulateStep;
    private final org.apache.commons.math3.distribution.RealDistribution distribution;

    public CommonsMathDecimalDistribution(org.apache.commons.math3.distribution.RealDistribution distribution, Double tabulateStep, Class<VALUE> fieldType) {
        super(fieldType);
        this.tabulateStep = tabulateStep;
        this.distribution = distribution;
    }

    @Override
    protected ToDoubleFunction<RandomSequence> buildDoubleFunction() {
        if (tabulateStep != null) {

            final double step = tabulateStep.doubleValue();
            final double invStep = 1.0 / step;

            final double[] inverseCumulatedProbability = DoubleStream.iterate(0, s -> s + tabulateStep)
                    .map(s -> distribution.inverseCumulativeProbability(s))
                    .toArray();

            final int maxI = inverseCumulatedProbability.length;

            return (r) -> {
                int i = (int) Math.round(r.nextDouble() * invStep);
                if (i < 0) {
                    i = -i - 1;
                }
                if (i > maxI) i = maxI;

                return inverseCumulatedProbability[i];
            };
        } else {
            return (r) -> distribution.inverseCumulativeProbability(r.nextDouble());
        }
    }
}
