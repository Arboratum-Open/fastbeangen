package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.google.common.io.Resources;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static InputStream getResourceAsStream(String resource) throws IOException {
        final InputStream resourceAsStream = Resources.getResource(EnumeratedDistributionGeneratorBuilder.class, resource).openStream();
        if (resourceAsStream == null) throw new IllegalArgumentException("Resource not found : " + resource);
        return resourceAsStream;
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

                setup(randomSequence -> {
                    int index = Arrays.binarySearch(cumulativeProbabilities, randomSequence.nextDouble());
                    if (index < 0) {
                        index = -index - 1;
                    }
                    return fixValues[index];
                });
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

                setup(randomSequence -> {
                    int index = Arrays.binarySearch(cumulativeProbabilities, randomSequence.nextDouble());
                    if (index < 0) {
                        index = -index - 1;
                    }
                    return genValues[index].generate(randomSequence);
                });
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

    public EnumeratedDistributionGeneratorBuilder weights(double... weights) {
        this.weights = weights;

        return this;
    }
    public EnumeratedDistributionGeneratorBuilder weights(long... weights) {
        this.weights = Arrays.stream(weights).mapToDouble(l -> (double)l).toArray();

        return this;
    }


    public EnumeratedDistributionGeneratorBuilder values(CLASS... values) {
        this.values = values;

        return this;
    }

    public EnumeratedDistributionGeneratorBuilder from(Frequency frequency) {
        final int size = frequency.getUniqueCount();
        this.values = (CLASS[]) new Object[size];
        this.weights = new double[size];
        final Iterator<Map.Entry<Comparable<?>, Long>> entryIterator = frequency.entrySetIterator();
        for (int i = 0; i < size; i++) {
            final Map.Entry<Comparable<?>, Long> e = entryIterator.next();
            values[i] = (CLASS) e.getKey();
            weights[i] = e.getValue();
        }

        return this;
    }


    public EnumeratedDistributionGeneratorBuilder valuesFromCSVResource(String resource) {
        return valuesFromCSVResource(resource, v -> (CLASS) v);
    }

    public EnumeratedDistributionGeneratorBuilder valuesFromCSVResource(String resource, final Function<String, CLASS> keyToValueMapper) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResourceAsStream(resource), "UTF-8"))) {
            final List<Pair<String, Double>> pairs = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.split(","))
                    .map(tokens -> {
                        if (tokens.length == 2) {
                            return new Pair<>(tokens[0], Double.parseDouble(tokens[1]));
                        } else {
                            return new Pair<>(tokens[0], 1.0);
                        }
                    }).collect(Collectors.toList());

            if (pairs.stream().mapToDouble(p -> p.getSecond()).filter(value -> value != 1.0).findAny().isPresent()) {
                this.weights = pairs.stream().mapToDouble(p -> p.getSecond()).toArray();
            }

            this.values = (CLASS[]) pairs.stream().map(p -> p.getFirst()).map(keyToValueMapper).toArray();

            return this;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("invalid resource file :" + resource, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid resource file :" + resource, e);
        }
    }

    public EnumeratedDistributionGeneratorBuilder generators(AbstractGeneratorBuilder<? extends CLASS>... values) {
        this.valueBuilders = values;

        return this;
    }


}
