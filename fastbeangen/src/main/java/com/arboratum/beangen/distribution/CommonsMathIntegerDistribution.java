package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Created by gpicron on 08/08/2016.
 */
public strictfp class CommonsMathIntegerDistribution<VALUE extends Number> extends IntegerDistribution<VALUE> {

    private final boolean tabulated;
    private final org.apache.commons.math3.distribution.IntegerDistribution distribution;

    public CommonsMathIntegerDistribution(org.apache.commons.math3.distribution.IntegerDistribution distribution, boolean tabulate, Class<VALUE> fieldType) {
        super(fieldType);
        this.tabulated = tabulate;
        this.distribution = distribution;
    }


    @Override
    protected ToLongFunction<RandomSequence> buildLongFunction() {
        final ToIntFunction<RandomSequence> toIntFunction = buildIntFunction();
        return (r) -> toIntFunction.applyAsInt(r);
    }

    @Override
    protected ToIntFunction<RandomSequence> buildIntFunction() {
        if (tabulated) {
            List<Pair<Integer, Double>> cumProb = new ArrayList<>();
            double lastCum = 0;
            final int supportUpperBound = distribution.inverseCumulativeProbability(1d - RandomSequence.DOUBLE_STEP);
            for (int i = distribution.getSupportLowerBound(); i <= supportUpperBound; i++) {
                final double v = distribution.cumulativeProbability(i);
                if (v - lastCum < RandomSequence.DOUBLE_STEP) continue;
                lastCum = v;
                cumProb.add(new Pair<>(i, v));
            }

            final int[] inverseCumulatedProbability = new int[cumProb.size()];
            final double[] cumulatedProbalibity = new double[cumProb.size()];

            for (int i = 0; i < cumProb.size(); i++) {
                inverseCumulatedProbability[i] = cumProb.get(i).getFirst();
                cumulatedProbalibity[i] = cumProb.get(i).getSecond();
            }

            return (r) -> {
                int i = Arrays.binarySearch(cumulatedProbalibity, r.nextDouble());
                if (i < 0) {
                    i = -i - 1;
                }
                return inverseCumulatedProbability[i];
            };
        } else {
            return (r) -> distribution.inverseCumulativeProbability(r.nextDouble());
        }
    }
}
