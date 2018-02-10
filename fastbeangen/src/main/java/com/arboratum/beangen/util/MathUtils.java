package com.arboratum.beangen.util;

import com.arboratum.beangen.Generator;
import com.google.common.collect.Streams;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
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
            private volatile int cached = 0;
            private volatile boolean noMore = false;

            @Override
            public IntSequence generate(RandomSequence register) {
                if (noMore && cached == 0) return null;

                final int nth = register.nextInt((noMore) ? cached : numUniquewanted);

                if (nth < cached) {
                    return cachedSequences.get(nth);
                } else {
                    IntSequence e = null;
                    final int[] buffer = new int[total];
                    synchronized (cachedSequences) {
                        if (nth < cached) { // was increased in the meantime
                            return cachedSequences.get(nth);
                        } else {
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
                                    cachedSequences.add(e);
                                    cached++;
                                }
                            }

                            if (e == null) {
                                noMore = true;
                                cachedSequencesSet = null;
                                if (cached > 0) {
                                    return cachedSequences.get(nth % cached);
                                } else {
                                    return null;
                                }
                            } else {
                                if (cached == numUniquewanted) {
                                    noMore = true;
                                    cachedSequencesSet = null;
                                }
                                return e;
                            }
                        }
                    }
                }
            }
        };
    }


    public static class SubSetSumIndex {
        private int[] weights;
        private int total = 0;
        private int[][] index;
        private BigInteger[] counts;

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
            BigInteger[] counts = new BigInteger[total+1];
            counts[0] = BigInteger.ZERO;

            for (int subtotal = 1; subtotal < index.length; subtotal++) {
                BitSet possibilities = new BitSet();
                BigInteger count = BigInteger.ZERO;
                for (Map.Entry<Integer, BitSet> e : invertedIndexByWeightValue.entrySet()) {
                    final int weight = e.getKey();

                    if (weight > subtotal) break; // stop searching, all the rest is too big

                    final BitSet correspondingElements = e.getValue();

                    if (weight == subtotal) {
                        possibilities.or(correspondingElements);
                        count = count.add(BigInteger.valueOf(correspondingElements.cardinality()));
                    } else {
                        if (index[subtotal - weight].length != 0) {
                            possibilities.or(correspondingElements);
                        }
                        BigInteger subcount = counts[subtotal - weight];
                        if (!subcount.equals(BigInteger.ZERO)) {
                            count = count.add(BigInteger.valueOf(correspondingElements.cardinality()).multiply(subcount));
                        }
                    }
                }
                counts[subtotal] = count;
                index[subtotal] = possibilities.stream().toArray();
            }

            this.index = index;
            this.total = total;
            this.counts = counts;

            return this;
        }

        public int[] getWeights() {
            return weights;
        }

        public BigInteger[] getCounts() {
            return counts;
        }

        public Flux<IntSequence> allCombinations(int sum) {
            if (sum == 0) {
                return Flux.empty();
            } else {
                final int[] localWeights = this.weights;
                int[][] localIndex = this.index;
                return allCombRecursive(sum, localWeights, localIndex);
            }
        }

        private Flux<IntSequence> allCombRecursive(int sum, int[] localWeights, int[][] localIndex) {
            int[] localIndexAtSum = localIndex[sum];

            return Flux.range(0, localIndexAtSum.length)
                    .map(i -> new IntSequence(new int[] {localIndexAtSum[i]}, true))
                    .concatMap(left -> {
                                int weight = localWeights[left.intAt(0)];
                                if (weight == sum) {
                                    return Flux.just(left);
                                } else {
                                    return allCombRecursive(sum - weight, localWeights, localIndex).map(right -> left.concat(right));
                                }
                            }
                    );

        }

        public Flux<IntSequence> randomSolutions(RandomSequence randomSequence, int sum) {
            final int[][] index = this.getIndex();
            final int[] weights = this.getWeights();

            RandomSequence localSequence = new RandomSequence(randomSequence.nextLong(Long.MAX_VALUE));

            return Flux.generate(new Consumer<SynchronousSink<IntSequence>>() {
                final int[] buffer = new int[sum];
                @Override
                public void accept(SynchronousSink<IntSequence> synchronousSink) {
                    int i = 0;
                    int S = sum;
                    for (; i < buffer.length && S > 0; i++) {
                        final int[] map = index[S];
                        final int weigthI = map[localSequence.nextInt(map.length)];
                        buffer[i] = weigthI;
                        S -= weights[weigthI];
                    }
                    synchronousSink.next(new IntSequence(buffer, 0, i));
                }
            });

        }

    }
}
