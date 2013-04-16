package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.beanutils.BeanUtils;

import javax.persistence.*;
import java.util.UUID;

@MappedSuperclass
public class Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(unique=true, nullable=false, updatable=false)
    @Getter @Setter
    protected Long id;

    @Column(unique=true, updatable=false, nullable=false, length=BasicConstraintConstants.UUID_MAXLEN)
    @Getter @Setter
    private volatile String uuid = null;

    public void beforeCreate() {
        if (uuid != null) throw new IllegalStateException("uuid already initialized");
        uuid = UUID.randomUUID().toString();
    }

    public void update(Identifiable thing) {
        Long existingId = getId();
        String existingUuid = getUuid();
        try {
            BeanUtils.copyProperties(this, thing);

        } catch (Exception e) {
            throw new IllegalArgumentException("update: error copying properties: "+e, e);

        } finally {
            // Do not allow these to be updated
            setId(existingId);
            setUuid(existingUuid);
        }
    }

    @Column(updatable=false, nullable=false)
    @Getter @Setter
    protected long ctime = System.currentTimeMillis();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identifiable)) return false;

        Identifiable that = (Identifiable) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
