package org.cobbzilla.wizard.dao;

/**
 * Forked from dropwizard https://github.com/dropwizard/
 * https://github.com/dropwizard/dropwizard/blob/master/LICENSE
 */

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;
import org.hibernate.criterion.Restrictions;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractCRUDDAO<E extends Identifiable>
        extends AbstractDAO<E>
        implements DAO<E> {

    public List<E> findAll() { return list(criteria()); }

    public E findByUuid(String uuid) {
        return uniqueResult(criteria().add(Restrictions.eq("uuid", uuid)));
    }

    public boolean exists(String uuid) {
        return findByUuid(uuid) != null;
    }

    @Override public Object preCreate(@Valid E entity) { return entity; }
    @Override public E postCreate(E entity, Object context) { return  entity; }

    public E create(@Valid E entity) {
        entity.beforeCreate();
        entity.setUuid((String) hibernateTemplate.save(checkNotNull(entity)));
        return entity;
    }

    public E createOrUpdate(@Valid E entity) {
        return (entity.getUuid() == null) ? create(entity) : update(entity);
    }

    @Override public Object preUpdate(@Valid E entity) { return entity; }
    @Override public E postUpdate(@Valid E entity, Object context) { return entity; }

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

    public List<E> findByField(String field, Object value) {
        return list(criteria().add(Restrictions.eq(field, value)));
    }

    @Override
    public List<E> search(ResultPage resultPage) {
        // todo
        return Collections.emptyList();
    }
}
