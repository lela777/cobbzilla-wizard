package org.cobbzilla.wizard.dao;

import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.model.ExpirableBase;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public abstract class AbstractRedisDAO<T extends ExpirableBase> implements DAO<T> {

    public abstract Class<T> getEntityClass();

    @Autowired private RedisService redis;

    // not supported
    @Override public SearchResults<T> search(ResultPage resultPage) { return notSupported(); }
    @Override public T findByUniqueField(String field, Object value) { return notSupported(); }
    @Override public List<T> findAll() { return notSupported(); }

    // default implementations
    @Override public Object preCreate(@Valid T entity) { if (!entity.hasUuid()) entity.initUuid(); return entity; }
    @Override public T postCreate(T entity, Object context) { return entity; }
    @Override public Object preUpdate(@Valid T entity) { return entity; }
    @Override public T postUpdate(@Valid T entity, Object context) { return entity; }

    // get something
    @Override public T get(Serializable id) {
        final String json = redis.get(id.toString());
        return json == null ? null : JsonUtil.fromJsonOrDie(json, getEntityClass());
    }

    @Override public T findByUuid(String uuid) { return get(uuid); }

    @Override public boolean exists(String uuid) { return get(uuid) != null; }

    // set something
    @Override public T create(@Valid T entity) {
        redis.set(entity.getUuid(), JsonUtil.toJsonOrDie(entity), "NX", "EX", entity.getExpirationSeconds());
        return entity;
    }

    @Override public T update(@Valid T entity) {
        redis.set(entity.getUuid(), JsonUtil.toJsonOrDie(entity));
        return entity;
    }

    @Override public T createOrUpdate(@Valid T entity) {
        return entity.hasUuid() ? update(entity) : create(entity);
    }

    // delete something
    @Override public void delete(String uuid) { redis.del(uuid); }

}
