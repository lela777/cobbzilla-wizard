package org.cobbzilla.wizard.dao;

/**
 * Forked from dropwizard https://github.com/dropwizard/
 * https://github.com/dropwizard/dropwizard/blob/master/LICENSE
 */

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.api.CrudOperation;
import org.cobbzilla.wizard.model.AuditLog;
import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.FlushMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.reflect.ReflectionUtil.toMap;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;
import static org.hibernate.criterion.Restrictions.*;

@Transactional @Slf4j
public abstract class AbstractCRUDDAO<E extends Identifiable> extends AbstractDAO<E> {

    public static final String NO_SUB_KEY = "__no_subkey";

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
    public E findFirstByUuids(Collection<String> uuids) { return findFirstByFieldIn("uuid", uuids); }

    @Transactional(readOnly=true)
    @Override public boolean exists(String uuid) { return findByUuid(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) {
        try {
            return auditingEnabled() ? audit(null, entity, CrudOperation.create) : entity;
        } finally {
            flushObjectCache(entity);
        }
    }

    protected String subCacheAttribute () { return null; }

    public void flushObjectCache(E entity) {
        synchronized (ocache) {
            if (ocache.get().isEmpty()) return;

            final String subCacheAttr = subCacheAttribute();
            final Object val = (subCacheAttr != null) ? ReflectionUtil.get(entity, subCacheAttr) : null;

            if (val != null) {
                Map<Object, Object> subCache = (Map<Object, Object>) ocache.get().get(val);
                if (subCache != null && !subCache.isEmpty()) {
                    subCache = new ConcurrentHashMap<>();
                    ocache.get().put(val.toString(), subCache);
                }
            } else {
                ocache.set(new ConcurrentHashMap<>());
            }

            final Map globalCache = (Map) ocache.get().get(NO_SUB_KEY);
            if (!empty(globalCache)) {
                ocache.get().put(NO_SUB_KEY, new ConcurrentHashMap<>());
            }
        }
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
        try {
            return auditingEnabled() ? audit(findByUuid(entity.getUuid()), entity, CrudOperation.update) : entity;
        } finally {
            flushObjectCache(entity);
        }
    }

    @Override public E postUpdate(E entity, Object context) {
        return auditingEnabled() ? commit_audit(entity, context) : entity;
    }

    @Override public E update(@Valid E entity) { return update(entity, this); }

    public static <E extends Identifiable> E update(@Valid E entity, AbstractCRUDDAO<E> dao) {
        entity.beforeUpdate();
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
            flushObjectCache(found);
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
    public E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3, String f4, Object v4) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        final Criterion expr4 = v4 == null ? isNull(f4) : eq(f4, v4);
        return uniqueResult(and(expr1, expr2, expr3, expr4));
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByField(String field, Object value) {
        return list(sort(criteria().add(eq(field, value))), 0, getFinderMaxResults());
    }

    protected DetachedCriteria sort(DetachedCriteria criteria) {
        final Order order = getDefaultSortOrder();
        return order == null ? criteria : criteria.addOrder(order);
    }

    public Order getDefaultSortOrder() { return null; }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldLike(String field, String value) {
        return list(criteria().add(ilike(field, value)).addOrder(Order.asc(field)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public List<E> findByFieldLike(String field, String value, Order order) {
        return list(criteria().add(ilike(field, value)).addOrder(order), 0, getFinderMaxResults());
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
    public List<E> findByFieldsEqualAndFieldLike(String f1, Object v1, String f2, Object v2, String likeField, String likeValue) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        return list(criteria().add(and(
                expr1,
                expr2,
                ilike(likeField, likeValue)
        )).addOrder(Order.asc(likeField)), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public List<E> findByFieldsEqualAndFieldLike(String f1, Object v1, String f2, Object v2, String f3, Object v3, String likeField, String likeValue) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        return list(criteria().add(and(
                expr1,
                expr2,
                expr3,
                ilike(likeField, likeValue)
        )).addOrder(Order.asc(likeField)), 0, getFinderMaxResults());
    }

    public DetachedCriteria buildFindInCriteria(String field, @NotNull Object[] values) {
        return criteria().add(in(field, values)).addOrder(Order.asc(field));
    }

    public DetachedCriteria buildFindInCriteria(String field, @NotNull Collection<?> values) {
        return criteria().add(in(field, values)).addOrder(Order.asc(field));
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldIn(String field, Object[] values) {
        return empty(values) ? new ArrayList<E>() : list(buildFindInCriteria(field, values), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    @Override public List<E> findByFieldIn(String field, Collection<?> values) {
        return empty(values) ? new ArrayList<E>() : list(buildFindInCriteria(field, values), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public E findFirstByFieldIn(String field, Object[] values) {
        return empty(values) ? null : first(buildFindInCriteria(field, values));
    }

    @Transactional(readOnly=true)
    public E findFirstByFieldIn(String field, Collection<?> values) {
        return empty(values) ? null : first(buildFindInCriteria(field, values));
    }

    protected int getFinderMaxResults() { return 100; }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        return list(sort(criteria().add(and(expr1, expr2))), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        return list(sort(criteria().add(and(expr1, expr2, expr3))), 0, getFinderMaxResults());
    }

    @Transactional(readOnly=true)
    public List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3, String f4, Object v4) {
        final Criterion expr1 = v1 == null ? isNull(f1) : eq(f1, v1);
        final Criterion expr2 = v2 == null ? isNull(f2) : eq(f2, v2);
        final Criterion expr3 = v3 == null ? isNull(f3) : eq(f3, v3);
        final Criterion expr4 = v4 == null ? isNull(f4) : eq(f4, v4);
        return list(sort(criteria().add(and(expr1, expr2, expr3, expr4))), 0, getFinderMaxResults());
    }

    @Getter private final AtomicReference<Map<String, Object>> ocache = new AtomicReference<>(new ConcurrentHashMap<>());

    private static final Object NULL_OBJECT = new Object();
    private static final AtomicInteger cacheHits = new AtomicInteger(0);
    private static final AtomicInteger cacheMisses = new AtomicInteger(0);
    private static final AtomicLong cacheMissTime = new AtomicLong(0);

    @Transactional(readOnly=true)
    public <T> T cacheLookup(String cacheKey, Function<Object[], T> lookup, Object... args) {
        return cacheLookup(cacheKey, NO_SUB_KEY, lookup, args);
    }

    @Transactional(readOnly=true)
    public <T> T cacheLookup(String cacheKey, String cacheSubKey, Function<Object[], T> lookup, Object... args) {
        final String subCacheAttr = subCacheAttribute();
        final Map<String, Object> c = subCacheAttr == null ? ocache.get() : (Map<String, Object>) ocache.get().computeIfAbsent(cacheSubKey, o -> new ConcurrentHashMap<String, Object>());
        if (!c.containsKey(cacheKey)) {
            synchronized (c) {
                if (!c.containsKey(cacheKey)) {
                    final long start = now();
                    final T thing;
                    try {
                        thing = lookup.apply(args);
                    } catch (Exception e) {
                        return die("cacheLookup: lookup failed: "+e, e);
                    }
                    final long end = now();
                    int misses = cacheMisses.incrementAndGet();
                    long missTime = cacheMissTime.addAndGet(end - start);
                    if (misses % 1000 == 0) log.info("DAO-cache: "+misses+" misses took "+cacheMissTime + " to look up, average of "+(missTime/misses)+"ms per lookup");

                    c.put(cacheKey, thing == null ? NULL_OBJECT : cacheCopy(thing));
                    return thing;
                } else {
                    return getOrNull(cacheKey, c);
                }
            }
        } else {
            return getOrNull(cacheKey, c);
        }
    }

    private <T> T getOrNull(String cacheKey, Map<String, Object> c) {
        int hits = cacheHits.incrementAndGet();
        if (hits % 1000 == 0) log.info("DAO-cache: "+hits+" cache hits, saved "+formatDuration(hits*(cacheMissTime.get()/cacheMisses.get())));
        final T thing = (T) c.get(cacheKey);
        return thing == NULL_OBJECT ? null : thing;
    }

    private <T> T cacheCopy(T thing) {
        if (thing == NULL_OBJECT) return null;
        if (empty(thing)) return thing;
        try {
            if (thing instanceof Collection) {
                final Collection c = (Collection) instantiate(thing.getClass());
                for (Iterator iter = ((Collection) thing).iterator(); iter.hasNext(); ) {
                    final Object element = iter.next();
                    c.add(cacheCopy(element));
                }
                return (T) c;
            } else if (thing.getClass().isArray() && !empty(thing)) {
                final Object[] a = (Object[]) instantiate(thing.getClass());
                for (int i=0; i<((Object[]) thing).length; i++) {
                    a[i] = cacheCopy(((Object[]) thing)[i]);
                }
                return (T) a;
            } else {
                // force set uuid
                final T copyOfThing = instantiate((Class<T>) thing.getClass());
                ReflectionUtil.copy(copyOfThing, thing);
                return copyOfThing;
            }
        } catch (Exception e) {
            return die("cacheCopy: error copying: " + thing + ": " + e, e);
        }
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
