package com.arboratum.beangen.util;

import org.junit.Test;
import reactor.core.publisher.Flux;

import java.util.Arrays;

import static com.arboratum.beangen.util.SubSetSumProblemWithRepetition.findPossibleContatenationOfWords;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by gpicron on 09/06/2017.
 */
public class SubSetSumProblemWithRepetitionTest {


    @Test
    public void nominal() {
        int[] lengths = {3, 4, 5, 3, 7, 8, 9, 10, 11, 12, 8, 14, 15, 11, 18, 19, 20, 23};
        int totalLength = 55;

        Flux<int[]> solutions = findPossibleContatenationOfWords(lengths, totalLength, new RandomSequence(2)).take(2000);


        solutions = solutions.doOnNext(s -> {
            System.out.println(Arrays.toString(s));
            assertEquals(55+1, Arrays.stream(s).map(i -> lengths[i]+1).sum());
        });

        assertEquals(2000, solutions.count().block().longValue());
    }


    @Test
    public void shortResult() {
        int[] lengths = {3, 4, 3};
        int totalLength = 7;

        Flux<int[]> solutions = findPossibleContatenationOfWords(lengths, totalLength, new RandomSequence(2)).take(2000);

        final int[][] ints = solutions.toStream().toArray(int[][]::new);


        assertEquals(4, ints.length);
        assertThat(ints[0], equalTo(new int[] {2, 2}));
        assertThat(ints[1], equalTo(new int[] {0, 0}));
        assertThat(ints[2], equalTo(new int[] {2, 0}));
        assertThat(ints[3], equalTo(new int[] {0, 2}));
    }


}