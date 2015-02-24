package org.cobbzilla.wizard.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass
@NoArgsConstructor
@EqualsAndHashCode(of={"name"}, callSuper=false)
@ToString(callSuper=true)
public abstract class UniquelyNamedEntity extends IdentifiableBase implements NamedEntity {

    public UniquelyNamedEntity (String name) { setName(name); }

    protected boolean forceLowercase () { return true; }

    @HasValue(message="err.name.empty")
    @Column(length=100, unique=true, nullable=false, updatable=false)
    @Size(max=100)
    protected String name;
    public boolean hasName () { return !StringUtil.empty(name); }

    public String getName () { return hasName() ? (forceLowercase() ? name.toLowerCase() : name) : name; }
    public UniquelyNamedEntity setName (String name) { this.name = (name == null ? null : forceLowercase() ? name.toLowerCase() : name); return this; }

    public boolean isSameName(UniquelyNamedEntity other) { return getName().equals(other.getName()); }

}
