package com.arboratum.beangen.distribution;

import com.arboratum.beangen.util.RandomSequence;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Created by gpicron on 08/08/2016.
 */
public abstract class DecimalDistribution<VALUE extends Number> {
    private final Class<VALUE> fieldType;
    protected final int type;

    protected static final int FLOAT = 0;
    protected static final int DOUBLE = 1;

    protected DecimalDistribution(Class<VALUE> fieldType) {
        this.fieldType = fieldType;
        if (fieldType == Double.TYPE || fieldType == Double.class) {
            type = DOUBLE;
        } else if (fieldType == Float.TYPE || fieldType == Float.class) {
            type = FLOAT;
        } else {
            throw new IllegalArgumentException("Invalid type");
        }
    }

    public Function<RandomSequence, VALUE> build() {
        ToDoubleFunction<RandomSequence> toDoubleFunction = buildDoubleFunction();
        switch (type) {
            case FLOAT:
                return seq -> (VALUE)Float.valueOf((float) toDoubleFunction.applyAsDouble(seq));

            case DOUBLE:
                return seq -> (VALUE)Double.valueOf(toDoubleFunction.applyAsDouble(seq));

        }
        throw new IllegalArgumentException("Invalid type");
    }

    protected abstract ToDoubleFunction<RandomSequence> buildDoubleFunction();

}
