package org.cobbzilla.wizard.dao;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.wizard.model.Identifiable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.string.StringUtil.UTF8cs;

// todo: implement retries, where we tear down the client completely and rebuild it
// this is necessary if the memcached server is restarted while we're running
@Slf4j
public abstract class AbstractSessionDAO<T extends Identifiable> {

    private static final String PADDING_SUFFIX = "__PADDING__";

    private final MemcachedClient memcached;

    // what are we storing?
    protected abstract Class<T> getEntityClass();

    // what's the cipher key?
    protected abstract String getPassphrase();

    // override these if necessary
    protected int getMemcachedPort() { return 11211; }
    protected String getMemcachedHost() { return "127.0.0.1"; }

    public AbstractSessionDAO() {
        try {
            memcached = new XMemcachedClient(getMemcachedHost(), getMemcachedPort());
        } catch (IOException e) {
            throw new IllegalStateException("Error connecting to memcached: "+e, e);
        }
    }

    public String create (T thing) {
        final String sessionId = UUID.randomUUID().toString();
        set(sessionId, thing);
        return sessionId;
    }

    public T find(String uuid) {
        try {
            final byte[] rawData = memcached.get(uuid);
            if (rawData == null) return null;
            return deserialize(CryptoUtil.decrypt(rawData, getPassphrase()));

        } catch (Exception e) {
            log.error("Error reading from memcached: " + e, e);
            return null;
        }
    }

    private void set(String uuid, T thing) {
        try {
            final byte[] data = CryptoUtil.encrypt(serialize(thing), getPassphrase());
            memcached.set(uuid, (int) TimeUnit.DAYS.toSeconds(30), data);
        } catch (Exception e) {
            throw new IllegalStateException("Error writing to memcached: "+e, e);
        }
    }

    // override these for full control -- toJson/fromJson will not be called at all
    protected byte[] serialize(T thing) throws Exception { return pad(toJson(thing)); }
    protected T deserialize(byte[] data) throws Exception { return fromJson(unpad(new String(data))); }

    // override these to keep the padding but do your own json I/O
    protected String toJson(T thing) throws Exception { return JsonUtil.toJson(thing); }
    protected T fromJson(String json) throws Exception { return JsonUtil.fromJson(json, getEntityClass()); }

    private byte[] pad(String data) throws Exception {
        return (data + PADDING_SUFFIX + RandomStringUtils.random(1024)).getBytes(UTF8cs);
    }

    private String unpad(String data) {
        if (data == null) return null;
        int paddingPos = data.indexOf(PADDING_SUFFIX);
        if (paddingPos == -1) return null;
        return data.substring(0, paddingPos);
    }

    public void update(String uuid, T thing) {
        set(uuid, thing);
    }

    public void invalidate(String uuid) {
        try {
            memcached.delete(uuid);
        } catch (Exception e) {
            throw new IllegalStateException("Error deleting from memcached: "+e, e);
        }
    }

    public boolean isValid (String uuid) {
        try {
            return find(uuid) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

}
