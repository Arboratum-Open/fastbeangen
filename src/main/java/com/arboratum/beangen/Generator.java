package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import lombok.Getter;

import java.io.Serializable;
import java.util.function.Function;
import java.util.function.LongFunction;

/**
 * Created by gpicron on 09/08/2016.
 */
public abstract class Generator<CLASS> implements Function<RandomSequence, CLASS>, LongFunction<CLASS>, Serializable {
    @Getter
    protected final Class<CLASS> type;

    protected Generator(Class<CLASS> type) {
        this.type = type;
    }

    public CLASS generate(long id) {
        RandomSequence register = new RandomSequence(id);
        return generate(register);
    }

    public abstract CLASS generate(RandomSequence register);


    @Override
    public CLASS apply(RandomSequence sequence) {
        return generate(sequence);
    }

    @Override
    public CLASS apply(long value) {
        return generate(value);
    }
}
