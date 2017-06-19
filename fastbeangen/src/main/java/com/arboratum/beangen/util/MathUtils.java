package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import com.google.common.base.Stopwatch;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.IntStream;

import static com.arboratum.beangen.BaseBuilders.aString;
import static java.lang.Math.min;

/**
 * Created by gpicron on 17/06/2017.
 */
public class MathUtils {


    /**
     * Knuth shuffles (in place, modifies the array element order randomly)
     *
     *
     * @param array
     * @param randomSequence
     */
    public static void randomPermutation(int[] array, RandomSequence randomSequence) {
        final int n = array.length;

        for (int i = 0; i < n-1; i++) {
            final int j = randomSequence.nextInt(n - i);
            swap(array, i, i+j);
        }
    }




    private static void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }


    public static IntSequence[] randomSubsetSum(int[] weights, int total, int numUniquewanted, final RandomSequence randomSequence) {
        // dynamic programming indexing structure, weights sorted
        final int[][] index = new int[total+1][];
        index[0] = new int[0];
        int minWeight = Integer.MAX_VALUE;
        for (int subtotal = 1; subtotal < index.length; subtotal++) {
            RoaringBitmap possibilities = new RoaringBitmap();
            for (int i = 0; i < weights.length; i++) {
                final int weight = weights[i];

                if (weight > subtotal) break; // stop searching, all the rest is too big

                if (weight == subtotal) {
                    possibilities.add(i);
                } else if (index[subtotal-weight].length != 0) {
                    possibilities.add(i);
                }
            }
            final int cardinality = possibilities.getCardinality();
            index[subtotal] = possibilities.toArray();
            if (cardinality > 0) {
                minWeight = min(minWeight, weights[possibilities.first()]);
            }
        }

        if (index[total].length == 0) return new IntSequence[0]; // no possible solution

        LinkedHashSet<IntSequence> results = new LinkedHashSet<>(numUniquewanted);

        final int[] buffer = new int[total / minWeight];

        int found = 0;

        for (int trial = 0; trial < (numUniquewanted * 2) && found < numUniquewanted; trial++) {
            int i = 0;
            int S = total;
            for (; i < buffer.length && S > 0; i++) {
                final int[] map = index[S];
                final int weigthI = map[randomSequence.nextInt(map.length)];
                buffer[i] = weigthI;
                S -= weights[weigthI];
            }

            if (results.add(new IntSequence(buffer, 0, i))) found++;
        }
        return results.toArray(new IntSequence[results.size()]);
    }



    public static void main(String[] args) {
        final Generator<String> build = aString().withCharactersAnd("ABCDE").uniformLength(4, 12).build();
        final String[] A = IntStream.range(0, 20).mapToObj(i -> build.generate(i)).toArray(String[]::new);
        Arrays.sort(A, Comparator.comparingInt(String::length));
        final int[] lengths = Arrays.stream(A).mapToInt(String::length).map(i -> i+1).toArray();

        for (int total = 55; total <= 55; total++) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            final int distinct = 50000;
            final IntSequence[] result = randomSubsetSum(lengths, total+1, distinct, new RandomSequence(31));

            System.out.println(result.length);
            System.out.println(stopwatch.stop());
            stopwatch.reset(); stopwatch.start();

            final IntSequence[] result2 = randomSubsetSum(lengths, total+1, distinct, new RandomSequence(1));

            System.out.println(result2.length);


            System.out.println(stopwatch.stop());

        }


    }


}
