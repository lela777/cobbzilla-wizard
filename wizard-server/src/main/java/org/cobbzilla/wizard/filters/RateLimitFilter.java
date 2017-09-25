package org.cobbzilla.wizard.filters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy = true) private final RedisService cache = initCache();
    protected abstract RedisService initCache();

    @Getter(lazy = true) private final String scriptSha = initScript();
    public String initScript() {
        return getCache().loadScript(StreamUtil.stream2string("org/cobbzilla/wizard/filters/api_limiter_redis.lua"));
    }

    @Getter private final static LoadingCache<String, List<String>> keys =
            CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .build(
                                new CacheLoader<String, List<String>>() {
                                    public List<String> load(String key) {
                                        return new SingletonList<>(key);
                                    }
                                });

    protected abstract List<String> getKey(ContainerRequest request);

    protected abstract List<ApiRateLimit> getLimits();

    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        Long i = checkOverflow(request);
        if (i != null) {
            //handleRejection(i); To be implemented
            throw new WebApplicationException(Response.status(429).build());
        }
        return request;
    }

    public Long checkOverflow(ContainerRequest request) {
        return (Long) getCache().eval(getScriptSha(), getKey(request), getLimits().stream().map(
                x->new String[]{String.valueOf(x.limit), String.valueOf(x.interval), String.valueOf(x.block)})
                                                                                  .flatMap(Arrays::stream)
                                                                                  .collect(Collectors.toList()));
    }
}
