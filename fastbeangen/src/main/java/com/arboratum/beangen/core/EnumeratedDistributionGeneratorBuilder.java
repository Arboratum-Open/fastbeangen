package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.DistributionUtils;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.MathArrays;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by gpicron on 08/08/2016.
 */
public class EnumeratedDistributionGeneratorBuilder<CLASS> extends AbstractGeneratorBuilder<CLASS> {

    private double[] weights;
    private CLASS[] values;
    private AbstractGeneratorBuilder<? extends CLASS>[] valueBuilders;

    public EnumeratedDistributionGeneratorBuilder(Class<CLASS> fieldType) {
        super(fieldType);
    }



    @Override
    public Generator<CLASS> build() {
        if (!(values != null) ^ (valueBuilders != null))
            throw new IllegalArgumentException("either values or builders must be specified");


        if (values != null) {
            final CLASS[] fixValues = this.values.clone();
            final int len = values.length;

            if (weights != null) {
                if (weights.length != len)
                    throw new IllegalArgumentException("number of weights differ from the number of values");
                final double[] cumulativeProbabilities = buildCumProbs();


                if (weights.length == 1) {
                    final CLASS fixValue0 = fixValues[0];
                    setup(randomSequence -> fixValue0);
                } else if (weights.length == 2) {
                    final CLASS fixValue0 = fixValues[0];
                    final CLASS fixValue1 = fixValues[1];
                    final double p = cumulativeProbabilities[0];
                    setup(randomSequence -> randomSequence.nextDouble() < p ? fixValue0 : fixValue1);
                } else {
                    setup(randomSequence -> {
                        int index = Arrays.binarySearch(cumulativeProbabilities, randomSequence.nextDouble());
                        if (index < 0) {
                            index = -index - 1;
                        }
                        return fixValues[index];
                    });
                }
            } else {
                setup(randomSequence -> fixValues[randomSequence.nextInt(len)]);
            }
        } else if (valueBuilders != null) {
            final Generator<CLASS>[] genValues = Arrays.stream(valueBuilders).map(AbstractGeneratorBuilder::build).toArray(Generator[]::new);
            final int len = genValues.length;

            if (weights != null) {
                if (weights.length != len)
                    throw new IllegalArgumentException("number of weights differ from the number of values");
                final double[] cumulativeProbabilities = buildCumProbs();

                if (weights.length == 1) {
                    final Generator<CLASS> genValue0 = genValues[0];
                    setup(randomSequence -> genValue0.generate(randomSequence));
                } else if (weights.length == 2) {
                    final Generator<CLASS> genValue0 = genValues[0];
                    final Generator<CLASS> genValue1 = genValues[1];
                    final double p = cumulativeProbabilities[0];
                    setup(randomSequence -> {
                        final Generator<CLASS> gen = randomSequence.nextDouble() < p ? genValue0 : genValue1;
                        return gen.generate(randomSequence);
                    });
                } else {

                    setup(randomSequence -> {
                        int index = Arrays.binarySearch(cumulativeProbabilities, randomSequence.nextDouble());
                        if (index < 0) {
                            index = -index - 1;
                        }
                        return genValues[index].generate(randomSequence);
                    });
                }
            } else {
                setup(randomSequence -> genValues[randomSequence.nextInt(len)].generate(randomSequence));
            }
        }

        return super.build();
    }

    private double[] buildCumProbs() {
        final double[] cumulativeProbabilities = MathArrays.normalizeArray(weights, 1.0);

        double sum = 0;
        for (int i = 0; i < cumulativeProbabilities.length; i++) {
            sum += cumulativeProbabilities[i];
            cumulativeProbabilities[i] = sum;
        }
        return cumulativeProbabilities;
    }

    @Override
    protected void assertFieldTypeSupported() {

    }

    public EnumeratedDistributionGeneratorBuilder<CLASS> weights(double... weights) {
        this.weights = weights;

        return this;
    }
    public EnumeratedDistributionGeneratorBuilder<CLASS> weights(long... weights) {
        this.weights = Arrays.stream(weights).mapToDouble(l -> (double)l).toArray();

        return this;
    }


    public EnumeratedDistributionGeneratorBuilder<CLASS> values(CLASS... values) {
        this.values = values;

        return this;
    }
    public EnumeratedDistributionGeneratorBuilder<CLASS> from(Frequency frequency) {
        return from(frequency, v -> (CLASS) v);
    }

    public <K extends Comparable<K>> EnumeratedDistributionGeneratorBuilder<CLASS> from(Frequency frequency, final Function<K, CLASS> keyToValueMapper) {
        final int size = frequency.getUniqueCount();
        this.values = (CLASS[]) new Object[size];
        this.weights = new double[size];
        boolean hasWeights = false;
        final Iterator<Map.Entry<Comparable<?>, Long>> entryIterator = frequency.entrySetIterator();
        for (int i = 0; i < size; i++) {
            final Map.Entry<Comparable<?>,Long> e =  entryIterator.next();
            values[i] = keyToValueMapper.apply((K) e.getKey());
            final long value = e.getValue();

            if (value != 1L) hasWeights = true;

            weights[i] = value;
        }

        if (!hasWeights) weights = null;

        return this;
    }


    public EnumeratedDistributionGeneratorBuilder<CLASS> valuesFromCSVResource(String resource) {
        return valuesFromCSVResource(resource, v -> (CLASS) v);
    }

    public EnumeratedDistributionGeneratorBuilder<CLASS> valuesFromCSVResource(String resource, final Function<String, CLASS> keyToValueMapper) {
        final Frequency frequency = DistributionUtils.fromCsvResource(resource);

        return from(frequency, keyToValueMapper);
    }

    public EnumeratedDistributionGeneratorBuilder<CLASS> generators(AbstractGeneratorBuilder<? extends CLASS>... values) {
        this.valueBuilders = values;

        return this;
    }


}
