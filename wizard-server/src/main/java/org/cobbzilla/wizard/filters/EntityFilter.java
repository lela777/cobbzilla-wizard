package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;

import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

@Slf4j
public abstract class EntityFilter<T> implements ContainerResponseFilter {

    @Getter(lazy=true) private final Class<T> matchEntityClass = initMatchEntityClass();
    private Class<T> initMatchEntityClass() { return getFirstTypeParam(getClass()); }

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

    protected boolean shouldFilter(ContainerRequest request, ContainerResponse response, String responseClassName, Class<?> responseClass) {
        return getMatchEntityClass().isAssignableFrom(responseClass);
    }

    protected ContainerResponse filter(ContainerRequest request, ContainerResponse response, String responseClassName, Class<?> responseClass) {
        return response;
    }

}