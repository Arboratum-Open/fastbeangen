package com.arboratum.beangen.example1.model;

import com.arboratum.beangen.database.UpdateOf;

/**
 * Created by gpicron on 14/02/2017.
 */
public class ChangeAddressCommand implements UpdateOf<Person> {
    private Person.Address newAddress;

    public ChangeAddressCommand() {
    }

    @Override
    public Person apply(Person person) {
        person.setAddress(newAddress);
        return person;
    }

    public Person.Address getNewAddress() {
        return this.newAddress;
    }

    public void setNewAddress(Person.Address newAddress) {
        this.newAddress = newAddress;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeAddressCommand)) return false;
        final ChangeAddressCommand other = (ChangeAddressCommand) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$newAddress = this.getNewAddress();
        final Object other$newAddress = other.getNewAddress();
        if (this$newAddress == null ? other$newAddress != null : !this$newAddress.equals(other$newAddress))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $newAddress = this.getNewAddress();
        result = result * PRIME + ($newAddress == null ? 43 : $newAddress.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ChangeAddressCommand;
    }

    public String toString() {
        return "com.arboratum.beangen.example1.model.ChangeAddressCommand(newAddress=" + this.getNewAddress() + ")";
    }
}
