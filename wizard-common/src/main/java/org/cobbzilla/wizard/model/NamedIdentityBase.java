package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@MappedSuperclass @EqualsAndHashCode(of="name") @ToString(of={"name"})
@NoArgsConstructor @Accessors(chain=true)
public class NamedIdentityBase implements NamedEntity, Identifiable {

    public NamedIdentityBase (String name) { setName(name); }

    public NamedIdentityBase update(NamedIdentityBase other) { return setName(other.getName()); }

    @Override public void beforeCreate() {}
    @Override public void beforeUpdate() { setMtime(); }

    @HasValue(message="err.name.empty")
    @Id @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter protected String name;

    @Override @Transient public String getUuid() { return getName(); }
    @Override public void setUuid(String uuid) { setName(uuid); }

    @Column(updatable=false, nullable=false)
    @Getter @JsonIgnore private long ctime = now();
    public void setCtime (long time) { /*noop*/ }
    @JsonIgnore @Transient public long getCtimeAge () { return now() - ctime; }

    @Column(nullable=false)
    @Getter @JsonIgnore private long mtime = now();
    public void setMtime (long time) { this.mtime = time; }
    public void setMtime () { this.mtime = now(); }
    @JsonIgnore @Transient public long getMtimeAge () { return now() - mtime; }

}
