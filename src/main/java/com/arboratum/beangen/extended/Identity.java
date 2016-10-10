package com.arboratum.beangen.extended;

import lombok.Data;

import java.util.Date;

/**
 * Created by gpicron on 28/06/2016.
 */
@Data
public class Identity {

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
