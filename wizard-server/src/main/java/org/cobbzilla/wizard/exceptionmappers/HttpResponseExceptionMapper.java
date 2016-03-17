package org.cobbzilla.wizard.exceptionmappers;

import org.cobbzilla.wizard.resources.ResourceHttpException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static org.cobbzilla.wizard.resources.ResourceUtil.status;

@Provider
public class HttpResponseExceptionMapper
        extends AbstractConstraintViolationExceptionMapper<ResourceHttpException>
        implements ExceptionMapper<ResourceHttpException> {

    @Override public Response toResponse(ResourceHttpException e) { return status(e.getStatus(), e.getEntity()); }

}
