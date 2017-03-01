package com.arboratum.beangen.core;

import com.arboratum.beangen.distribution.CommonsMathIntegerDistribution;
import com.arboratum.beangen.distribution.UniformIntegerDistribution;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.math3.distribution.IntegerDistribution;

/**
 * Created by gpicron on 08/08/2016.
 */
public class IntegerGeneratorBuilder<VALUE extends Number> extends AbstractGeneratorBuilder<VALUE> {

    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            Byte.TYPE, Byte.class,
            Short.TYPE, Short.class,
            Integer.TYPE, Integer.class,
            Long.TYPE, Long.class
    );


    public IntegerGeneratorBuilder(Class<VALUE> fieldType) {
        super(fieldType);
    }

    public IntegerGeneratorBuilder<VALUE> uniform() {
        setup(new UniformIntegerDistribution<>(null, null, fieldType).build());
        return this;
    }

    public IntegerGeneratorBuilder<VALUE> uniform(VALUE min, VALUE max) {
        setup(new UniformIntegerDistribution<>(min, max, fieldType).build());
        return this;
    }




    public IntegerGeneratorBuilder<VALUE> distribution(IntegerDistribution distribution) {
        setup(new CommonsMathIntegerDistribution<VALUE>(distribution, true, fieldType).build());
        return this;
    }

}
