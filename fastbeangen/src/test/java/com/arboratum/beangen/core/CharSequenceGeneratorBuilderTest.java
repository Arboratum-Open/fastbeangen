package com.arboratum.beangen.core;

import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.RandomSequence;
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
}