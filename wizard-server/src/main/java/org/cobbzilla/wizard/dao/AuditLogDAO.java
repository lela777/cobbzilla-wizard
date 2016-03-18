package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.AuditLog;

import javax.validation.Valid;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public abstract class AuditLogDAO<E extends AuditLog> extends AbstractCRUDDAO<E> {

    public abstract String getEncryptionKey ();

    @Override public Object preCreate(@Valid E entity) {
        final String key = getEncryptionKey();
        return empty(key) ? super.preCreate(entity) : super.preCreate((E) entity.encrypt(key));
    }

    @Override public Object preUpdate(@Valid E entity) {
        final String key = getEncryptionKey();
        return empty(key) ? super.preCreate(entity) : super.preCreate((E) entity.encrypt(key));
    }

}
