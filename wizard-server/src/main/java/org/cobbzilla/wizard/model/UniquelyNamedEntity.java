package org.cobbzilla.wizard.model;

import lombok.EqualsAndHashCode;
import org.cobbzilla.util.string.StringUtil;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass @EqualsAndHashCode(of={"name"}, callSuper=false)
public abstract class UniquelyNamedEntity<T extends IdentifiableBase> extends IdentifiableBase {

    protected boolean forceLowercase () { return true; }

    @Column(length=100, unique=true, nullable=false)
    @Size(max=100)
    protected String name;
    public boolean hasName () { return !StringUtil.empty(name); }

    public String getName () { return hasName() ? (forceLowercase() ? name.toLowerCase() : name) : name; }
    public void setName (String name) { this.name = (name == null ? null : forceLowercase() ? name.toLowerCase() : name); }
    public T withName (String n) { setName(n); return (T) this; }

}
