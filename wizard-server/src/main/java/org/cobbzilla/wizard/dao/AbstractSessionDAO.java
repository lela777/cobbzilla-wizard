package org.cobbzilla.wizard.dao;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.CryptoUtil;
import org.cobbzilla.wizard.model.Identifiable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
        final String uuid = UUID.randomUUID().toString();
        thing.setUuid(uuid);
        set(uuid, thing);
        return uuid;
    }

    private void set(String uuid, T thing) {
        try {
            final byte[] data = CryptoUtil.encrypt(pad(thing), getPassphrase());
            memcached.set(uuid, (int) TimeUnit.DAYS.toSeconds(30), data);
        } catch (Exception e) {
            throw new IllegalStateException("Error writing to memcached: "+e, e);
        }
    }

    private byte[] pad(T thing) throws Exception {
        return (JsonUtil.toJson(thing) + PADDING_SUFFIX + RandomStringUtils.random(1024)).getBytes();
    }

    private String unpad(String json) {
        if (json == null) return null;
        int paddingPos = json.indexOf(PADDING_SUFFIX);
        if (paddingPos == -1) return null;
        return json.substring(0, paddingPos);
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

    public T find(String uuid) {
        try {
            final byte[] data = memcached.get(uuid);
            if (data == null) return null;

            final String json = new String(CryptoUtil.decrypt(data, getPassphrase()));
            return JsonUtil.fromJson(unpad(json), getEntityClass());

        } catch (Exception e) {
            throw new IllegalStateException("Error reading from memcached: "+e, e);
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
