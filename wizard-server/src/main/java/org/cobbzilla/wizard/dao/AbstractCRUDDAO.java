package org.cobbzilla.wizard.dao;

/**
 * Forked from dropwizard https://github.com/dropwizard/
 * https://github.com/dropwizard/dropwizard/blob/master/LICENSE
 */

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.api.CrudOperation;
import org.cobbzilla.wizard.model.AuditLog;
import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.toMap;
import static org.hibernate.criterion.Restrictions.*;

@Transactional @Slf4j
public abstract class AbstractCRUDDAO<E extends Identifiable> extends AbstractDAO<E> {

    public <A extends AuditLog> AuditLogDAO<A> getAuditLogDAO() { return null; }
    public boolean auditingEnabled () { return getAuditLogDAO() != null; }

    @Transactional(readOnly=true)
    @Override public List<E> findAll() { return list(criteria()); }

    @Transactional(readOnly=true)
    @Override public E findByUuid(String uuid) { return findByUniqueField("uuid", uuid); }

    @Transactional(readOnly=true)
    public List<E> findByUuids(Collection<String> uuids) {
        return empty(uuids) ? new ArrayList<E>() : findByFieldIn("uuid", uuids);
    }

    @Transactional(readOnly=true)
    @Override public boolean exists(String uuid) { return findByUuid(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) {
        return auditingEnabled() ? audit(null, entity, CrudOperation.create) : entity;
    }
    @Override public E postCreate(E entity, Object context) {
        return auditingEnabled() ? commit_audit(entity, context) : entity;
    }

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

    @Override public Object preUpdate(@Valid E entity) {
        return auditingEnabled() ? audit(findByUuid(entity.getUuid()), entity, CrudOperation.update) : entity;
    }

    @Override public E postUpdate(E entity, Object context) {
        return auditingEnabled() ? commit_audit(entity, context) : entity;
    }

    @Override public E update(@Valid E entity) {
        entity.beforeUpdate();
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
        if (found != null) {
            final AuditLog auditLog = auditingEnabled() ? audit_delete(found) : null;
            getHibernateTemplate().delete(found);
            getHibernateTemplate().flush();
            if (auditLog != null) commit_audit_delete(auditLog);
        }
    }

    @Transactional(readOnly=true)
    @Override public E findByUniqueField(String field, Object value) {
        return uniqueResult(value == null ? isNull(field) : eq(field, value));
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        return uniqueResult(and(expr1, expr2));
    }

    @Transactional(readOnly=true)
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        return uniqueResult(and(expr1, expr2, expr3));
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByField(String field, Object value) {
        return list(criteria().add(eq(field, value)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldLike(String field, String value) {
        return list(criteria().add(ilike(field, value)).addOrder(Order.asc(field)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldEqualAndFieldLike(String eqField, Object eqValue, String likeField, String likeValue) {
        final Criterion expr1 = eqValue == null ? isNull(eqField) : eq(eqField, eqValue);
        return list(criteria().add(and(
                expr1,
                ilike(likeField, likeValue)
        )).addOrder(Order.asc(likeField)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldIn(String field, Object[] values) {
        return empty(values) ? new ArrayList<E>() : list(criteria().add(in(field, values)).addOrder(Order.asc(field)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldIn(String field, Collection<?> values) {
        return empty(values) ? new ArrayList<E>() : list(criteria().add(in(field, values)).addOrder(Order.asc(field)), 0, getFinderMaxResults());
    }

    protected int getFinderMaxResults() { return 100; }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        return list(criteria().add(and(expr1, expr2)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        return list(criteria().add(and(expr1, expr2, expr3)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public E cacheLookup(String uuid, Map<String, E> cache) {
        final E thing = cache.get(uuid);
        return (thing != null) ? thing : findByUuid(uuid);
    }

    protected void setFlushMode() { setFlushMode(getHibernateTemplate()); }
    public static void setFlushMode(HibernateTemplate template) { template.getSessionFactory().getCurrentSession().setFlushMode(FlushMode.COMMIT); }

    private static final String PROP_AUDIT_LOG = "__auditLog";

    private Object audit(E prevEntity, E newEntity, CrudOperation operation) {

        if (newEntity == null) die("audit("+operation.name()+"): newEntity cannot be null");

        AuditLog auditLog = getAuditLogDAO().newEntity()
                .setEntityType(getEntityClass().getName())
                .setEntityUuid(newEntity.getUuid())
                .setOperation(operation)
                .setPrevState(prevEntity == null ? null : toJsonOrDie(toMap(prevEntity)))
                .setNewState(toJsonOrDie(toMap(newEntity, getAuditFields(), getAuditExcludeFields())));

        auditLog = getAuditLogDAO().create(auditLog);

        final Map<String, Object> ctx = new HashMap<>();
        ctx.put(PROP_AUDIT_LOG, auditLog);
        return ctx;
    }

    protected String[] getAuditFields() { return null; }
    protected String[] getAuditExcludeFields() { return null; }

    private E commit_audit(E entity, Object context) {
        final Map<String, Object> ctx = (Map<String, Object>) context;
        final AuditLog auditLog = (AuditLog) ctx.get(PROP_AUDIT_LOG);
        auditLog.setSuccess(true);
        getAuditLogDAO().update(auditLog);
        return entity;
    }

    private AuditLog audit_delete(E found) {
        AuditLog auditLog = getAuditLogDAO().newEntity()
                .setEntityType(getEntityClass().getName())
                .setEntityUuid(found.getUuid())
                .setOperation(CrudOperation.delete)
                .setPrevState(toJsonOrDie(toMap(found)))
                .setNewState(null);

        return getAuditLogDAO().create(auditLog);
    }

    private void commit_audit_delete(AuditLog auditLog) {
        auditLog.setSuccess(true);
        getAuditLogDAO().update(auditLog);
    }

}
