package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by gpicron on 15/12/2016.
 */
public class BooleanGeneratorBuilderTest {

    public static final int ITERATIONS = 1000000;

    @Test
    public void uniformIdemPotence() throws Exception {
        BooleanGeneratorBuilder builder = new BooleanGeneratorBuilder(Boolean.class);
        final Generator<Boolean> generator = builder.uniform().build();

        assertEquals(false, generator.generate(0));
        assertEquals(false, generator.generate(1));
        assertEquals(true, generator.generate(2));
        assertEquals(true, generator.generate(3));
        assertEquals(true, generator.generate(4));
        assertEquals(true, generator.generate(5));
    }

    @Test
    public void uniform() throws Exception {
        BooleanGeneratorBuilder builder = new BooleanGeneratorBuilder(Boolean.class);
        final Generator<Boolean> generator = builder.uniform().build();

        double count = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            if (generator.generate(i)) count++;
        }

        assertEquals(0.5, count / ITERATIONS, 0.001);
    }

    @Test
    public void bernoulli() throws Exception {
        for (double p = 0.0; p <= 1.0; p+=0.05) {
            testWithProba(p);
        }
    }

    private void testWithProba(final double p) {
        BooleanGeneratorBuilder builder = new BooleanGeneratorBuilder(Boolean.class);
        final Generator<Boolean> generator = builder.bernoulli(p).build();

        double count = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            if (generator.generate(i)) count++;
        }

        assertEquals(p, count/ ITERATIONS, 0.001);
    }

    @Test
    public void bernoulliIdemPotence() throws Exception {
        BooleanGeneratorBuilder builder = new BooleanGeneratorBuilder(Boolean.class);
        final Generator<Boolean> generator = builder.bernoulli(0.3).build();

        assertEquals(true, generator.generate(0));
        assertEquals(false, generator.generate(1));
        assertEquals(true, generator.generate(2));
        assertEquals(false, generator.generate(3));
        assertEquals(true, generator.generate(4));
        assertEquals(false, generator.generate(5));
    }

}