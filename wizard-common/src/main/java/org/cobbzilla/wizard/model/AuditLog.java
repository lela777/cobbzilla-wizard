package org.cobbzilla.wizard.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.system.Bytes;
import org.cobbzilla.wizard.api.CrudOperation;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public class AuditLog extends StrongIdentifiableBase {

    @Size(max=200, message="err.entityType.length")
    @Column(length=200, nullable=false, updatable=false)
    @Getter @Setter private String entityType;

    @HasValue(message="err.uuid.empty")
    @Size(max=UUID_MAXLEN, message="err.uuid.length")
    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String entityUuid;

    @Column(length=10, nullable=false, updatable=false)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private CrudOperation operation;

    public static final int STATE_MAXLEN = (int) (512 * Bytes.KB);

    @Size(max=STATE_MAXLEN, message="err.prevState.length")
    @Column(length=STATE_MAXLEN, updatable=false)
    @Getter @Setter private String prevState;

    @Size(max=STATE_MAXLEN, message="err.newState.length")
    @Column(length=STATE_MAXLEN, updatable=false)
    @Getter @Setter private String newState;

    @Column(nullable=false)
    @Getter @Setter private boolean success = false;

}
