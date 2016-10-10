package com.arboratum.beangen.core;

import com.arboratum.beangen.distribution.CommonsMathDecimalDistribution;
import com.arboratum.beangen.distribution.UniformDecimalDistribution;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.math3.distribution.RealDistribution;

/**
 * Created by gpicron on 08/08/2016.
 */
public class DecimalGeneratorBuilder<VALUE extends Number> extends AbstractGeneratorBuilder<VALUE> {

    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            Float.TYPE, Float.class,
            Double.TYPE, Double.class
    );

    public DecimalGeneratorBuilder(Class<VALUE> fieldType) {
        super(fieldType);
    }


    public DecimalGeneratorBuilder<VALUE> uniform() {
        setup(new UniformDecimalDistribution<>(null, null, fieldType).build());
        return this;
    }

    public DecimalGeneratorBuilder<VALUE> uniform(VALUE min, VALUE max) {
        setup(new UniformDecimalDistribution<>(min, max, fieldType).build());
        return this;
    }

    public DecimalGeneratorBuilder<VALUE> distribution(RealDistribution distribution) {
        setup(new CommonsMathDecimalDistribution<VALUE>(distribution, null, fieldType).build());
        return this;
    }

}
