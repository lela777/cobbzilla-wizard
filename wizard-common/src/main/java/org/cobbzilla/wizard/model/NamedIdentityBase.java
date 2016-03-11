package org.cobbzilla.wizard.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass @EqualsAndHashCode(of="name")
@NoArgsConstructor @Accessors(chain=true)
public class NamedIdentityBase implements NamedEntity, Identifiable {

    public static final int NAME_MAXLEN = UniquelyNamedEntity.NAME_MAXLEN;

    public NamedIdentityBase (String name) { setName(name); }

    @Id @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter protected String name;

    @Override public String getUuid() { return getName(); }
    @Override public void setUuid(String uuid) { setName(uuid); }

    @Override public void beforeCreate() {}

}
