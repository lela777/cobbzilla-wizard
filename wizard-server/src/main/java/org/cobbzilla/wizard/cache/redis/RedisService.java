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

import java.util.List;

@Service @Slf4j
public class RedisService {

    @Autowired @Getter @Setter private HasRedisConfiguration configuration;
    private String getKey() { return configuration.getRedis().getKey(); }

    @Getter(lazy=true) private final Jedis redis = initJedis();

    private Jedis initJedis() { return new Jedis(configuration.getRedis().getHost(), configuration.getRedis().getPort()); }

    public String get(String key) { return decrypt(getRedis().get(key)); }

    public String lpop(String data) { return decrypt(getRedis().lpop(data)); }

    public void set(String key, String value, String exxx, String nxex, long time) {
        getRedis().set(key, encrypt(value), exxx, nxex, time);
    }

    public void set(String key, String value) { getRedis().set(key, encrypt(value)); }

    public void lpush(String key, String value) { getRedis().lpush(key, encrypt(value)); }

    public void del(String key) { getRedis().del(key); }

    public void set_plaintext(String key, String value, String exxx, String nxex, long time) {
        getRedis().set(key, value, exxx, nxex, time);
    }

    public void set_plaintext(String key, String value) {
        getRedis().set(key, value);
    }

    public Long incr(String key) { return getRedis().incr(key); }
    public Long incrBy(String key, long value) { return getRedis().incrBy(key, value); }

    public Long decr(String key) { return getRedis().decr(key); }
    public Long decrBy(String key, long value) { return getRedis().decrBy(key, value); }

    public List<String> list(String key) {
        final Long llen = getRedis().llen(key);
        if (llen == null) return null;
        return getRedis().lrange(key, 0, llen);
    }

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

}
