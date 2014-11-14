package org.cobbzilla.wizard.cache.redis;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service @Slf4j
public class ActivationCodeService {

    @Autowired @Getter @Setter private RedisService redis;

    public String peek(String key) { return redis.get(key); }

    public boolean attempt(String key, String claimant) {
        try {
            final Long remaining = redis.decr(key);
            if (remaining == null || remaining < 0) return false;
            redis.lpush(getClaimantsKey(key), claimant);
            return true;

        } catch (Exception e) {
            log.warn("attempt("+key+") error: "+e);
            return false;
        }
    }

    public void define (String key, int quantity, long expirationSeconds) {
        redis.set_plaintext(key, String.valueOf(quantity), "XX", "EX", expirationSeconds);
    }

    public List<String> getClaimants (String key) { return redis.list(getClaimantsKey(key)); }

    private String getClaimantsKey(String key) { return key+"_claimed"; }

    /**
     * @param args [0] = redis key; [1] = key; [2] = quantity; [3] = expiration (# days)
     */
    public static void main (String[] args) {

        final String redisKey = args[0];
        final RedisService redis = new RedisService();
        redis.setConfiguration(new HasRedisConfiguration() {
            @Override public RedisConfiguration getRedis() { return new RedisConfiguration(redisKey); }
        });

        final ActivationCodeService acService = new ActivationCodeService();
        acService.setRedis(redis);

        final String key = args[1];

        if (args.length > 2) {
            final int quantity = Integer.parseInt(args[2]);
            final long expirationSeconds = Integer.parseInt(args[3]) * TimeUnit.DAYS.toSeconds(1);

            acService.define(key, quantity, expirationSeconds);
            System.out.println("successfully defined key: " + key);

        } else {
            System.out.println("key: " + key);
            System.out.println("remaining: " + acService.peek(key));
            System.out.println("claimants: " + StringUtil.toString(acService.getClaimants(key), ", "));
        }
    }

}
