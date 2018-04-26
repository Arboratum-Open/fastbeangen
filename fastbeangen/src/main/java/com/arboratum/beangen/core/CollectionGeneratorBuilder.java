package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.math3.stat.Frequency;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by gpicron on 08/08/2016.
 */
public class CollectionGeneratorBuilder<VALUE, COL extends Collection<VALUE>> extends AbstractGeneratorBuilder<COL> {
    private static final Logger log = Logger.getLogger(CollectionGeneratorBuilder.class.getName());


    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(
            List.class, Set.class, SortedSet.class
    );


    public CollectionGeneratorBuilder(Class<COL> fieldType) {
        super(fieldType);
    }

    /**
     * Produces a collection with uniform size distribution between min and max (inclusive) of values generated by the valueGenerator
     *
     * @param min
     * @param max
     * @param valueGenerator
     * @return
     */
    public CollectionGeneratorBuilder<VALUE, COL> of(int min, int max, AbstractGeneratorBuilder<VALUE> valueGenerator ) {
        return of(min, max, valueGenerator.build());
    }

    /**
     * Produces a collection with uniform size distribution between min and max (inclusive) of values generated by the valueGenerator
     *
     * @param min
     * @param max
     * @param valueGenerator
     * @return
     */
    public CollectionGeneratorBuilder<VALUE, COL> of(int min, int max, Generator<VALUE> valueGenerator) {
        return of(BaseBuilders.aInteger().uniform(min, max).build(), valueGenerator);
    }

    /**
     * Produces a collection with size distribution given by the frequency table of values generated by the valueGenerator
     *
     * @param sizeFrequency a frequency with Integer as key
     * @param valueGenerator
     * @return
     */
    public CollectionGeneratorBuilder<VALUE, COL> of(Frequency sizeFrequency, AbstractGeneratorBuilder<VALUE> valueGenerator) {
        return of(BaseBuilders.enumerated(Integer.class).from(sizeFrequency).build(), valueGenerator.build());
    }

    /**
     * Produces a collection with size distribution given by the frequency table of values generated by the valueGenerator
     *
     * @param sizeFrequency a frequency with Integer as key
     * @param valueGenerator
     * @return
     */
    public CollectionGeneratorBuilder<VALUE, COL> of(Frequency sizeFrequency, Generator<VALUE> valueGenerator) {
        return of(BaseBuilders.enumerated(Integer.class).from(sizeFrequency).build(), valueGenerator);
    }

    /**
     * Produces a collection with size generated by the sizeGenerator between min and max (inclusive) of values generated by the valueGenerator
     *
     * @param sizeGenerator
     * @param valueGenerator
     * @return
     */
    public CollectionGeneratorBuilder<VALUE, COL> of(Generator<? extends Number> sizeGenerator, Generator<VALUE> valueGenerator) {
        if (fieldType.isAssignableFrom(LinkedHashSet.class)) {
            setup(randomSequence -> {
                int size = sizeGenerator.apply(randomSequence).intValue();
                Set r = new LinkedHashSet(size);

                for (int i = 0; r.size() < size; i++) {
                    r.add(valueGenerator.generate(randomSequence));

                    if (i > 10 * size) {
                        log.warning("Seems we cannot generate a value for the set that doesn't exist yet. Stop trying.");
                        break;
                    }
                }
                return (COL) r;
            });
        } else if (fieldType.isAssignableFrom(TreeSet.class)) {
            setup(randomSequence -> {
                int size = sizeGenerator.apply(randomSequence).intValue();
                Set r = new TreeSet();

                for (int i = 0; r.size() < size; i++) {
                    r.add(valueGenerator.generate(randomSequence));

                    if (i > 10 * size) {
                        log.warning("Seems we cannot generate a value for the set that doesn't exist yet. Stop trying.");
                        break;
                    }
                }
                return (COL)r;
            });
        } else if (fieldType.isAssignableFrom(ArrayList.class)) {
            setup(randomSequence -> {
                int size = sizeGenerator.apply(randomSequence).intValue();
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
