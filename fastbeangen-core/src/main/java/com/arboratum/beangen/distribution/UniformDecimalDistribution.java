package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;

import java.util.function.ToDoubleFunction;

/**
 * Created by gpicron on 08/08/2016.
 */
public class UniformDecimalDistribution<VALUE extends Number> extends DecimalDistribution<VALUE> {
    private boolean hasBound;
    private double lower;
    private double upper;

    public UniformDecimalDistribution(VALUE minInclusive, VALUE maxInclusive, Class<VALUE> fieldType) {
        super(fieldType);

        hasBound = false;
        if (minInclusive != null) {
            lower = minInclusive.longValue();
            hasBound = true;
        } else {
            lower = min();
        }

        if (maxInclusive != null) {
            upper = maxInclusive.longValue();
            hasBound = true;
        } else {
            upper = max();
        }
    }

    private double max() {
        switch (type) {
            case FLOAT: return Float.MAX_VALUE;
            default: return Double.MAX_VALUE;
        }
    }
    private double min() {
        switch (type) {
            case FLOAT: return -Float.MAX_VALUE;
            default: return -Double.MAX_VALUE;
        }
    }

    @Override
    protected ToDoubleFunction<RandomSequence> buildDoubleFunction() {
        if (hasBound) {
                return seq -> {
                    final double u = seq.nextDouble();
                    return u * upper + (1 - u) * lower;
                };
        } else {
            return RandomSequence::nextDouble;
        }

    }
}
