package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.distribution.UniformIntegerDistribution;
import com.arboratum.beangen.util.RandomSequence;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import java.util.function.Function;

/**
 * Created by gpicron on 08/08/2016.
 */
public class CollectionGeneratorBuilder<COL extends Collection<?>> extends AbstractGeneratorBuilder<COL> {

    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            List.class, Set.class
    );


    public CollectionGeneratorBuilder(Class<COL> fieldType) {
        super(fieldType);
    }


    public CollectionGeneratorBuilder<COL> of(int min, int max, AbstractGeneratorBuilder valueBuilder ) {
        return of(min, max, valueBuilder.build());
    }

    public CollectionGeneratorBuilder<COL> of(int min, int max, Generator valueGenerator) {
        final Function<RandomSequence, Integer> sizeGenerator = new UniformIntegerDistribution<>(min, max, Integer.TYPE).build();
        if (fieldType.isAssignableFrom(HashSet.class)) {
            setup(randomSequence -> {
                int size = sizeGenerator.apply(randomSequence);
                Set r = new HashSet(size);

                for (int i = 0; i < size; i++) {
                    r.add(valueGenerator.generate(randomSequence));
                }
                return (COL)r;
            });
        } else if (fieldType.isAssignableFrom(ArrayList.class)) {
            setup(randomSequence -> {
                int size = sizeGenerator.apply(randomSequence);
                ArrayList r = new ArrayList(size);

                for (int i = 0; i < size; i++) {
                    r.add(valueGenerator.generate(randomSequence));
                }
                return (COL)r;
            });

        }
        return this;
    }


}
