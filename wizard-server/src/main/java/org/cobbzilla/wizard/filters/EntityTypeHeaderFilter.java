package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class EntityTypeHeaderFilter extends EntityFilter<Object> {

    protected String getTypeHeaderName() { return Identifiable.ENTITY_TYPE_HEADER_NAME; }

    @Override public ContainerResponse filter(ContainerRequest request,
                                              ContainerResponse response,
                                              String responseClassName,
                                              Class<?> responseClass) {

        final boolean isSearchResults = SearchResults.class.isAssignableFrom(responseClass);
        final boolean isCollection = Collection.class.isAssignableFrom(responseClass);
        final boolean isMap = Map.class.isAssignableFrom(responseClass);
        final boolean isArray = responseClass.isArray();
        String elementClassName;
        if (isSearchResults) {
            final SearchResults searchResults = (SearchResults) response.getEntity();
            response.getHttpHeaders().add(getTypeHeaderName(),
                                       SearchResults.class.getName() + (searchResults.hasResults() ? "<" + getCollectionElementClass(searchResults.getResults()) + ">" : ""));

        } else if (isCollection) {
            response.getHttpHeaders().add(getTypeHeaderName(), getCollectionElementClass((Collection) response.getEntity())+"[]");

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

    protected String getCollectionElementClass(Collection c) {
        try {
            return empty(c) ? "" : c.iterator().next().getClass().getName();
        } catch (Exception ignored) {
            return "";
        }
    }

}