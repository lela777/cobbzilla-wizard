package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.stream.Collectors;

@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy=true) private final RedisService buckets = initCache();
    protected abstract RedisService initCache();

    @Getter(lazy=true) private final String scriptSha = initScript();
    protected abstract String initScript();


    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        final List<Long> fail = checkOverflow(request).stream().filter(x->x!=0).collect(Collectors.toList());
        if (!fail.isEmpty()) {
            throw new WebApplicationException(Response.status(429).entity(JsonUtil.toJsonOrDie(fail)).build());
        }
        return request;
    }

    public abstract List<Long> checkOverflow(ContainerRequest request);
}
