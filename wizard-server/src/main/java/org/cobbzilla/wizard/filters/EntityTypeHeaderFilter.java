package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.Identifiable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

@Slf4j
public class EntityTypeHeaderFilter implements ContainerResponseFilter {

    protected String getTypeHeaderName() { return Identifiable.ENTITY_TYPE_HEADER_NAME; }

    @Override public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {

        final Type entityType = containerResponse.getEntityType();
        if (entityType == null) return containerResponse;

        final Class<?> responseClass;
        final String responseClassName;
        try {
            responseClassName = entityType.toString().split(" ")[1];
            responseClass = forName(responseClassName);
        } catch (Exception e) {
            log.warn("filter: error with '"+entityType+"': "+e);
            return containerResponse;
        }

        boolean isCollection = Collection.class.isAssignableFrom(responseClass);
        boolean isMap = Map.class.isAssignableFrom(responseClass);
        boolean isArray = responseClass.isArray();
        String elementClassName;
        if (isCollection) {
            final Collection c = (Collection) containerResponse.getEntity();
            try {
                elementClassName = empty(c) ? "" : c.iterator().next().getClass().getName();
            } catch (Exception e) {
                elementClassName = "";
            }
            containerResponse.getHttpHeaders().add(getTypeHeaderName(), elementClassName+"[]");

        } else if (isArray) {
            final Object[] a = (Object[]) containerResponse.getEntity();
            try {
                elementClassName = empty(a) ? "" : a[0].getClass().getName();
            } catch (Exception e) {
                elementClassName = "";
            }
            containerResponse.getHttpHeaders().add(getTypeHeaderName(), elementClassName + "[]");

        } else if (isMap) {
            containerResponse.getHttpHeaders().add(getTypeHeaderName(), LinkedHashMap.class.getName());
        } else {
            containerResponse.getHttpHeaders().add(getTypeHeaderName(), responseClassName);
        }
        return containerResponse;
    }

}