package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.ArrayUtil;

import java.lang.reflect.Type;

import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

@Slf4j
public abstract class EntityTypeFilter implements ContainerResponseFilter {

    @Override public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {

        final Type entityType = response.getEntityType();
        if (entityType == null) return response;

        final Class<?> responseClass;
        final String responseClassName;
        try {
            responseClassName = entityType.toString().split(" ")[1];
            responseClass = forName(responseClassName);
        } catch (Exception e) {
            log.warn("filter: error with '" + entityType + "': " + e);
            return response;
        }

        return shouldFilter(request, response, responseClassName, responseClass)
                ? filter(request, response, responseClassName, responseClass)
                : response;
    }

    protected Class<?>[] filterTypes () { return (Class<?>[]) ArrayUtil.EMPTY_OBJECT_ARRAY; }

    protected boolean shouldFilter(ContainerRequest request, ContainerResponse response, String responseClassName, Class<?> responseClass) {
        for (Class<?> c : filterTypes()) if (c.isAssignableFrom(responseClass)) return true;
        return false;
    }

    protected abstract ContainerResponse filter(ContainerRequest request,
                                                ContainerResponse response,
                                                String responseClassName,
                                                Class<?> responseClass);

}