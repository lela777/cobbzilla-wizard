package org.cobbzilla.wizard.dao;

/**
 * Forked from dropwizard https://github.com/dropwizard/
 * https://github.com/dropwizard/dropwizard/blob/master/LICENSE
 */

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;

@Transactional @Slf4j
public abstract class AbstractCRUDDAO<E extends Identifiable> extends AbstractDAO<E> {

    public static final Transformer TO_UUID = new FieldTransfomer("uuid");
    public static <E> Collection<String> toUuid (Collection<E> c) { return CollectionUtils.collect(c, TO_UUID); }

    @Transactional(readOnly=true)
    @Override public List<E> findAll() { return list(criteria()); }

    @Transactional(readOnly=true)
    @Override public E findByUuid(String uuid) { return uniqueResult(criteria().add(eq("uuid", uuid))); }

    @Transactional(readOnly=true)
    public List<E> findByUuids(Collection<String> uuids) {
        return list(criteria().add(Restrictions.in("uuid", uuids)));
    }

    @Transactional(readOnly=true)
    @Override public boolean exists(String uuid) { return findByUuid(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) { return entity; }
    @Override public E postCreate(E entity, Object context) { return entity; }

    @Override public E create(@Valid E entity) { return AbstractCRUDDAO.create(entity, this); }

    public static <E extends Identifiable> E create(E entity, AbstractCRUDDAO<E> dao) {
        entity.beforeCreate();
        final Object ctx = dao.preCreate(entity);
        setFlushMode(dao.getHibernateTemplate());
        entity.setUuid((String) dao.getHibernateTemplate().save(checkNotNull(entity)));
        dao.getHibernateTemplate().flush();
        return dao.postCreate(entity, ctx);
    }

    @Override public E createOrUpdate(@Valid E entity) {
        return (entity.getUuid() == null) ? create(entity) : update(entity);
    }
    public static <E extends Identifiable> E createOrUpdate(@Valid E entity, DAO<E> dao) {
        return (entity.getUuid() == null) ? dao.create(entity) : dao.update(entity);
    }

    public E upsert(@Valid E entity) {
        if (entity.getUuid() == null) throw new IllegalArgumentException("upsert: uuid must not be null");
        return exists(entity.getUuid()) ? update(entity) : create(entity);
    }

    @Override public Object preUpdate(@Valid E entity) { return entity; }
    @Override public E postUpdate(@Valid E entity, Object context) { return entity; }

    @Override public E update(@Valid E entity) {
        final Object ctx = preUpdate(entity);
        setFlushMode();
        entity = getHibernateTemplate().merge(checkNotNull(entity));
        getHibernateTemplate().flush();
        return postUpdate(entity, ctx);
    }

    public static <E extends Identifiable> E update(@Valid E entity, AbstractCRUDDAO<E> dao) {
        final Object ctx = dao.preUpdate(entity);
        setFlushMode(dao.getHibernateTemplate());
        entity = dao.getHibernateTemplate().merge(checkNotNull(entity));
        dao.getHibernateTemplate().flush();
        return dao.postUpdate(entity, ctx);
    }

    @Override public void delete(String uuid) {
        final E found = get(checkNotNull(uuid));
        setFlushMode();
        if (found != null) getHibernateTemplate().delete(found);
        getHibernateTemplate().flush();
    }

    @Transactional(readOnly=true)
    @Override public E findByUniqueField(String field, Object value) {
        return uniqueResult(eq(field, value));
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2) {
        return uniqueResult(and(eq(f1, v1), eq(f2, v2)));
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        return uniqueResult(and(eq(f1, v1), eq(f2, v2), eq(f3, v3)));
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByField(String field, Object value) {
        return list(criteria().add(eq(field, value)));
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2) {
        return list(criteria().add(and(eq(f1, v1), eq(f2, v2))));
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        return list(criteria().add(and(eq(f1, v1), eq(f2, v2), eq(f3, v3))));
    }

    @Transactional(readOnly=true)
    public E cacheLookup(String uuid, Map<String, E> cache) {
        final E thing = cache.get(uuid);
        return (thing != null) ? thing : findByUuid(uuid);
    }

    protected void setFlushMode() { setFlushMode(getHibernateTemplate()); }
    protected static void setFlushMode(HibernateTemplate template) { template.getSessionFactory().getCurrentSession().setFlushMode(FlushMode.COMMIT); }

}
