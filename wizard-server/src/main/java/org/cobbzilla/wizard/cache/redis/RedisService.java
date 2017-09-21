package org.cobbzilla.wizard.cache.redis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.security.CryptoUtil.string_decrypt;
import static org.cobbzilla.util.security.CryptoUtil.string_encrypt;
import static org.cobbzilla.util.system.Sleep.sleep;

@Service @NoArgsConstructor @Slf4j
public class RedisService {

    public static final int MAX_RETRIES = 5;

    @Autowired @Getter @Setter private HasRedisConfiguration configuration;

    public String loadScript(String script) { return getRedis().scriptLoad(script); }

    public Object eval(String scriptsha, List<String> keys, List<String> args) {
        return getRedis().evalsha(scriptsha, keys, args);
    }

    @Getter @Setter private String key;
    protected boolean hasKey () { return !empty(getKey()); }

    private final AtomicReference<Jedis> redis = new AtomicReference<>();
    private Jedis newJedis() { return new Jedis(configuration.getRedis().getHost(), configuration.getRedis().getPort()); }

    @Getter @Setter private String prefix = null;

    public RedisService(HasRedisConfiguration configuration) {
        this(configuration, configuration.getRedis().getPrefix(), configuration.getRedis().getKey());
    }

    public RedisService(HasRedisConfiguration configuration, String prefix, String key) {
        this.configuration = configuration;
        this.prefix = prefix;
        this.key = key;
    }

    private Map<String, RedisService> prefixServiceCache = new ConcurrentHashMap<>();

    public RedisService prefixNamespace(String prefix) { return prefixNamespace(prefix, configuration.getRedis().getKey()); }

    public RedisService prefixNamespace(String prefix, String key) {
        RedisService r = prefixServiceCache.get(prefix);
        if (r == null) {
            String basePrefix = (this.prefix != null) ? this.prefix : configuration.getRedis().getPrefix();
            basePrefix = empty(basePrefix) ? "" : basePrefix + ".";
            r = new RedisService(configuration, basePrefix + prefix, key);
            prefixServiceCache.put(prefix, r);
        }
        return r;

    }

    public void reconnect () {
        log.debug("marking redis for reconnection...");
        synchronized (redis) {
            if (redis.get() != null) {
                try { redis.get().disconnect(); } catch (Exception e) {
                    log.warn("error disconnecting from redis before reconnecting: "+e);
                }
            }
            redis.set(null);
        }
    }

    private Jedis getRedis () {
        synchronized (redis) {
            if (redis.get() == null) {
                log.debug("connecting to redis...");
                redis.set(newJedis());
            }
        }
        return redis.get();
    }

    public <V> RedisMap<V> map (String prefix) { return map(prefix, null); }
    public <V> RedisMap<V> map (String prefix, Long duration) { return new RedisMap<>(prefix, duration, this); }

    public boolean exists(String key) { return __exists(key, 0, MAX_RETRIES); }

    public <T> T getObject(String key, Class<T> clazz) {
        final String json = get(key);
        return empty(json) ? null : fromJsonOrDie(json, clazz);
    }

    public String get(String key) { return decrypt(__get(key, 0, MAX_RETRIES)); }

    public String get_plaintext(String key) { return __get(key, 0, MAX_RETRIES); }

    public String lpop(String data) { return decrypt(__lpop(data, 0, MAX_RETRIES)); }

    public void set(String key, String value, String nxxx, String expx, long time) {
        __set(key, value, nxxx, expx, time, 0, MAX_RETRIES);
    }

    public void set(String key, String value, String expx, long time) {
        __set(key, value, "XX", expx, time, 0, MAX_RETRIES);
        __set(key, value, "NX", expx, time, 0, MAX_RETRIES);
    }

    public void set(String key, String value) { __set(key, value, 0, MAX_RETRIES); }

    public <T> void setObject(String key, T thing) { __set(key, toJsonOrDie(thing), 0, MAX_RETRIES); }

    public void lpush(String key, String value) { __lpush(key, value, 0, MAX_RETRIES); }

    public void del(String key) { __del(key, 0, MAX_RETRIES); }

    public void set_plaintext(String key, String value, String nxxx, String expx, long time) {
        __set(key, value, nxxx, expx, time, 0, MAX_RETRIES);
    }

    public void set_plaintext(String key, String value) {
        __set(key, value, 0, MAX_RETRIES);
    }

    public Long incr(String key) { return __incrBy(key, 1, 0, MAX_RETRIES); }

    public Long incrBy(String key, long value) { return __incrBy(key, value, 0, MAX_RETRIES); }
    public Long decr(String key) { return __decrBy(key, 1, 0, MAX_RETRIES); }

    public Long decrBy(String key, long value) { return __decrBy(key, value, 0, MAX_RETRIES); }

    public List<String> list(String key) { return __list(key, 0, MAX_RETRIES); }

    public Collection<String> keys(String key) { return __keys(key, 0, MAX_RETRIES); }

    // override these for full control
    protected String encrypt(String data) {
        if (!hasKey()) return data;
        return string_encrypt(data, getKey());
    }

    protected String decrypt(String data) {
        if (!hasKey()) return data;
        if (data == null) return null;
        return string_decrypt(data, getKey());
    }

    private void resetForRetry(int attempt, String reason) {
        reconnect();
        sleep(attempt * 10, reason);
    }

    private String prefix (String key) {
        return empty(prefix) ? key : prefix + "." + key;
    }

    private String __get(String key, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().get(prefix(key));
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__get");
            return __get(key, attempt+1, maxRetries);
        }
    }

    private boolean __exists(String key, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().exists(prefix(key));
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__exists");
            return __exists(key, attempt+1, maxRetries);
        }
    }

    private String __set(String key, String value, String nxxx, String expx, long time, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().set(prefix(key), encrypt(value), nxxx, expx, time);
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__set");
            return __set(key, value, nxxx, expx, time, attempt + 1, maxRetries);
        }
    }

    private String __set(String key, String value, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().set(prefix(key), encrypt(value));
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__set");
            return __set(key, value, attempt+1, maxRetries);
        }
    }

    private Long __lpush(String key, String value, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().lpush(prefix(key), encrypt(value));
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__lpush");
            return __lpush(key, value, attempt + 1, maxRetries);
        }
    }

    private String __lpop(String data, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().lpop(data);
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__lpop");
            return __lpop(data, attempt+1, maxRetries);
        }
    }

    private Long __del(String key, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().del(prefix(key));
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__del");
            return __del(key, attempt+1, maxRetries);
        }
    }

    private Long __incrBy(String key, long value, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().incrBy(prefix(key), value);
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__incrBy");
            return __incrBy(key, value, attempt + 1, maxRetries);
        }
    }

    private Long __decrBy(String key, long value, int attempt, int maxRetries) {
        try {
            synchronized (redis) {
                return getRedis().decrBy(prefix(key), value);
            }
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__decrBy");
            return __decrBy(key, value, attempt+1, maxRetries);
        }
    }

    private List<String> __list(String key, int attempt, int maxRetries) {
        try {
            final Long llen;
            synchronized (redis) {
                llen = getRedis().llen(prefix(key));
            }
            if (llen == null) return null;

            final List<String> range;
            synchronized (redis) {
                range = getRedis().lrange(prefix(key), 0, llen);
            }
            final List<String> list = new ArrayList<>(range.size());
            for (String item : range) list.add(decrypt(item));

            return list;

        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__list");
            return __list(key, attempt + 1, maxRetries);
        }
    }

    private Collection<String> __keys(String key, int attempt, int maxRetries) {
        try {
            final Set<String> keys;
            synchronized (redis) {
                keys = getRedis().keys(prefix(key));
            }
            return keys;

        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__keys");
            return __list(key, attempt + 1, maxRetries);
        }
    }

    public void flush() {
        final Collection<String> keys = keys(prefix("*"));
        for (String key : keys) {
            del(key);
        }
    }

}
