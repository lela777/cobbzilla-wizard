package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.validation.UniqueValidatorDao;

public abstract class FormNameKeyUniqueValidatorDAO<E extends Identifiable> extends AbstractCRUDDAO<E> implements UniqueValidatorDao {

    protected abstract E findByNameKey(String nameKey);

    @Override
    public boolean isUnique(String uniqueFieldName, Object uniqueValue) {
        if (uniqueValue == null) return true;
        switch (uniqueFieldName) {
            case "nameKey":
                return findByNameKey(uniqueValue.toString()) == null;
            default:
                throw new IllegalArgumentException("isUnique: unsupported field name: "+uniqueFieldName);
        }
    }

    @Override
    public boolean isUnique(String uniqueFieldName, Object uniqueValue, String idFieldName, Object idValue) {
        if (uniqueValue == null) return true;
        switch (uniqueFieldName) {
            case "nameKey":
                final E thing = findByNameKey(uniqueValue.toString());
                if (thing == null) return true;

                switch (idFieldName) {
                    case "uuid": return thing.getUuid().equals(idValue);
                    default: throw new IllegalArgumentException("isUnique: unsupported idFieldName: "+idFieldName);
                }

            default:
                throw new IllegalArgumentException("isUnique: unsupported uniqueFieldName: "+uniqueFieldName);
        }
    }

}
