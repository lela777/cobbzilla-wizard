package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.beanutils.BeanUtils;

import javax.persistence.*;
import java.util.UUID;

@MappedSuperclass @EqualsAndHashCode(of={"id"})
public class IdentifiableBase implements Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    @Column(unique=true, nullable=false, updatable=false)
    @Getter @Setter protected Long id;

    @Column(unique=true, updatable=false, nullable=false, length=BasicConstraintConstants.UUID_MAXLEN)
    @Getter @Setter private volatile String uuid = null;

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

}
