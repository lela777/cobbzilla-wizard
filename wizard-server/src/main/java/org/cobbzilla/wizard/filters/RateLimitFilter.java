package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.LinkedList;
import java.util.Queue;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    protected final static int max = 100;

    @Getter(lazy=true) private final RedisService buckets = initCache();
    protected abstract RedisService initCache();

    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        final String key = getKey(request);
        //maybe check if user uses invalidated api-token
        long timestamp = now();
        Bucket bucket = getBuckets().getObject(key, getBucketClass());
        if (empty(bucket)) {
            bucket = buildBucket(timestamp);
        }

        useBucket(bucket, request, timestamp);

        getBuckets().setObject(key, bucket);

        if (bucket.tooMany || bucket.blocked)
            throw new WebApplicationException(Response.status(429).build());
        return request;
    }

    protected abstract Class<? extends Bucket>  getBucketClass();

    protected abstract void useBucket(Bucket bucket, ContainerRequest request, long timestamp);

    protected abstract String getKey(ContainerRequest request);

    protected abstract Bucket buildBucket(long timestamp);

    @AllArgsConstructor @NoArgsConstructor
    protected static class Bucket {
        @Getter @Setter protected Integer tokens;
        @Getter @Setter protected int breach = 0;
        @Getter @Setter protected long last_refresh;
        @Getter @Setter protected Queue<Long> triggers;
        @Getter @Setter protected long block_time = 0;
        @Getter @Setter protected boolean tooMany = false;
        @Getter @Setter protected boolean blocked = false;

        protected Bucket(int tokens, long timestamp) {
            this.tokens = tokens;
            last_refresh = timestamp;
            triggers = new LinkedList<>();
        }


    }

}
