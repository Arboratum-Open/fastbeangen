package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.util.DistributionUtils;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.arboratum.beangen.BaseBuilders.aString;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by gpicron on 24/05/2017.
 */
public class CollectionGeneratorBuilderTest {
    @Test
    public void ofUniformSize() throws Exception {
        final AbstractGeneratorBuilder<String> valueGenerator = aString().alphaNumeric(3, 5);
        final Generator<List<String>> generator = BaseBuilders.<List<String>, String>aList()
                .of(0, 5, valueGenerator)
                .build();

        assertThat(generator.generate(1), equalTo(asList("tuNQ")));
        assertThat(generator.generate(2), equalTo(asList("VjV", "2khU", "Wor")));
    }

    @Test
    public void ofFrequencyBasedSize() throws Exception {
        Map<Integer, Integer> sizeFrequency = new HashMap<>();
        sizeFrequency.put(1, 100);
        sizeFrequency.put(2, 50);

        final Frequency sizeFreqs = DistributionUtils.convert(sizeFrequency);

        final AbstractGeneratorBuilder<String> valueGenerator = aString().alphaNumeric(3, 5);
        final Generator<List<String>> generator = BaseBuilders.<List<String>, String>aList()
                .of(sizeFreqs, valueGenerator)
                .build();

        assertThat(generator.generate(1), equalTo(asList("tuNQ")));
        assertThat(generator.generate(2), equalTo(asList("VjV")));
        assertThat(generator.generate(3), equalTo(asList("ko6")));
        assertThat(generator.generate(4), equalTo(asList("uDSR")));
        assertThat(generator.generate(5), equalTo(asList("qFY")));
        assertThat(generator.generate(6), equalTo(asList("Jqk", "eRM12")));
        assertThat(generator.generate(7), equalTo(asList("54n")));
        assertThat(generator.generate(8), equalTo(asList("NBB")));
        assertThat(generator.generate(9), equalTo(asList("VN0")));

    }



}