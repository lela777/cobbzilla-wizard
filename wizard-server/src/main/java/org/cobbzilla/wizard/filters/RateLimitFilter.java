package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy=true) private final RedisService buckets = initCache();
    protected abstract RedisService initCache();

    protected List<String> getList(long timestamp) {
        List<String> list = new ArrayList<>();
        list.add(String.valueOf(10000));
        list.add(String.valueOf(TimeUtil.HOUR));
        list.add(String.valueOf(timestamp));
        return list;
    }


    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        List<Pair<Integer, Long>> map = new ArrayList<>();
        String key = null;
        final List<String> keys = getKeys(request);
        for (String k : keys) {
            if (!empty(k)) {
                key = k;
                break;
            }
        }
        List<Long> result = getBuckets().checkOverflow(key, getList(now()));
        List<Long> fail = result.stream().filter(x->x!=0).collect(Collectors.toList());
        if (!fail.isEmpty()) {
            throw new WebApplicationException(Response.status(429).entity(JsonUtil.toJsonOrDie(fail)).build());
        }
        return request;
    }

    protected abstract List<String> getKeys(ContainerRequest request);
}
