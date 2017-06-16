package com.arboratum.beangen.util;

import com.google.common.collect.Collections2;
import com.google.common.primitives.Ints;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * reference https://en.wikipedia.org/wiki/Subset_sum_problem
 *
 * Permit to find a list of concatenation of word for a given length
 *
 * Created by gpicron on 09/06/2017.
 */
public class SubSetSumProblemWithRepetition {

    public static Flux<int[]> findPossibleContatenationOfWords(int[] lengths, int totalLength, RandomSequence walker) {

        return SubSetSumProblemWithRepetition.subSetSum(0, lengths, totalLength + 1, new int[0], walker)
                .buffer(10000)  // a  bit of mixing combinations
                .flatMap(ints -> Flux.fromStream(walker.randomWalk(0, ints.size()).boxed()).map(ints::get));
/*
        return Flux.<int[]>create(sink -> {
            ;
            sink.complete();
        })
                 */
    }

    private static Flux<int[]> subSetSum(int start, int[] nums, int target, int[] currentSolution, RandomSequence walker) {
        if (target == 0) {
            final Collection<List<Integer>> permutations = Collections2.orderedPermutations(Arrays.stream(currentSolution).sorted().boxed().collect(Collectors.toList()));
            return Flux.fromIterable(permutations).map(integers -> Ints.toArray(integers));
        } else {
            return Flux.fromStream(walker.randomWalk(start, nums.length).boxed())

                    .flatMap(i -> {
                        final int spaceConsumed = nums[i] + 1; // add 1 for the space between words
                        if (spaceConsumed <= target) {
                            int[] nextSolution = Arrays.copyOf(currentSolution, currentSolution.length + 1); // new potential partial solution
                            nextSolution[currentSolution.length] = i;

                            return subSetSum(i, nums, target - spaceConsumed, nextSolution, walker);
                        } else {
                            return Flux.empty();
                        }
                    });
        }
    }
}
