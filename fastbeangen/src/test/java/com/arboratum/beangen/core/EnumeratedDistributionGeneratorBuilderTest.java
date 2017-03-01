package com.arboratum.beangen.core;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import org.junit.Assert;
import org.junit.Test;

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
    public void weights() throws Exception {

    }

}