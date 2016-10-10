package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Created by gpicron on 07/08/2016.
 */
public class GeneratorTest {
    public static final int ROUNDS = 10000;

    @Test
    public void generate() throws Exception {
        Generator<long[]> gen = new Generator<long[]>(long[].class) {
            @Override
            public long[] generate(RandomSequence register) {
                return IntStream.range(0,1024).mapToLong(value -> register.nextLong(1024)).toArray();
            }
        };

        Frequency freqs = new Frequency();
        for (int i = 0; i < ROUNDS; i++) {
            final long[] generate = gen.generate(i);
            for (long n : generate) {
                freqs.addValue(n);
            }
        }
        System.out.println(freqs);

        SummaryStatistics statistic = new SummaryStatistics();
        Iterator<Map.Entry<Comparable<?>, Long>> iter= freqs.entrySetIterator();
        for (Map.Entry<Comparable<?>, Long> v = iter.next(); iter.hasNext(); v = iter.next()) {
            statistic.addValue(v.getValue());
        }
        System.out.println(statistic);
    }



}