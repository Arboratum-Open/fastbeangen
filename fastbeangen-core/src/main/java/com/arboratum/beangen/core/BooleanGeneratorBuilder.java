package com.arboratum.beangen.core;

import com.google.common.collect.ImmutableSet;

/**
 * Created by gpicron on 08/08/2016.
 */
public strictfp class BooleanGeneratorBuilder extends AbstractGeneratorBuilder<Boolean> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            Boolean.TYPE, Boolean.class
    );

    public BooleanGeneratorBuilder(Class fieldType) {
        super(fieldType);
    }

    public BooleanGeneratorBuilder uniform() {
        setup(r -> r.nextBoolean());

        return this;
    }

    public BooleanGeneratorBuilder bernoulli(double p) {
        if (p == 0.5) uniform();
        else if (p == 0) setup(r -> Boolean.FALSE);
        else if (p == 1) setup(r -> Boolean.TRUE);
        else setup(r -> r.nextDouble() < p);

        return this;
    }


}
