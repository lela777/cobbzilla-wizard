package org.cobbzilla.wizard.resources;

import lombok.AccessLevel;
import lombok.Getter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.util.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public abstract class AbstractCachedSubResource<R extends AbstractCachedSubResource> {

    @Getter(AccessLevel.PROTECTED) private final AtomicReference<Map<String, R>> cacheRef = new AtomicReference<>(getCacheMap());
    protected abstract Map<String, R> getCacheMap();

    @Getter(lazy=true) private final R subResourceProto = initSubResourceProto();
    private R initSubResourceProto() { return (R) instantiate(getClass()); }

    public R forContext(ApplicationContext ctx, Object... args) {
        final StringBuilder cacheKey = new StringBuilder();
        for (Object o : args) {
            if (o instanceof Identifiable) {
                cacheKey.append(((Identifiable) o).getUuid()).append(":");
            }
        }
        synchronized (cacheRef) {
            final Map<String, R> resourceCache = (Map<String, R>) getSubResourceProto().getCacheRef().get();
            R r = resourceCache.get(cacheKey.toString());
            if (r == null) {
                r = (R) instantiate(getSubResourceProto().getClass(), args);
                SpringUtil.autowire(ctx, r);
                resourceCache.put(cacheKey.toString(), r);
            }
            return r;
        }
    }

}
