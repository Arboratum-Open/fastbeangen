package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gpicron on 10/08/2016.
 */
public class EnumeratedDistributionGeneratorBuilderTest {

    @Test
    public void values() throws Exception {
        final Generator gen = BaseBuilders.enumerated(String.class).values("ABC", "DEF", "HIJ").build();
        Assert.assertEquals("ABC", gen.generate(0));
        Assert.assertEquals("DEF", gen.generate(32));
        Assert.assertEquals("HIJ", gen.generate(6));
        Assert.assertEquals("ABC", gen.generate(0));

    }


    @Test
    public void generators() throws Exception {
        final Generator gen = BaseBuilders.enumerated(String.class).generators(
                BaseBuilders.aString().alphaNumeric(3,3),
                BaseBuilders.aString().alphaNumeric(2,2)).build();
        Assert.assertEquals("8O", gen.generate(0));
        Assert.assertEquals("TB0", gen.generate(32));
        Assert.assertEquals("Jq", gen.generate(6));
        Assert.assertEquals("8O", gen.generate(0));
        Assert.assertEquals("Jq", gen.generate(6));
        Assert.assertEquals("TB0", gen.generate(32));

    }


    @Test
    public void generatorMultiLayer() throws Exception {
        Map<Integer, Integer> yearCount = new HashMap<>();
        yearCount.put(1995, 100);
        yearCount.put(1996, 10);

        final int size = yearCount.size();
        AbstractGeneratorBuilder<DateTime>[] generators= new AbstractGeneratorBuilder[size];
        long[] weights = new long[size];
        int i = 0;
        for (Map.Entry<Integer, Integer> e : yearCount.entrySet()) {
            final DateTime year = new DateTime(e.getKey(), 1, 1, 0, 0, DateTimeZone.UTC);

            final int daysInThisYear = Days.daysBetween(year, year.plusYears(1)).getDays();

            generators[i] = BaseBuilders.aInteger().uniform(0, daysInThisYear).convert(year::plusDays);
            weights[i] = e.getValue();
            i++;
        }


        final Generator<DateTime> gen = BaseBuilders.enumerated(DateTime.class).generators(generators).weights(weights).build();
        Assert.assertEquals("1995-10-13T00:00:00.000Z", gen.generate(0).toString());
        Assert.assertEquals("1995-02-03T00:00:00.000Z", gen.generate(30).toString());

    }

    @Test
    public void weights() throws Exception {

    }

}