package org.cobbzilla.wizard.model;

import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.validation.constraints.Size;

@EqualsAndHashCode(of={"name"}, callSuper=false)
public abstract class UniquelyNamedEntity<T extends IdentifiableBase> extends IdentifiableBase {

    protected boolean forceLowercase () { return true; }

    @Column(length=100, unique=true, nullable=false)
    @Size(max=100)
    protected String name;
    public String getName () { return hasUuid() ? (forceLowercase() ? getUuid().toLowerCase() : getUuid()) : null; }
    public void setName (String name) { this.setUuid(name == null ? null : (forceLowercase() ? name.toLowerCase() : name)); }
    public T withName (String n) { setName(n); return (T) this; }

}
