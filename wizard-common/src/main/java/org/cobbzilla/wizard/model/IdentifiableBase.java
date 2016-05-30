package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.FieldTransformer;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.string.StringUtil;
import org.hibernate.cfg.ImprovedNamingStrategy;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;

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
        if (uuid != null) die("uuid already initialized on "+getClass().getName());
        initUuid();
    }

    @Override public void beforeUpdate() { setMtime(); }

    public void initUuid() { setUuid(UUID.randomUUID().toString()); }

    public void update(Identifiable thing) { update(thing, null); }

    public void update(Identifiable thing, String[] fields) {
        String existingUuid = getUuid();
        try {
            ReflectionUtil.copy(this, thing, fields);

        } catch (Exception e) {
            throw new IllegalArgumentException("update: error copying properties: "+e, e);

        } finally {
            // Do not allow these to be updated
            setUuid(existingUuid);
        }
    }

    @Column(updatable=false, nullable=false)
    @Getter @JsonIgnore private long ctime = now();
    public void setCtime (long time) { /*noop*/ }
    @JsonIgnore @Transient public long getCtimeAge () { return now() - ctime; }

    @Column(nullable=false)
    @Getter @JsonIgnore private long mtime = now();
    public void setMtime (long time) { this.mtime = time; }
    public void setMtime () { this.mtime = now(); }
    @JsonIgnore @Transient public long getMtimeAge () { return now() - mtime; }

    @Override public String toString() { return simpleName()+"{uuid=" + uuid + "}"; }

    public static String[] toUuidArray(List<? extends Identifiable> entities) {
        return empty(entities)
                ? StringUtil.EMPTY_ARRAY
                : (String[]) collectArray(entities, "uuid");
    }

    public static List<String> toUuidList(List<? extends Identifiable> entities) {
        if (empty(entities)) return Collections.emptyList();
        return collectList(entities, "uuid");
    }

    private static final Map<String, FieldTransformer> fieldTransformerCache = new ConcurrentHashMap<>();
    protected static FieldTransformer getFieldTransformer(String field) {
        FieldTransformer f = fieldTransformerCache.get(field);
        if (f == null) {
            f = new FieldTransformer(field);
            fieldTransformerCache.put(field, f);
        }
        return f;
    }

    public static <T> T[] collectArray(List<? extends Identifiable> entities, String field) {
        return (T[]) CollectionUtils.collect(entities, getFieldTransformer(field)).toArray(new String[entities.size()]);
    }
    public static <T> List<T> collectList(List<? extends Identifiable> entities, String field) {
        return (List<T>) CollectionUtils.collect(entities, getFieldTransformer(field));
    }
    public static List<String> collectStringList(List<? extends Identifiable> entities, String field) {
        return (List<String>) CollectionUtils.collect(entities, getFieldTransformer(field));
    }

}
