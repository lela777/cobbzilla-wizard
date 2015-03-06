package org.cobbzilla.wizard.cache.memcached;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import org.cobbzilla.util.security.CryptoUtil;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.system.Sleep.sleep;

@Service @Slf4j @NoArgsConstructor @AllArgsConstructor
public class MemcachedService {

    public static final int MAX_RETRIES = 5;

    public MemcachedService (String host, int port, String key) {
        this(new MemcachedConfiguration(host, port, key));
    }

    @Getter @Setter private MemcachedConfiguration configuration;

    private final AtomicReference<MemcachedClient> memcached = new AtomicReference<>();
    private MemcachedClient newClient() {
        try {
            return new XMemcachedClient(configuration.getHost(), configuration.getPort());
        } catch (Exception e) {
            return die("Error creating client: "+e, e);
        }
    }

    private MemcachedClient getMemcached () {
        synchronized (memcached) {
            if (memcached.get() == null) {
                log.info("connecting to memcached...");
                memcached.set(newClient());
            }
        }
        return memcached.get();
    }

    public String get (String name) {
        return decrypt(__get(name, 0, MAX_RETRIES));
    }

    public boolean set (String name, String value, int expirationSeconds) {
        return __set(name, encrypt(value), expirationSeconds, 0, MAX_RETRIES);
    }

    private String __get(String name, int attempt, int maxRetries) {
        try {
            return getMemcached().get(name);
        } catch (Exception e) {
            if (attempt > maxRetries) {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                die("__get: maxRetries (" + MAX_RETRIES + ") exceeded, last exception: " + e, e);
            }
            resetForRetry(attempt, "retrying MemcachedService.__get");
            return __get(name, attempt+1, maxRetries);
        }
    }

    private boolean __set(String name, String value, int expirationSeconds, int attempt, int maxRetries) {
        try {
            return getMemcached().set(name, expirationSeconds, value);
        } catch (Exception e) {
            if (attempt > maxRetries) {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                die("__set: maxRetries (" + MAX_RETRIES + ") exceeded, last exception: "+e, e);
            }
            resetForRetry(attempt, "retrying MemcachedService.__set");
            return __set(name, value, expirationSeconds, attempt+1, maxRetries);
        }
    }

    // override these for full control
    protected String encrypt(String data) {
        if (!configuration.hasKey()) return data;
        return CryptoUtil.string_encrypt(data, configuration.getKey());
    }

    protected String decrypt(String data) {
        if (!configuration.hasKey()) return data;
        if (data == null) return null;
        return CryptoUtil.string_decrypt(data, configuration.getKey());
    }

    private void resetForRetry(int attempt, String reason) {
        reconnect();
        sleep(attempt * 10, reason);
    }

    public void reconnect () {
        log.info("marking memcached for reconnection...");
        synchronized (memcached) {
            if (memcached.get() != null) {
                try { memcached.get().shutdown(); } catch (Exception e) {
                    log.warn("error shutting down memcached client before reconnecting: "+e);
                }
            }
            memcached.set(null);
        }
    }

}
