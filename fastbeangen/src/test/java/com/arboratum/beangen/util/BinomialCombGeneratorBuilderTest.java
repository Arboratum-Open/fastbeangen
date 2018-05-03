package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gpicron.
 */
public class BinomialCombGeneratorBuilderTest {
    @Test
    public void build() throws Exception {
        BinomialCombGeneratorBuilder builder =  new BinomialCombGeneratorBuilder("test");
        builder.addBinomial("p1", 0.1);
        builder.addBinomial("p2",0.2);

        Generator<boolean[]> generator = builder.build();

        double[] stat = new double[2];

        for (int i = 0; i < 1000000; i++) {
            boolean[] generated = generator.generate(i);
            for (int j = 0; j < stat.length; j++) {
                if (generated[j]) stat[j] += 1;
            }
        }

        for (int j = 0; j < stat.length; j++) {
            stat[j] /= 1000000;

            Assert.assertEquals("blended proba for " + j, 0.1 * (j+1), stat[j], 0.001);
        }


    }
    @Test
    public void buildExcludeFalseTrue() throws Exception {
        BinomialCombGeneratorBuilder builder =  new BinomialCombGeneratorBuilder("test");
        builder.addBinomial("p1",0.2);
        builder.addBinomial("p2",0.2);
        builder.excludeCombination(false, true);

        Generator<boolean[]> generator = builder.build();

        double[] stat = new double[2];

        for (int i = 0; i < 100000; i++) {
            boolean[] generated = generator.generate(i);

            Assert.assertThat(generated, Matchers.not(Matchers.equalTo(new boolean[]{false,true})));

            for (int j = 0; j < stat.length; j++) {
                if (generated[j]) stat[j] += 1;
            }
        }

        for (int j = 0; j < stat.length; j++) {
            stat[j] /= 100000;

            Assert.assertEquals("blended proba for " + j, stat[j], 0.2, 0.001);
        }


    }

    @Test
    public void buildExcludeSome() throws Exception {
        BinomialCombGeneratorBuilder builder =  new BinomialCombGeneratorBuilder("test");
        builder.addBinomial("p1",0.1);
        builder.addBinomial("p2",0.2);
        builder.addBinomial("p3",0.3);
        builder.excludeCombination(false, true, true);
        builder.excludeCombination(true, false, true);

        Generator<boolean[]> generator = builder.build();

        double[] stat = new double[3];

        for (int i = 0; i < 100000; i++) {
            boolean[] generated = generator.generate(i);

            Assert.assertThat(generated, Matchers.not(Matchers.equalTo(new boolean[]{false, true, true})));
            Assert.assertThat(generated, Matchers.not(Matchers.equalTo(new boolean[]{true, false, true})));

            for (int j = 0; j < stat.length; j++) {
                if (generated[j]) stat[j] += 1;
            }
        }

        for (int j = 0; j < stat.length; j++) {
            stat[j] /= 100000;

            Assert.assertEquals("blended proba for " + j, 0.1 * (j+1), stat[j], 0.01);
        }


    }
}