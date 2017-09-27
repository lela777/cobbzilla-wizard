package org.cobbzilla.wizard.filters;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;
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

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@NoArgsConstructor @Slf4j
@Provider @Service
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy=true) private final RedisService cache = initCache();
    protected abstract RedisService initCache();

    @Getter(lazy=true) private final String scriptSha = initScript();
    public String initScript() {
        return getCache().loadScript(StreamUtil.stream2string(getPackagePath(RateLimitFilter.class)+"/api_limiter_redis.lua"));
    }

    @Getter private final static LoadingCache<String, List<String>> keys =
            CacheBuilder.newBuilder()
                        .maximumSize(1000)
                        .expireAfterAccess(5, TimeUnit.MINUTES)
                        .build(new CacheLoader<String, List<String>>() {
                            public List<String> load(String key) { return new SingletonList<>(key); }
                        });

    protected abstract List<String> getKeys(ContainerRequest request);

    protected abstract List<ApiRateLimit> getLimits();

    @Getter(lazy=true) private final List<ApiRateLimit> _limits = initLimits();
    private List<ApiRateLimit> initLimits() { return getLimits(); }

    @Getter(lazy=true) private final List<String> limitsAsStrings = initLimitsAsStrings();
    protected List<String> initLimitsAsStrings() {
        final List<ApiRateLimit> limits = getLimits();
        if (empty(limits)) return null;
        return limits.stream().map(x->new String[] {
                String.valueOf(x.getLimit()),
                String.valueOf(x.getIntervalDuration()),
                String.valueOf(x.getBlockDuration())
        }).flatMap(Arrays::stream).collect(Collectors.toList());
    }

    @Override public ContainerRequest filter(@Context ContainerRequest request) {

        if (getLimitsAsStrings() == null) return request; // noop

        final List<String> keys = getKeys(request);
        final Long i = (Long) getCache().eval(getScriptSha(), keys, getLimitsAsStrings());
        if (i != null) {
            final List<ApiRateLimit> limits = get_limits();
            if (i < 0 || i >= limits.size()) {
                log.warn("filter: unknown limit ("+i+") exceeded for keys: "+StringUtil.toString(keys));
            } else {
                log.warn("filter: limit ("+limits.get(i.intValue())+") exceeded for keys: "+StringUtil.toString(keys));
            }
            throw new WebApplicationException(Response.status(429).build());
        }

        return request;
    }

}
