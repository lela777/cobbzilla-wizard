package org.cobbzilla.wizard.dao;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.model.Identifiable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

@Slf4j
public abstract class AbstractSessionDAO<T extends Identifiable> {

    @Autowired private RedisService redis;

    // what are we storing?
    protected Class<T> getEntityClass() { return getFirstTypeParam(getClass(), Identifiable.class); }

    // what's the cipher key?
    protected abstract String getPassphrase();

    public String create (T thing) {
        final String sessionId = randomUUID().toString();
        set(sessionId, thing, false);
        return sessionId;
    }

    public T find(String uuid) {
        if (empty(uuid)) return null;
        try {
            final String found = redis.get(uuid);
            if (found == null) return null;
            return fromJson(found);

        } catch (Exception e) {
            log.error("Error reading from redis: " + e, e);
            return null;
        }
    }

    public void invalidateAllSessions(String uuid) {
        String sessionId;
        while ((sessionId = redis.lpop(uuid)) != null) {
            invalidate(sessionId);
        }
        invalidate(uuid);
    }

    private void set(String uuid, T thing, boolean shouldExist) {
        redis.set(uuid, toJson(thing), shouldExist ? "XX" : "NX", "EX", TimeUnit.DAYS.toSeconds(30));
        redis.lpush(thing.getUuid(), uuid);
    }

    // override these to keep the padding but do your own json I/O
    protected String toJson(T thing) { return JsonUtil.toJsonOrDie(thing); }
    protected T fromJson(String json) { return JsonUtil.fromJsonOrDie(json, getEntityClass()); }

    public void update(String uuid, T thing) { set(uuid, thing, true); }

    public void invalidate(String uuid) { redis.del(uuid); }

    public boolean isValid (String uuid) { return find(uuid) != null; }

}
