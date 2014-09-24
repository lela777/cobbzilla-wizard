package org.cobbzilla.wizard.dao;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.wizard.model.Identifiable;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

import static java.util.UUID.randomUUID;
import static org.cobbzilla.util.string.StringUtil.empty;

// todo: implement retries, where we tear down the client completely and rebuild it
// this is necessary if the redis server is restarted while we're running
@Slf4j
public abstract class AbstractSessionDAO<T extends Identifiable> {

    private static final String PADDING_SUFFIX = "__PADDING__";

    private final Jedis redis;

    // what are we storing?
    protected abstract Class<T> getEntityClass();

    // what's the cipher key?
    protected abstract String getPassphrase();

    // override these if necessary
    protected int getRedisPort() { return 6379; }
    protected String getRedisHost () { return "127.0.0.1"; }

    public AbstractSessionDAO() { redis = new Jedis(getRedisHost(), getRedisPort()); }

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
            return deserialize(found);

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
        redis.set(uuid, serialize(thing), shouldExist ? "XX" : "NX", "EX", (int) TimeUnit.DAYS.toSeconds(30));
        redis.lpush(thing.getUuid(), uuid);
    }

    // override these for full control -- toJson/fromJson will not be called at all
    protected String serialize(T thing) {
        try { return Base64.encodeBytes(CryptoUtil.encryptOrDie(pad(toJson(thing)).getBytes(), getPassphrase())); } catch (Exception e) {
            throw new IllegalStateException("Error serializing: "+thing, e);
        }
    }
    protected T deserialize(String data) throws Exception { return fromJson(unpad(new String(CryptoUtil.decrypt(Base64.decode(data), getPassphrase())))); }

    // override these to keep the padding but do your own json I/O
    protected String toJson(T thing) throws Exception { return JsonUtil.toJson(thing); }
    protected T fromJson(String json) throws Exception { return JsonUtil.fromJson(json, getEntityClass()); }

    private String pad(String data) throws Exception {
        return data + PADDING_SUFFIX + RandomStringUtils.random(1024);
    }

    private String unpad(String data) {
        if (data == null) return null;
        int paddingPos = data.indexOf(PADDING_SUFFIX);
        if (paddingPos == -1) return null;
        return data.substring(0, paddingPos);
    }

    public void update(String uuid, T thing) {
        set(uuid, thing, true);
    }

    public void invalidate(String uuid) {
        redis.del(uuid);
    }

    public boolean isValid (String uuid) {
        return find(uuid) != null;
    }

}
