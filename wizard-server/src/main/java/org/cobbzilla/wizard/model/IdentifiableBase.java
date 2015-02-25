package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.beanutils.BeanUtils;
import org.cobbzilla.util.string.StringUtil;

import javax.persistence.*;
import java.util.UUID;

@MappedSuperclass @EqualsAndHashCode(of={"uuid"}) @ToString
public class IdentifiableBase implements Identifiable {

    public static final int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    @Id @Column(unique=true, updatable=false, nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private volatile String uuid = null;

    public boolean hasUuid () { return !StringUtil.empty(uuid); }

    public void beforeCreate() {
        if (uuid != null) throw new IllegalStateException("uuid already initialized");
        initUuid();
    }

    public void initUuid() { uuid = UUID.randomUUID().toString(); }

    public void update(Identifiable thing) {
        String existingUuid = getUuid();
        try {
            BeanUtils.copyProperties(this, thing);

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

}
