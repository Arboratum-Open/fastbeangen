package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Created by gpicron on 23/02/2017.
 */
public class CharSequenceGeneratorBuilderTest {

    @Test
    public void matchingidemPotencyTest2() throws Exception {
        final Generator<String> s = new CharSequenceGeneratorBuilder<>(String.class)
                .matching("[A-Z][a-z]{2,10} street")
                .build();

        File ref = new File("working-mathcing");

        Stream<String> stream = LongStream.range(0, 1000000).mapToObj(RandomSequence::new)
                .map(s::generate);

        if (!ref.exists()) {
            BufferedWriter fileOutputStream = new BufferedWriter(new FileWriter(ref));

            stream
                    .forEach(d -> {
                        try {
                            fileOutputStream.write(d);
                            fileOutputStream.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            fileOutputStream.close();
        } else {
            System.out.println("load and compare");
            BufferedReader fileReader = new BufferedReader(new FileReader(ref));

            stream.forEach(d -> {
                String refs = null;
                try {
                    refs = fileReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Assert.assertEquals(refs, d);
            });
            //ref.delete();

        }

    }

    @Test
    public void withCharactersAndUniform() throws Exception {
        Frequency frequency = new Frequency();
        frequency.incrementValue('a', 100);
        frequency.incrementValue('b', 50);

        final Generator<String> s = new CharSequenceGeneratorBuilder<>(String.class)
                .withCharactersAnd(frequency).uniformLength(10, 20)
                .build();

        Assert.assertEquals("bababababaaaaaaabaaa", s.generate(0));
        Assert.assertEquals("abaaababaabaaabaa", s.generate(1));
        Assert.assertEquals("bbbaaaabaaabba", s.generate(100));

    }

    @Test
    public void withCharactersAndDistro() throws Exception {
        Frequency frequency = new Frequency();
        frequency.incrementValue('a', 100);
        frequency.incrementValue('b', 50);
        Frequency frequencyLength = new Frequency();
        frequencyLength.incrementValue(10, 100);
        frequencyLength.incrementValue(20, 50);

        final Generator<String> s = new CharSequenceGeneratorBuilder<>(String.class)
                .withCharactersAnd(frequency).lengthDistribution(frequencyLength)
                .build();

        Assert.assertEquals("bababababaaaaaaabaaa", s.generate(0));
        Assert.assertEquals("abaaababaa", s.generate(1));
        Assert.assertEquals("bbbaaaabaa", s.generate(100));

    }
}