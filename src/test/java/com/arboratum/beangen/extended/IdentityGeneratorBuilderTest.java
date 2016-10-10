package com.arboratum.beangen.extended;

import com.arboratum.beangen.Generator;
import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneOffset;

/**
 * Created by gpicron on 16/08/2016.
 */
public class IdentityGeneratorBuilderTest {
    @Test
    public void all() throws Exception {
        final Generator<Identity> identityGenerator = new IdentityGeneratorBuilder().all().build();

        final Identity identity = identityGenerator.generate(1);
        Assert.assertEquals("DE", identity.getCountryOfBirth());
        Assert.assertEquals("2003-02-12T11:05:50.070Z", identity.getDateOfBirth().toInstant().atOffset(ZoneOffset.UTC).toString());
        Assert.assertEquals("Roth", identity.getFamilyNames());
        Assert.assertEquals("Kurt", identity.getFirstNames());
        Assert.assertEquals(Identity.Gender.male, identity.getGender());

        for (int i = 0; i < 100000; i++) {
            System.out.println(identityGenerator.generate(i));
        }
    }

}