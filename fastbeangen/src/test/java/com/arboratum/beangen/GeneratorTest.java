package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by gpicron on 07/08/2016.
 */
public class GeneratorTest {
    public static final int ROUNDS = 1000 * 1024;

    @Test
    public void generate() throws Exception {
        Generator<Long> gen = new Generator<Long>(Long.class) {
            @Override
            public Long generate(RandomSequence register) {
                return register.nextLong(1024);
            }
        };

        final Frequency freqs = new Frequency();
        for (int i = 0; i < ROUNDS; i++) {
            freqs.addValue(gen.generate(i));
        }
        System.out.println(freqs);

        SummaryStatistics statistic = new SummaryStatistics();
        Iterator<Map.Entry<Comparable<?>, Long>> iter= freqs.entrySetIterator();
        for (Map.Entry<Comparable<?>, Long> v = iter.next(); iter.hasNext(); v = iter.next()) {
            statistic.addValue(v.getValue());
        }
        System.out.println(statistic);


        assertEquals(statistic.getN(), 1023);

        final double[] expected = IntStream.range(0, 1024).mapToDouble(value -> ROUNDS).toArray();
        final long[] actuals = IntStream.range(0, 1024).mapToLong(value -> freqs.getCount(value)).toArray();


        final double test = new ChiSquareTest().chiSquareTest(expected, actuals);
        System.out.println("Chi2 test:" + test);
        assertThat(test, greaterThan(0.999));

    }


    @Test
    public void idemPotencyTest() {
        final double[] longs = LongStream.range(0, 100000).mapToObj(RandomSequence::new)
                .flatMapToDouble((RandomSequence rs) -> IntStream.range(0, 1000).mapToDouble(x->rs.nextDouble())).toArray();

        for (int i = 0; i < 1; i++) {
            final double[] long2 = LongStream.range(0, 100000).mapToObj(RandomSequence::new)
                    .flatMapToDouble((RandomSequence rs) -> IntStream.range(0, 1000).mapToDouble(x->rs.nextDouble())).toArray();

            Assert.assertArrayEquals(longs, long2, RandomSequence.DOUBLE_STEP);
        }
    }

    @Test
    public void idemPotencyTest2() throws IOException {
        File ref = new File("working");

        final DoubleStream doubleStream = LongStream.range(0, 100000).mapToObj(RandomSequence::new)
                .flatMapToDouble((RandomSequence rs) -> IntStream.range(0, 1000).mapToDouble(x -> rs.nextDouble()));

        if (!ref.exists()) {
            BufferedWriter fileOutputStream = new BufferedWriter(new FileWriter(ref));

            doubleStream
                    .forEach(d -> {
                        try {
                            fileOutputStream.write(Long.toString(Double.doubleToLongBits(d)));
                            fileOutputStream.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            fileOutputStream.close();
        } else {
            System.out.println("load and compare");
            BufferedReader fileReader = new BufferedReader(new FileReader(ref));

            doubleStream.forEach(d -> {
                 String s = null;
                try {
                    s = fileReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final long l = Long.parseLong(s);
                final double v = Double.longBitsToDouble(l);
                Assert.assertEquals(v, d, RandomSequence.DOUBLE_STEP);
            });
            ref.delete();
        }

    }


}