package org.cobbzilla.wizard.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @NoArgsConstructor
@EqualsAndHashCode(of={"name"}, callSuper=false)
public abstract class UniquelyNamedEntity extends IdentifiableBase implements NamedEntity {

    public static final int NAME_MAXLEN = 100;

    public UniquelyNamedEntity (String name) { setName(name); }

    public boolean forceLowercase () { return true; }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    protected String name;
    public boolean hasName () { return !empty(name); }

    public String getName () { return hasName() ? (forceLowercase() ? name.toLowerCase() : name) : name; }
    public UniquelyNamedEntity setName (String name) { this.name = (name == null ? null : forceLowercase() ? name.toLowerCase() : name); return this; }

    public boolean isSameName(UniquelyNamedEntity other) { return getName().equals(other.getName()); }

    @Override public String toString() { return "{"+getName() + ": "+super.toString()+"}"; }
}
