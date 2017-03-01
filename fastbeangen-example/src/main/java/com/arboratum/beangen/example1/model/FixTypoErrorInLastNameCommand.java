package com.arboratum.beangen.example1.model;

import com.arboratum.beangen.database.UpdateOf;

/**
 * Created by gpicron on 14/02/2017.
 */
public class FixTypoErrorInLastNameCommand implements UpdateOf<Person> {
    private String additionalChars;

    public FixTypoErrorInLastNameCommand() {
    }

    @Override
    public Person apply(Person person) {
        person.getIdentity().setFamilyNames(person.getIdentity().getFamilyNames() + additionalChars);
        return person;
    }

    public String getAdditionalChars() {
        return this.additionalChars;
    }

    public void setAdditionalChars(String additionalChars) {
        this.additionalChars = additionalChars;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof FixTypoErrorInLastNameCommand)) return false;
        final FixTypoErrorInLastNameCommand other = (FixTypoErrorInLastNameCommand) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$additionalChars = this.getAdditionalChars();
        final Object other$additionalChars = other.getAdditionalChars();
        if (this$additionalChars == null ? other$additionalChars != null : !this$additionalChars.equals(other$additionalChars))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $additionalChars = this.getAdditionalChars();
        result = result * PRIME + ($additionalChars == null ? 43 : $additionalChars.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof FixTypoErrorInLastNameCommand;
    }

    public String toString() {
        return "com.arboratum.beangen.example1.model.FixTypoErrorInLastNameCommand(additionalChars=" + this.getAdditionalChars() + ")";
    }
}
