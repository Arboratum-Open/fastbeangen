package com.arboratum.beangen.example1.model;

import javax.persistence.*;
import java.util.List;

/**
 * Created by gpicron on 14/02/2017.
 */
@Entity
public class Relation implements Identifiable {
    @Id
    private long id;
    @ManyToMany (cascade = CascadeType.REMOVE)
    private List<Person> persons;

    public Relation() {
    }

    public long getId() {
        return this.id;
    }

    public List<Person> getPersons() {
        return this.persons;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setPersons(List<Person> persons) {
        this.persons = persons;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Relation)) return false;
        final Relation other = (Relation) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getId() != other.getId()) return false;
        final Object this$persons = this.getPersons();
        final Object other$persons = other.getPersons();
        if (this$persons == null ? other$persons != null : !this$persons.equals(other$persons)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $id = this.getId();
        result = result * PRIME + (int) ($id >>> 32 ^ $id);
        final Object $persons = this.getPersons();
        result = result * PRIME + ($persons == null ? 43 : $persons.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Relation;
    }

    public String toString() {
        return "com.arboratum.beangen.example1.model.Relation(id=" + this.getId() + ", persons=" + this.getPersons() + ")";
    }
}
