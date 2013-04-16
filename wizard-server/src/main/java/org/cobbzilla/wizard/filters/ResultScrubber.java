package org.cobbzilla.wizard.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;

@Slf4j
public abstract class ResultScrubber implements ContainerResponseFilter {

    protected abstract ScrubbableField[] getFieldsToScrub(Object entity);

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        final Object entity = response.getEntity();
        for (ScrubbableField field : getFieldsToScrub(entity)) {
            if (entity != null && field.targetType.isAssignableFrom(entity.getClass())) {
                try {
                    ReflectionUtil.setNull(entity, field.name, field.type);
                } catch (Exception e) {
                    log.warn("filter: Error calling ReflectionUtil.setNull("+entity+", "+field.name+", "+field.type.getName()+"): "+e);
                }
            }
        }
        return response;
    }

}
