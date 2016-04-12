package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import java.util.Comparator;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@MappedSuperclass @EqualsAndHashCode(of="name")
@NoArgsConstructor @Accessors(chain=true)
public class NamedIdentityBase implements NamedEntity, Identifiable {

    public static final int NAME_MAXLEN = UniquelyNamedEntity.NAME_MAXLEN;

    public static final Comparator<? extends NamedIdentityBase> NAME_COMPARATOR = new Comparator<NamedIdentityBase>() {
        @Override public int compare(NamedIdentityBase o1, NamedIdentityBase o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

    public NamedIdentityBase (String name) { setName(name); }

    @Override public void beforeCreate() {}
    @Override public void beforeUpdate() { setMtime(); }

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
