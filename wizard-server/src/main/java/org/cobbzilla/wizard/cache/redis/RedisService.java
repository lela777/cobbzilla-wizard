package org.cobbzilla.wizard.cache.redis;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.util.string.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.system.Sleep.sleep;

@Service @Slf4j
public class RedisService {

    public static final int MAX_RETRIES = 5;

    @Autowired @Getter @Setter private HasRedisConfiguration configuration;
    private String getKey() { return configuration.getRedis().getKey(); }

    private final AtomicReference<Jedis> redis = new AtomicReference<>();
    private Jedis newJedis() { return new Jedis(configuration.getRedis().getHost(), configuration.getRedis().getPort()); }

    public void reconnect () {
        log.info("marking redis for reconnection...");
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
                log.info("connecting to redis...");
                redis.set(newJedis());
            }
        }
        return redis.get();
    }

    public String get(String key) { return decrypt(__get(key, 0, MAX_RETRIES)); }

    public String get_plaintext(String key) { return __get(key, 0, MAX_RETRIES); }

    public String lpop(String data) { return decrypt(__lpop(data, 0, MAX_RETRIES)); }

    public void set(String key, String value, String exxx, String nxex, long time) {
        __set(key, value, exxx, nxex, time, 0, MAX_RETRIES);
    }

    public void set(String key, String value) { __set(key, value, 0, MAX_RETRIES); }

    public void lpush(String key, String value) { __lpush(key, value, 0, MAX_RETRIES); }

    public void del(String key) { __del(key, 0, MAX_RETRIES); }

    public void set_plaintext(String key, String value, String exxx, String nxex, long time) {
        __set(key, value, exxx, nxex, time, 0, MAX_RETRIES);
    }

    public void set_plaintext(String key, String value) {
        __set(key, value, 0, MAX_RETRIES);
    }

    public Long incr(String key) { return __incrBy(key, 1, 0, MAX_RETRIES); }

    public Long incrBy(String key, long value) { return __incrBy(key, value, 0, MAX_RETRIES); }
    public Long decr(String key) { return __decrBy(key, 1, 0, MAX_RETRIES); }

    public Long decrBy(String key, long value) { return __decrBy(key, value, 0, MAX_RETRIES); }

    public List<String> list(String key) { return __list(key, 0, MAX_RETRIES); }

    // override these for full control -- toJson/fromJson will not be called at all
    protected String encrypt(String data) {
        try { return Base64.encodeBytes(CryptoUtil.encryptOrDie(pad(data).getBytes(), getKey())); } catch (Exception e) {
            throw new IllegalStateException("Error encrypting: "+e, e);
        }
    }

    protected String decrypt(String data) {
        if (data == null) return null;
        try { return unpad(new String(CryptoUtil.decrypt(Base64.decode(data), getKey()))); } catch (Exception e) {
            throw new IllegalStateException("Error decrypting: "+e, e);
        }
    }

    private static final String PADDING_SUFFIX = "__PADDING__";

    private String pad(String data) throws Exception { return data + PADDING_SUFFIX + RandomStringUtils.random(128); }

    private String unpad(String data) {
        if (data == null) return null;
        int paddingPos = data.indexOf(PADDING_SUFFIX);
        if (paddingPos == -1) return null;
        return data.substring(0, paddingPos);
    }

    private void resetForRetry(int attempt, String reason) {
        reconnect();
        sleep(attempt * 10, reason);
    }

    private String __get(String key, int attempt, int maxRetries) {
        try {
            return getRedis().get(key);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__get");
            return __get(key, attempt+1, maxRetries);
        }
    }

    private String __set(String key, String value, String exxx, String nxex, long time, int attempt, int maxRetries) {
        try {
            return getRedis().set(key, encrypt(value), exxx, nxex, time);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__set");
            return __set(key, value, exxx, nxex, time, attempt + 1, maxRetries);
        }
    }

    private String __set(String key, String value, int attempt, int maxRetries) {
        try {
            return getRedis().set(key, encrypt(value));
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__set");
            return __set(key, value, attempt+1, maxRetries);
        }
    }

    private Long __lpush(String key, String value, int attempt, int maxRetries) {
        try {
            return getRedis().lpush(key, encrypt(value));
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__lpush");
            return __lpush(key, value, attempt + 1, maxRetries);
        }
    }

    private String __lpop(String data, int attempt, int maxRetries) {
        try {
            return getRedis().lpop(data);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__lpop");
            return __lpop(data, attempt+1, maxRetries);
        }
    }

    private Long __del(String key, int attempt, int maxRetries) {
        try {
            return getRedis().del(key);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__del");
            return __del(key, attempt+1, maxRetries);
        }
    }

    private Long __incrBy(String key, long value, int attempt, int maxRetries) {
        try {
            return getRedis().incrBy(key, value);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__incrBy");
            return __incrBy(key, value, attempt + 1, maxRetries);
        }
    }

    private Long __decrBy(String key, long value, int attempt, int maxRetries) {
        try {
            return getRedis().decrBy(key, value);
        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__decrBy");
            return __decrBy(key, value, attempt+1, maxRetries);
        }
    }

    private List<String> __list(String key, int attempt, int maxRetries) {
        try {
            final Long llen = getRedis().llen(key);
            if (llen == null) return null;

            final List<String> range = getRedis().lrange(key, 0, llen);
            final List<String> list = new ArrayList<>(range.size());
            for (String item : range) list.add(decrypt(item));

            return list;

        } catch (RuntimeException e) {
            if (attempt > maxRetries) throw e;
            resetForRetry(attempt, "retrying RedisService.__list");
            return __list(key, attempt + 1, maxRetries);
        }
    }

}
