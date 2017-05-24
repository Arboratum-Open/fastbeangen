package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;

import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

/**
 * Created by gpicron on 08/08/2016.
 */
public abstract class IntegerDistribution<VALUE extends Number> {
    private final Class<VALUE> fieldType;
    protected final int type;

    protected static final int BYTE = 0;
    protected static final int SHORT = 1;
    protected static final int INTEGER = 2;
    protected static final int LONG = 3;

    protected IntegerDistribution(Class<VALUE> fieldType) {
        this.fieldType = fieldType;
        if (fieldType == Byte.TYPE || fieldType == Byte.class) {
            type = BYTE;
        } else if (fieldType == Short.TYPE || fieldType == Short.class) {
            type = SHORT;
        } else if (fieldType == Integer.TYPE || fieldType == Integer.class) {
            type = INTEGER;
        } else if (fieldType == Long.TYPE || fieldType == Long.class) {
            type = LONG;
        } else {
            throw new IllegalArgumentException("Invalid type");
        }
    }

    public Function<RandomSequence, VALUE> build() {
        ToIntFunction<RandomSequence> toIntFunction;
        switch (type) {
            case BYTE:
                toIntFunction = buildIntFunction();
                return seq -> (VALUE)Byte.valueOf((byte) toIntFunction.applyAsInt(seq));

            case SHORT:
                toIntFunction = buildIntFunction();
                return seq -> (VALUE)Short.valueOf((short) toIntFunction.applyAsInt(seq));

            case INTEGER:
                toIntFunction = buildIntFunction();
                return seq -> (VALUE)Integer.valueOf(toIntFunction.applyAsInt(seq));

            case LONG:
                ToLongFunction<RandomSequence> toLongFunction = buildLongFunction();
                return seq -> (VALUE)Long.valueOf(toLongFunction.applyAsLong(seq));

        }
        throw new IllegalArgumentException("Invalid type");
    }

    protected abstract ToLongFunction<RandomSequence> buildLongFunction();

    protected abstract ToIntFunction<RandomSequence> buildIntFunction();
}
