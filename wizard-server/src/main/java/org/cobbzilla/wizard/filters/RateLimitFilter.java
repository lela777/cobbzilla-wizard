package org.cobbzilla.wizard.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.stereotype.Service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

@Provider @Service @NoArgsConstructor
public abstract class RateLimitFilter implements ContainerRequestFilter {

    @Getter(lazy = true) private final RedisService buckets = initCache();
    protected abstract RedisService initCache();

    @Getter(lazy = true) private final String scriptSha = initScript();
    public String initScript() {
        return getBuckets().loadScript(StreamUtil.loadResourceAsStringOrDie("api/api_limiter_redis.lua"));
    }

    @Getter(lazy = true) private final List<String> list = initList();
    protected List<String> initList() {
        ArrayNode array = JsonUtil.fromJsonOrDie(StreamUtil.loadResourceAsStringOrDie("api/api_limits.json"),
                                                 ArrayNode.class);
        List<String> list = new ArrayList<>();
        for (JsonNode row : array) {
            list.add(row.get("limit").textValue());
            list.add(row.get("interval").textValue());
            list.add(row.get("block_dur").textValue());
        }
        return list;
    }

    protected abstract String getKey(ContainerRequest request);

    @Override public ContainerRequest filter(@Context ContainerRequest request) {
        Long i = checkOverflow(request);
        if (!ZillaRuntime.empty(i)) {
            //handleRejection(i); To be implemented
            throw new WebApplicationException(Response.status(429).build());
        }
        return request;
    }

    public Long checkOverflow(ContainerRequest request) {
        return (Long) getBuckets().eval(getScriptSha(), new SingletonList<>(getKey(request)), getList());
    }
}
