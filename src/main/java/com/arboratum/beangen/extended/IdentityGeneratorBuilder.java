package com.arboratum.beangen.extended;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.AbstractGeneratorBuilder;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Date;

/**
 * Created by gpicron on 16/08/2016.
 */
public class IdentityGeneratorBuilder extends AbstractGeneratorBuilder<Identity> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(Identity.class);


    private static final Generator<Identity.Gender> GENDER = BaseBuilders.anEnum(Identity.Gender.class).uniform().build();
    private static final Generator<java.util.Date> DATE_OF_BIRTH = BaseBuilders.aDate().uniform(
            Date.from(Instant.parse("1926-01-01T00:00:00Z")),Date.from(Instant.parse("2016-01-01T00:00:00Z"))).build();

    public IdentityGeneratorBuilder() {
        super(Identity.class);
    }

    @Value
    @Builder
    private static final class CountryConfig {
        private final String country;
        private final Generator<String> givenNameMale;
        private final Generator<String> givenNameFemale;
        private final Generator<String> familyName;
    }

    public IdentityGeneratorBuilder country(String iso) {
        final CountryConfig countryOfBirth = buildCountryConfig(iso);
        setup(seq -> {
            final Identity.Gender gender = GENDER.generate(seq);

            Identity identity = new Identity();
            identity.setDateOfBirth(DATE_OF_BIRTH.generate(seq));
            identity.setGender(gender);
            identity.setCountryOfBirth(countryOfBirth.getCountry());
            if (gender == Identity.Gender.male) {
                identity.setFirstNames(countryOfBirth.getGivenNameMale().generate(seq));
            } else {
                identity.setFirstNames(countryOfBirth.getGivenNameFemale().generate(seq));
            }
            identity.setFamilyNames(countryOfBirth.getFamilyName().generate(seq));

            return identity;
        });

        return this;
    }

    public IdentityGeneratorBuilder all() {
        return countries("/generator/country-population.csv");
    }

    public IdentityGeneratorBuilder countries(String resource) {
        setup(seq -> {
            final Identity.Gender gender = GENDER.generate(seq);
            final CountryConfig countryOfBirth = ((Generator<CountryConfig>) BaseBuilders.enumerated(CountryConfig.class)
                    .valuesFromCSVResource(resource, k -> buildCountryConfig(k)).build()).generate(seq);

            Identity identity = new Identity();
            identity.setDateOfBirth(DATE_OF_BIRTH.generate(seq));
            identity.setGender(gender);
            identity.setCountryOfBirth(countryOfBirth.getCountry());
            if (gender == Identity.Gender.male) {
                identity.setFirstNames(countryOfBirth.getGivenNameMale().generate(seq));
            } else {
                identity.setFirstNames(countryOfBirth.getGivenNameFemale().generate(seq));
            }
            identity.setFamilyNames(countryOfBirth.getFamilyName().generate(seq));

            return identity;
        });

        return this;
    }

    private static CountryConfig buildCountryConfig(String iso2) {
        return CountryConfig.builder()
                .country(iso2)
                .familyName(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/familyName_" + iso2 + ".csv").build())
                .givenNameMale(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/givenName_male_" + iso2 + ".csv").build())
                .givenNameFemale(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/givenName_female_" + iso2 + ".csv").build())
                .build();
    }

}
