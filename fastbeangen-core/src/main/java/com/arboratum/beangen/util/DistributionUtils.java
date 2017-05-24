package com.arboratum.beangen.util;

import com.arboratum.beangen.core.EnumeratedDistributionGeneratorBuilder;
import com.google.common.io.Resources;
import com.google.common.primitives.Longs;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.Pair;

import java.io.*;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Created by gpicron on 23/03/2017.
 */
public final class DistributionUtils {

    /**
     * The API use frequently Frequency object from commons math.  This method permits easy conversion
     * from the Map of counters
     *
     * @param counts
     *
     * @return Frequency object
     */
    public static <K extends Comparable<K>> Frequency convert(Map<K, ? extends Number> counts) {
        Frequency frequency = new Frequency();
        for (Map.Entry<K, ? extends Number> c : counts.entrySet()) {
            frequency.incrementValue(c.getKey(), c.getValue().longValue());
        }

        return frequency;
    }

    /**
     * The API use frequently Frequency object from commons math.  Load a resource CSV file into a Frequency object of (String, Long)
     *
     * Rows with 1 columns, (String, 1)
     * Rows with &gt;= 2 columns, (String, parsed long col 2)
     *
     * Expended encoding is UTF-8
     *
     * @return Frequency object
     */
    public static Frequency fromCsvResource(String resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResourceAsStream(resource), "UTF-8"))) {

            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#")) // filter comments
                    .map(line -> line.split(","))
                    .map(tokens -> {
                        Long count = null;
                        if (tokens.length >= 2) count = Longs.tryParse(tokens[1]);

                        return new Pair<>(tokens[0], (count == null) ? 1L : count);
                    }).collect(Frequency::new, (r, p) -> r.incrementValue(p.getKey(), p.getValue()), Frequency::merge);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("invalid resource file :" + resource, e);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid resource file :" + resource, e);
        }
    }

    /**
     * Creates a new Frequency by filtering entries with a predicate
     */
    public static <K extends Comparable<K>> Frequency filter(Frequency source, Predicate<K> predicate) {
        Frequency frequency = new Frequency();
        source.entrySetIterator()
                .forEachRemaining(entry -> {
                    final K key = (K) entry.getKey();
                    if (predicate.test(key)) {
                        frequency.incrementValue(key, entry.getValue());
                    }
                });
        return frequency;
    }

    /**
     * Creates a new Frenquency by joining 2 frequency (= joint probability)
     */
    public static <K1 extends Comparable<K1>, K2 extends Comparable<K2>, K extends Comparable<K>> Frequency join(Frequency source1, Frequency source2, BiFunction<K1, K2, K> keyJoin) {
        Frequency frequency = new Frequency();
        source1.entrySetIterator()
                .forEachRemaining(e1 -> {
                    final K1 k1 = (K1) e1.getKey();
                    source2.entrySetIterator()
                            .forEachRemaining(e2 -> {
                                final K2 k2 = (K2) e2.getKey();

                                final long increment = FastMath.multiplyExact(e1.getValue(), e2.getValue());
                                frequency.incrementValue(keyJoin.apply(k1, k2), increment);
                            });
                });
        return frequency;
    }




    private static InputStream getResourceAsStream(String resource) throws IOException {
        final InputStream resourceAsStream = Resources.getResource(EnumeratedDistributionGeneratorBuilder.class, resource).openStream();
        if (resourceAsStream == null) throw new IllegalArgumentException("Resource not found : " + resource);
        return resourceAsStream;
    }


}
