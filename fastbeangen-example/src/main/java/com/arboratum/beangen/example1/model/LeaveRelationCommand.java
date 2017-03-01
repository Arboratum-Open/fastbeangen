package com.arboratum.beangen.example1.model;

import com.arboratum.beangen.database.UpdateOf;

import java.util.List;

/**
 * Created by gpicron on 14/02/2017.
 */
public class LeaveRelationCommand implements UpdateOf<Relation> {
    private int index;

    public LeaveRelationCommand() {
    }

    @Override
    public Relation apply(Relation relation) {
        final List<Person> persons = relation.getPersons();
        persons.remove(index % persons.size());
        return relation;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LeaveRelationCommand)) return false;
        final LeaveRelationCommand other = (LeaveRelationCommand) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getIndex() != other.getIndex()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getIndex();
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof LeaveRelationCommand;
    }

    public String toString() {
        return "com.arboratum.beangen.example1.model.LeaveRelationCommand(index=" + this.getIndex() + ")";
    }
}
