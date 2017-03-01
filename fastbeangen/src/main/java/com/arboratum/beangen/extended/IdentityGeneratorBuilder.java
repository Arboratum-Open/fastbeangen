package com.arboratum.beangen.extended;

import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.core.AbstractGeneratorBuilder;
import com.google.common.collect.ImmutableSet;

import java.time.Instant;
import java.util.Date;

/**
 * Created by gpicron on 16/08/2016.
 */
public class IdentityGeneratorBuilder extends AbstractGeneratorBuilder<Identity> {
    public static final ImmutableSet<Class> SUPPORTED_TYPES = ImmutableSet.of(Identity.class);


    private static final Generator<Identity.Gender> GENDER = BaseBuilders.anEnum(Identity.Gender.class).uniform().build();
    private static final Generator<java.util.Date> DATE_OF_BIRTH = BaseBuilders.aDate().uniform(
            Date.from(Instant.parse("1926-01-01T00:00:00Z")),Date.from(Instant.parse("2016-01-01T00:00:00Z")))
            .build();

    public IdentityGeneratorBuilder() {
        super(Identity.class);
    }

    private static final class CountryConfig {
        private final String country;
        private final Generator<String> givenNameMale;
        private final Generator<String> givenNameFemale;
        private final Generator<String> familyName;

        @java.beans.ConstructorProperties({"country", "givenNameMale", "givenNameFemale", "familyName"})
        CountryConfig(String country, Generator<String> givenNameMale, Generator<String> givenNameFemale, Generator<String> familyName) {
            this.country = country;
            this.givenNameMale = givenNameMale;
            this.givenNameFemale = givenNameFemale;
            this.familyName = familyName;
        }

        public static CountryConfigBuilder builder() {
            return new CountryConfigBuilder();
        }

        public String getCountry() {
            return this.country;
        }

        public Generator<String> getGivenNameMale() {
            return this.givenNameMale;
        }

        public Generator<String> getGivenNameFemale() {
            return this.givenNameFemale;
        }

        public Generator<String> getFamilyName() {
            return this.familyName;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof CountryConfig)) return false;
            final CountryConfig other = (CountryConfig) o;
            final Object this$country = this.getCountry();
            final Object other$country = other.getCountry();
            if (this$country == null ? other$country != null : !this$country.equals(other$country)) return false;
            final Object this$givenNameMale = this.getGivenNameMale();
            final Object other$givenNameMale = other.getGivenNameMale();
            if (this$givenNameMale == null ? other$givenNameMale != null : !this$givenNameMale.equals(other$givenNameMale))
                return false;
            final Object this$givenNameFemale = this.getGivenNameFemale();
            final Object other$givenNameFemale = other.getGivenNameFemale();
            if (this$givenNameFemale == null ? other$givenNameFemale != null : !this$givenNameFemale.equals(other$givenNameFemale))
                return false;
            final Object this$familyName = this.getFamilyName();
            final Object other$familyName = other.getFamilyName();
            if (this$familyName == null ? other$familyName != null : !this$familyName.equals(other$familyName))
                return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $country = this.getCountry();
            result = result * PRIME + ($country == null ? 43 : $country.hashCode());
            final Object $givenNameMale = this.getGivenNameMale();
            result = result * PRIME + ($givenNameMale == null ? 43 : $givenNameMale.hashCode());
            final Object $givenNameFemale = this.getGivenNameFemale();
            result = result * PRIME + ($givenNameFemale == null ? 43 : $givenNameFemale.hashCode());
            final Object $familyName = this.getFamilyName();
            result = result * PRIME + ($familyName == null ? 43 : $familyName.hashCode());
            return result;
        }

        public String toString() {
            return "com.arboratum.beangen.extended.IdentityGeneratorBuilder.CountryConfig(country=" + this.getCountry() + ", givenNameMale=" + this.getGivenNameMale() + ", givenNameFemale=" + this.getGivenNameFemale() + ", familyName=" + this.getFamilyName() + ")";
        }

        public static class CountryConfigBuilder {
            private String country;
            private Generator<String> givenNameMale;
            private Generator<String> givenNameFemale;
            private Generator<String> familyName;

            CountryConfigBuilder() {
            }

            public CountryConfig.CountryConfigBuilder country(String country) {
                this.country = country;
                return this;
            }

            public CountryConfig.CountryConfigBuilder givenNameMale(Generator<String> givenNameMale) {
                this.givenNameMale = givenNameMale;
                return this;
            }

            public CountryConfig.CountryConfigBuilder givenNameFemale(Generator<String> givenNameFemale) {
                this.givenNameFemale = givenNameFemale;
                return this;
            }

            public CountryConfig.CountryConfigBuilder familyName(Generator<String> familyName) {
                this.familyName = familyName;
                return this;
            }

            public CountryConfig build() {
                return new CountryConfig(country, givenNameMale, givenNameFemale, familyName);
            }

            public String toString() {
                return "com.arboratum.beangen.extended.IdentityGeneratorBuilder.CountryConfig.CountryConfigBuilder(country=" + this.country + ", givenNameMale=" + this.givenNameMale + ", givenNameFemale=" + this.givenNameFemale + ", familyName=" + this.familyName + ")";
            }
        }
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
        final Generator<CountryConfig> country =  BaseBuilders.enumerated(CountryConfig.class)
                .valuesFromCSVResource(resource, k -> buildCountryConfig(k)).build();
        setup(seq -> {
            final Identity.Gender gender = GENDER.generate(seq);
            final CountryConfig countryOfBirth = country.generate(seq);

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
        iso2 = iso2.toUpperCase();
        return CountryConfig.builder()
                .country(iso2)
                .familyName(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/familyName_" + iso2 + ".csv").build())
                .givenNameMale(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/givenName_male_" + iso2 + ".csv").build())
                .givenNameFemale(BaseBuilders.enumerated(String.class).valuesFromCSVResource("/generator/givenName_female_" + iso2 + ".csv").build())
                .build();
    }

}
