package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import com.google.common.collect.Streams;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by gpicron on 17/06/2017.
 */
public class MathUtils {


    public static final Collector<Tuple2<Integer, Long>, ?, TreeMap<Integer, BitSet>> TO_INVERTED_INDEX = Collectors.groupingBy(
            Tuple2::getT1,
            TreeMap::new,
            Collector.of(
                    BitSet::new,
                    (bitSet, o) -> bitSet.set(o.getT2().intValue()),
                    (bitSet, bitSet2) -> {
                        bitSet.or(bitSet2);
                        return bitSet;
                    }));

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


    public static Generator<IntSequence> randomSubSetSum(int total, int numUniquewanted, RandomSequence randomSequence, SubSetSumIndex subSetSumIndex) {

        if (subSetSumIndex.getIndex()[total].length == 0) return new Generator<IntSequence>(IntSequence.class) {
            @Override
            public IntSequence generate(RandomSequence register) {
                return null;
            }
        };



        return new Generator<IntSequence>(IntSequence.class) {
            private ArrayList<IntSequence> cachedSequences = new ArrayList<>();
            private HashSet<IntSequence> cachedSequencesSet = new HashSet<>();
            private int cached = 0;
            private boolean noMore = false;

            @Override
            public IntSequence generate(RandomSequence register) {
                if (noMore && cached == 0) return null;

                final int nth = register.nextInt((noMore) ? cached : numUniquewanted);

                if (nth < cached) {
                    return cachedSequences.get(nth);
                } else {
                    IntSequence e = null;
                    final int[] buffer = new int[total];
                    for (int trial = 0; trial < (numUniquewanted * 10) && cached <= nth; trial++) {
                        int i = 0;
                        int S = total;
                        for (; i < buffer.length && S > 0; i++) {
                            final int[] map = subSetSumIndex.getIndex()[S];
                            final int weigthI = map[randomSequence.nextInt(map.length)];
                            buffer[i] = weigthI;
                            S -= subSetSumIndex.getWeights()[weigthI];
                        }

                        e = new IntSequence(buffer, 0, i);
                        if (cachedSequencesSet.add(e)) {
                            cached++;
                            cachedSequences.add(e);
                        }
                    }
                    if (e == null) {
                        noMore = true;
                        if (cached > 0) {
                            cachedSequences.get(nth % cached);
                        } else {
                            return null;
                        }

                    }
                    return e;
                }
            }
        };
    }


    public static class SubSetSumIndex {
        private int[] weights;
        private int total = 0;
        private int[][] index;

        public SubSetSumIndex(int[] weights) {
            this.weights = weights;
        }

        public int[][] getIndex() {
            return index;
        }

        public SubSetSumIndex rebuild(int total) {
            if (total <= this.total) return this;
            // index of weights by value
            final TreeMap<Integer, BitSet> invertedIndexByWeightValue = Streams.mapWithIndex(Arrays.stream(weights), Tuples::of)
                    .collect(TO_INVERTED_INDEX);

            // dynamic programming indexing structure, weights sorted
            int[][] index = new int[total + 1][];
            index[0] = new int[0];

            for (int subtotal = 1; subtotal < index.length; subtotal++) {
                BitSet possibilities = new BitSet();
                for (Map.Entry<Integer, BitSet> e : invertedIndexByWeightValue.entrySet()) {
                    final int weight = e.getKey();

                    if (weight > subtotal) break; // stop searching, all the rest is too big

                    final BitSet correspondingElements = e.getValue();

                    if (weight == subtotal) {
                        possibilities.or(correspondingElements);
                    } else if (index[subtotal-weight].length != 0) {
                        possibilities.or(correspondingElements);
                    }
                }
                final int cardinality = possibilities.cardinality();
                index[subtotal] = possibilities.stream().toArray();
            }

            this.index = index;
            this.total = total;

            return this;
        }

        public int[] getWeights() {
            return weights;
        }
    }
}
