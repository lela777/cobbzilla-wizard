package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.criterion.Restrictions;

import javax.validation.Valid;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractCRUDDAO<E extends Identifiable>
        extends AbstractDAO<E>
        implements AbstractCRUDDAOBase<E> {

    public List<E> findAll() {
        return list(criteria());
    }

    public E findByUuid(String uuid) {
        return uniqueResult(criteria().add(Restrictions.eq("uuid", uuid)));
    }

    public boolean exists(Long id) {
        return uniqueResult(hibernateTemplate.find("select 1 from " + getEntityClass().getSimpleName() + " e where e.id = ?", id)) != null;
    }

    public boolean exists(String uuid) {
        return findByUuid(uuid) != null;
    }

    public E create(@Valid E entity) {
        entity.beforeCreate();
        entity.setUuid((String) hibernateTemplate.save(checkNotNull(entity)));
        return entity;
    }

    public E createOrUpdate(@Valid E entity) {
        return (entity.getUuid() == null) ? create(entity) : update(entity);
    }

    public E update(@Valid E entity) {
        return hibernateTemplate.merge(checkNotNull(entity));
    }

    public void delete(String uuid) {
        E found = get(checkNotNull(uuid));
        if (found != null) {
            hibernateTemplate.delete(found);
        }
    }

    public E findByUniqueField(String field, Object value) {
        return uniqueResult(Restrictions.eq(field, value));
    }

}
