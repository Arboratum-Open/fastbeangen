package com.arboratum.beangen.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by gpicron on 21/11/2017.
 */
public class SubSetSumIndexTest {

    @Test
    public void allComb() {
        int[] weights = {5, 4, 3, 1, 1};
        MathUtils.SubSetSumIndex index = new MathUtils.SubSetSumIndex(weights);
        index.rebuild(13);
        System.out.println(Arrays.toString(index.getCounts()));
        for (int i = 1; i <= 13; i++) {
            System.out.println("total " + i);
            int expectedSum = i;
            long count = index.allCombinations(i).map(x -> {
                int sum = x.ints().map(in -> weights[in]).sum();
                Assert.assertEquals(expectedSum, sum);
                //System.out.println(x + " -> " + sum);
                return x;
            }).count().block();
            Assert.assertEquals(index.getCounts()[i].longValue(), count);
        }
    }

    @Test
    public void reservoirSampler() {
        int[] weights = {5, 4, 3, 1, 1};
        MathUtils.SubSetSumIndex index = new MathUtils.SubSetSumIndex(weights);
        index.rebuild(13);
        RandomSequence seq = new RandomSequence(1L);
        for (int i = 1; i <= 13; i++) {
            System.out.println("total " + i);
            List<IntSequence> sample = index.allCombinations(i).collect(new ReservoirSampler<>(seq, 5)).block();
            sample.forEach(System.out::println);
        }
    }


    @Test
    public void randomSolution() {
        int[] weights = {5, 4, 3, 1, 1};
        MathUtils.SubSetSumIndex index = new MathUtils.SubSetSumIndex(weights);
        index.rebuild(13);
        RandomSequence seq = new RandomSequence(1L);
        for (int i = 1; i <= 13; i++) {
            System.out.println("total " + i);

            for (int j = 0; j < 5; j++) {
                System.out.println(index.randomSolutions(seq, i).blockFirst());
            }

        }
    }


}