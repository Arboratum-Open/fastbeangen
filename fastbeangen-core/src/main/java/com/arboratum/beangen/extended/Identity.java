package com.arboratum.beangen.extended;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by gpicron on 28/06/2016.
 */
public class Identity {

    public Identity() {
    }

    public String getFamilyNames() {
        return this.familyNames;
    }

    public String getFirstNames() {
        return this.firstNames;
    }

    public Gender getGender() {
        return this.gender;
    }

    public String getCountryOfBirth() {
        return this.countryOfBirth;
    }

    public Date getDateOfBirth() {
        return this.dateOfBirth;
    }

    public void setFamilyNames(String familyNames) {
        this.familyNames = familyNames;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public void setCountryOfBirth(String countryOfBirth) {
        this.countryOfBirth = countryOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Identity)) return false;
        final Identity other = (Identity) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$familyNames = this.getFamilyNames();
        final Object other$familyNames = other.getFamilyNames();
        if (this$familyNames == null ? other$familyNames != null : !this$familyNames.equals(other$familyNames))
            return false;
        final Object this$firstNames = this.getFirstNames();
        final Object other$firstNames = other.getFirstNames();
        if (this$firstNames == null ? other$firstNames != null : !this$firstNames.equals(other$firstNames))
            return false;
        final Object this$gender = this.getGender();
        final Object other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !this$gender.equals(other$gender)) return false;
        final Object this$countryOfBirth = this.getCountryOfBirth();
        final Object other$countryOfBirth = other.getCountryOfBirth();
        if (this$countryOfBirth == null ? other$countryOfBirth != null : !this$countryOfBirth.equals(other$countryOfBirth))
            return false;
        final Date this$dateOfBirth = this.getDateOfBirth();
        final Date other$dateOfBirth = other.getDateOfBirth();
        if (this$dateOfBirth == null ? other$dateOfBirth != null :
                !(new DateTime(this$dateOfBirth).withMillisOfDay(0).equals(new DateTime(other$dateOfBirth).withMillisOfDay(0))))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $familyNames = this.getFamilyNames();
        result = result * PRIME + ($familyNames == null ? 43 : $familyNames.hashCode());
        final Object $firstNames = this.getFirstNames();
        result = result * PRIME + ($firstNames == null ? 43 : $firstNames.hashCode());
        final Object $gender = this.getGender();
        result = result * PRIME + ($gender == null ? 43 : $gender.hashCode());
        final Object $countryOfBirth = this.getCountryOfBirth();
        result = result * PRIME + ($countryOfBirth == null ? 43 : $countryOfBirth.hashCode());
        final Object $dateOfBirth = this.getDateOfBirth();
        result = result * PRIME + ($dateOfBirth == null ? 43 : $dateOfBirth.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Identity;
    }

    public String toString() {
        return "Identity(familyNames=" + this.getFamilyNames() + ", firstNames=" + this.getFirstNames() + ", gender=" + this.getGender() + ", countryOfBirth=" + this.getCountryOfBirth() + ", dateOfBirth=" + new DateTime(this.getDateOfBirth()) + ")";
    }

    public enum Gender {
        male,
        female
    }

    private String familyNames;
    private String firstNames;
    private Gender gender;
    private String countryOfBirth;
    private Date dateOfBirth;
}
