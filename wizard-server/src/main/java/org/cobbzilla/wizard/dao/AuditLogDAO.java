package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.AuditLog;

import javax.validation.Valid;

public abstract class AuditLogDAO<E extends AuditLog> extends AbstractCRUDDAO<E> {

    public abstract String getEncryptionKey ();

    @Override public Object preCreate(@Valid E entity) {
        return super.preCreate((E) entity.encrypt(getEncryptionKey()));
    }

    @Override public Object preUpdate(@Valid E entity) {
        return super.preUpdate((E) entity.encrypt(getEncryptionKey()));
    }

}
