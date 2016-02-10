package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.string.StringUtil;
import org.hibernate.cfg.ImprovedNamingStrategy;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.UUID;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @EqualsAndHashCode(of="uuid")
public class IdentifiableBase implements Identifiable {

    public String simpleName () { return getClass().getSimpleName(); }
    public String propName () { return StringUtil.uncapitalize(getClass().getSimpleName()); }

    public String tableName () { return ImprovedNamingStrategy.INSTANCE.classToTableName(getClass().getName()); }
    public String tableName (String className) { return ImprovedNamingStrategy.INSTANCE.classToTableName(className); }

    public String columnName () { return ImprovedNamingStrategy.INSTANCE.propertyToColumnName(propName()); }
    public String columnName (String propName) { return ImprovedNamingStrategy.INSTANCE.propertyToColumnName(propName); }

    @Id @Column(unique=true, updatable=false, nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private volatile String uuid = null;

    public boolean hasUuid () { return !empty(uuid); }

    public void beforeCreate() {
        if (uuid != null) die("uuid already initialized");
        initUuid();
    }

    public void initUuid() { setUuid(UUID.randomUUID().toString()); }

    public void update(Identifiable thing) {
        String existingUuid = getUuid();
        try {
            ReflectionUtil.copy(this, thing);

        } catch (Exception e) {
            throw new IllegalArgumentException("update: error copying properties: "+e, e);

        } finally {
            // Do not allow these to be updated
            setUuid(existingUuid);
        }
    }

    @Column(updatable=false, nullable=false)
    @Getter @JsonIgnore
    private long ctime = System.currentTimeMillis();
    public void setCtime (long time) { /*noop*/ }

    @JsonIgnore @Transient public long getCtimeAge () { return System.currentTimeMillis() - ctime; }

    @Override public String toString() { return getClass().getSimpleName()+"{uuid=" + uuid + "}"; }

    public static final Comparator<IdentifiableBase> CTIME_NEWEST_FIRST = new Comparator<IdentifiableBase>() {
        @Override public int compare(IdentifiableBase o1, IdentifiableBase o2) {
            return new Long(o1.getCtime()).compareTo(o2.getCtime());
        }
    };
    public static final Comparator<IdentifiableBase> CTIME_OLDEST_FIRST = new Comparator<IdentifiableBase>() {
        @Override public int compare(IdentifiableBase o1, IdentifiableBase o2) {
            return new Long(o2.getCtime()).compareTo(o1.getCtime());
        }
    };
}
