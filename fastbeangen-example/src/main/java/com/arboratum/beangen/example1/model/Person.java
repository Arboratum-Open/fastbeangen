package com.arboratum.beangen.example1.model;

import com.arboratum.beangen.extended.Identity;

import javax.persistence.*;
import java.util.List;

/**
 * Created by gpicron on 14/02/2017.
 */
@Entity
public class Person implements Identifiable {
    @Id
    private long id;
    @Embedded
    private Identity identity;
    @Embedded
    private Address address;

    @ManyToMany (mappedBy = "persons")
    private List<Relation> relations;


    public Person() {
    }

    public long getId() {
        return this.id;
    }

    public Identity getIdentity() {
        return this.identity;
    }

    public Address getAddress() {
        return this.address;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Person)) return false;
        final Person other = (Person) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getId() != other.getId()) return false;
        final Object this$identity = this.getIdentity();
        final Object other$identity = other.getIdentity();
        if (this$identity == null ? other$identity != null : !this$identity.equals(other$identity)) return false;
        final Object this$address = this.getAddress();
        final Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $id = this.getId();
        result = result * PRIME + (int) ($id >>> 32 ^ $id);
        final Object $identity = this.getIdentity();
        result = result * PRIME + ($identity == null ? 43 : $identity.hashCode());
        final Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Person;
    }

    public String toString() {
        return "Person(id=" + this.getId() + ", identity=" + this.getIdentity() + ", address=" + this.getAddress() + ")";
    }

    public static class Address {
        private String street;
        private String number;
        private String city;

        public Address() {
        }

        public String getStreet() {
            return this.street;
        }

        public String getNumber() {
            return this.number;
        }

        public String getCity() {
            return this.city;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Address)) return false;
            final Address other = (Address) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$street = this.getStreet();
            final Object other$street = other.getStreet();
            if (this$street == null ? other$street != null : !this$street.equals(other$street)) return false;
            final Object this$number = this.getNumber();
            final Object other$number = other.getNumber();
            if (this$number == null ? other$number != null : !this$number.equals(other$number)) return false;
            final Object this$city = this.getCity();
            final Object other$city = other.getCity();
            if (this$city == null ? other$city != null : !this$city.equals(other$city)) return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $street = this.getStreet();
            result = result * PRIME + ($street == null ? 43 : $street.hashCode());
            final Object $number = this.getNumber();
            result = result * PRIME + ($number == null ? 43 : $number.hashCode());
            final Object $city = this.getCity();
            result = result * PRIME + ($city == null ? 43 : $city.hashCode());
            return result;
        }

        protected boolean canEqual(Object other) {
            return other instanceof Address;
        }

        public String toString() {
            return "Person.Address(street=" + this.getStreet() + ", number=" + this.getNumber() + ", city=" + this.getCity() + ")";
        }
    }


}
