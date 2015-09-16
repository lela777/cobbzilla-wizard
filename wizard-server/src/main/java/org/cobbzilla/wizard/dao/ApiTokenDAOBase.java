package org.cobbzilla.wizard.dao;

import lombok.AccessLevel;
import lombok.Getter;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import org.cobbzilla.wizard.model.ApiToken;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ApiTokenDAOBase {

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final MemcachedClient client = initMemcachedClient();

    private MemcachedClient initMemcachedClient() {
        MemcachedClientBuilder builder = new XMemcachedClientBuilder("127.0.0.1:11211");
        try {
            final MemcachedClient c = builder.build();
            c.setPrimitiveAsString(true);
            return c;

        } catch (Exception e) {
            return die("initMemcachedClient: error: "+e, e);
        }
    }

    public ApiToken generateNewToken (String accountUuid) {
        final ApiToken token = newToken();
        try {
            if (!getClient().add(token.getToken(), ApiToken.EXPIRATION_SECONDS, accountUuid)) {
                die("generateNewToken: error writing to memcached: call returned false");
            }
        } catch (Exception e) {
            die("generateNewToken: error writing to memcached: "+e, e);
        }
        return token;
    }

    private ApiToken newToken() {
        final ApiToken token = new ApiToken();
        token.init();
        return token;
    }

    public String findAccount (String token) {
        try {
            return getClient().get(token);
        } catch (Exception e) {
            return die("findAccount: error reading from memcached: "+e, e);
        }
    }

    public void cancel(String token) {
        try {
            getClient().delete(token);
        } catch (Exception e) {
            die("cancel: error deleting from memcached: " + e, e);
        }
    }

    public ApiToken refreshToken(ApiToken token) {
        final ApiToken newToken = newToken();
        try {
            final String account = getClient().get(token.getToken());

            if (account == null) return null; // token not found

            // create new token
            if (getClient().set(newToken.getToken(), ApiToken.EXPIRATION_SECONDS, account)) return null;

            // delete old token (ignore return code)
            getClient().delete(token.getToken());

            return newToken;

        } catch (Exception e) {
            die("refreshToken: error talking to memcached: "+e, e); return null;
        }
    }
}
