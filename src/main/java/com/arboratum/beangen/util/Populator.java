package com.arboratum.beangen.util;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by gpicron on 07/08/2016.
 */
public final class Populator<CLASS, VALUE>  implements Serializable {
    private final ValueAssigner assigner;
    private final Function<RandomSequence, VALUE> generator;

    public Populator(ValueAssigner<CLASS, VALUE> assigner, Function<RandomSequence, VALUE> generator) {
        this.assigner = assigner;
        this.generator = generator;
    }

    public void populate(CLASS result, RandomSequence register) {
        assigner.assign(result, generator.apply(register));
    }


}
