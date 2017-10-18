package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class EntityTypeHeaderFilter extends EntityTypeFilter {

    protected String getTypeHeaderName() { return Identifiable.ENTITY_TYPE_HEADER_NAME; }

    @Override public ContainerResponse filter(ContainerRequest request,
                                              ContainerResponse response,
                                              String responseClassName,
                                              Class<?> responseClass) {

        boolean isCollection = Collection.class.isAssignableFrom(responseClass);
        boolean isMap = Map.class.isAssignableFrom(responseClass);
        boolean isArray = responseClass.isArray();
        String elementClassName;
        if (isCollection) {
            final Collection c = (Collection) response.getEntity();
            try {
                elementClassName = empty(c) ? "" : c.iterator().next().getClass().getName();
            } catch (Exception e) {
                elementClassName = "";
            }
            response.getHttpHeaders().add(getTypeHeaderName(), elementClassName+"[]");

        } else if (isArray) {
            final Object[] a = (Object[]) response.getEntity();
            try {
                elementClassName = empty(a) ? "" : a[0].getClass().getName();
            } catch (Exception e) {
                elementClassName = "";
            }
            response.getHttpHeaders().add(getTypeHeaderName(), elementClassName + "[]");

        } else if (isMap) {
            response.getHttpHeaders().add(getTypeHeaderName(), LinkedHashMap.class.getName());
        } else {
            response.getHttpHeaders().add(getTypeHeaderName(), responseClassName);
        }
        return response;
    }

}