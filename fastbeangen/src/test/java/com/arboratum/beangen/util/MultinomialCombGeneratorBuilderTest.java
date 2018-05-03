package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.IntStream;

/**
 * @author gpicron.
 */
public class MultinomialCombGeneratorBuilderTest {
    @Test
    public void build() throws Exception {
        MultinomialCombGeneratorBuilder builder =  new MultinomialCombGeneratorBuilder("test");

        double [][] expected = {
                { 0.1, 0.3, 0.6 },
                { 0.2, 0.8 }
        };

        initBuilder(builder, expected);

        assertRespect(expected, builder.build());


    }

    private void initBuilder(MultinomialCombGeneratorBuilder builder, double[][] expected) {
        for (int i = 0; i < expected.length; i++) {
            String[] values = IntStream.range(0, expected[i].length)
                    .mapToObj(v -> "v" + v)
                    .toArray(String[]::new);

            builder.addMultinomial("p"+i, values, expected[i]);
        }
    }

    private void assertRespect(double[][] expected, final Generator<int[]> generator) {

        double[][] stat = new double[expected.length][];
        for (int i = 0; i < expected.length; i++) {
            stat[i] = new double[expected[i].length];
        }

        for (int i = 0; i < 1000000; i++) {
            int[] generated = generator.generate(i);
            for (int j = 0; j < stat.length; j++) {
                stat[j][generated[j]] += 1;
            }
        }
        for (int i = 0; i < stat.length; i++) {
            for (int j = 0; j < stat[i].length; j++) {
                stat[i][j] /= 1000000;
            }
        }

        for (int i = 0; i < stat.length; i++) {
            for (int j = 0; j < stat[i].length; j++) {
                Assert.assertEquals("blended proba for (" + i + ","  + j + ")", expected[i][j], stat[i][j], 0.001);
            }
        }
    }

    @Test
    public void buildExclude() throws Exception {

        MultinomialCombGeneratorBuilder builder =  new MultinomialCombGeneratorBuilder("test");

        double [][] expected = {
                { 0.1, 0.3, 0.6 },
                { 0.2, 0.8 }
        };

        initBuilder(builder, expected);

        builder.excludeCombination(1, 0);

        Generator<int[]> generator = builder.build();
        assertRespect(expected, generator);


        for (int i = 0; i < 1000000; i++) {
            int[] generated = generator.generate(i);
            Assert.assertThat(generated, Matchers.not(Matchers.equalTo(new int[]{1,0})));
        }

    }


}