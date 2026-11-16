package com.arboratum.beangen;

import com.arboratum.beangen.util.RandomSequence;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HistoricalOutputCompatibilityTest {
    @Test
    public void preservesHistoricalValuesAndRepeatedGeneration() throws Exception {
        Generator<String> regex = BaseBuilders.aString().matching("[A-Z][a-z]{2,10} street").build();
        Generator<Date> dates = BaseBuilders.aDate().uniform().build();
        InputStream resource = getClass().getResourceAsStream("/historical-output.tsv");
        assertNotNull("Historical reference must be checked in, never auto-generated", resource);
        int checked = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
            String expected;
            while ((expected = reader.readLine()) != null) {
                if (expected.startsWith("#")) {
                    continue;
                }
                long seed = Long.parseLong(expected.substring(0, expected.indexOf('\t')));
                assertEquals("Historical output for seed " + seed, expected, values(seed, regex, dates));
                assertEquals("Repeated output for seed " + seed, expected, values(seed, regex, dates));
                checked++;
            }
        }
        assertEquals("All recorded boundary/reference seeds must be checked", 8, checked);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stillRejectsNegativeIds() {
        new RandomSequence(-1);
    }

    private static String values(long seed, Generator<String> regex, Generator<Date> dates) {
        RandomSequence random = new RandomSequence(seed);
        return seed + "\t" + random.nextLong() + "\t" + random.nextInt(100)
                + "\t" + Double.toHexString(random.nextDouble()) + "\t" + regex.generate(seed)
                + "\t" + dates.generate(seed).getTime();
    }
}
