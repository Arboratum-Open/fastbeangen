package com.arboratum.beangen.example1.model;

import com.arboratum.beangen.database.UpdateOf;

/**
 * Created by gpicron on 14/02/2017.
 */
public class JoinRelationCommand implements UpdateOf<Relation> {
    private Person addedPerson;

    public JoinRelationCommand() {
    }

    @Override
    public Relation apply(Relation relation) {
        relation.getPersons().add(addedPerson);
        return relation;
    }

    public Person getAddedPerson() {
        return this.addedPerson;
    }

    public void setAddedPerson(Person addedPerson) {
        this.addedPerson = addedPerson;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof JoinRelationCommand)) return false;
        final JoinRelationCommand other = (JoinRelationCommand) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$addedPerson = this.getAddedPerson();
        final Object other$addedPerson = other.getAddedPerson();
        if (this$addedPerson == null ? other$addedPerson != null : !this$addedPerson.equals(other$addedPerson))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $addedPerson = this.getAddedPerson();
        result = result * PRIME + ($addedPerson == null ? 43 : $addedPerson.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof JoinRelationCommand;
    }

    public String toString() {
        return "com.arboratum.beangen.example1.model.JoinRelationCommand(addedPerson=" + this.getAddedPerson() + ")";
    }
}
