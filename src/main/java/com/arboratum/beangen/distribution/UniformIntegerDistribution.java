package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;

import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Created by gpicron on 08/08/2016.
 */
public class UniformIntegerDistribution<VALUE extends Number> extends IntegerDistribution<VALUE> {
    private boolean hasBound;
    private long lower;
    private long upper;
    private final long range;

    public UniformIntegerDistribution(VALUE minInclusive, VALUE maxInclusive, Class<VALUE> fieldType) {
        super(fieldType);

        hasBound = false;
        if (minInclusive != null) {
            lower = minInclusive.longValue();
            hasBound = true;
        } else {
            lower = minOf(fieldType);
        }

        if (maxInclusive != null) {
            upper = maxInclusive.longValue();
            hasBound = true;
        } else {
            upper = maxOf(fieldType);
        }

        range = upper - lower + 1;

    }

    private static long maxOf(Class<?> fieldType) {
        if (fieldType == Byte.TYPE || fieldType == Byte.class) {
            return Byte.MAX_VALUE;
        } else if (fieldType == Short.TYPE || fieldType == Short.class) {
            return Short.MAX_VALUE;
        } else if (fieldType == Integer.TYPE || fieldType == Integer.class) {
            return Integer.MAX_VALUE;
        } else if (fieldType == Long.TYPE || fieldType == Long.class) {
            return Long.MAX_VALUE;
        }
        throw new IllegalArgumentException("Non numeric type");
    }

    private static long minOf(Class<?> fieldType) {
        if (fieldType == Byte.TYPE || fieldType == Byte.class) {
            return Byte.MIN_VALUE;
        } else if (fieldType == Short.TYPE || fieldType == Short.class) {
            return Short.MIN_VALUE;
        } else if (fieldType == Integer.TYPE || fieldType == Integer.class) {
            return Integer.MIN_VALUE;
        } else if (fieldType == Long.TYPE || fieldType == Long.class) {
            return Long.MIN_VALUE;
        }
        throw new IllegalArgumentException("Non numeric type");
    }


    @Override
    protected ToLongFunction<RandomSequence> buildLongFunction() {
        if (hasBound) {
            if (range <=0) {
                return seq -> {
                    while (true) {
                        final long r = seq.nextLong();
                        if (r >= lower && r <= upper) {
                            return r;
                        }
                    }
                };
            } else {
                return seq -> lower + seq.nextLong(range);
            }
        } else {
            return RandomSequence::nextLong;
        }
    }

    @Override
    protected ToIntFunction<RandomSequence> buildIntFunction() {
        if (hasBound) {
            return seq -> (int)(lower + seq.nextLong(range));
        } else {
            return RandomSequence::nextInt;
        }
    }
}
