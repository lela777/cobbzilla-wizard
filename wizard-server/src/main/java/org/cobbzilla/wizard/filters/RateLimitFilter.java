package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;


@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy=true) private final RedisService buckets = initCache();
    protected abstract RedisService initCache();

    @Getter(lazy=true) private final String scriptSha = initScript();
    public String initScript() {
        return getBuckets().loadScript(StreamUtil.loadResourceAsStringOrDie("api/api_limiter_redis.lua"));
    }

    protected abstract  List<String> getList();

    protected abstract String getKey(ContainerRequest request);

    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        Long i = checkOverflow(request);
        if (!ZillaRuntime.empty(i)) {
            throw new WebApplicationException(Response.status(429).build());
        }
        return request;
    }

    public Long checkOverflow(ContainerRequest request) {
        return (Long)getBuckets().eval(getScriptSha(), new SingletonList<>(getKey(request)), getList());
    }
}
