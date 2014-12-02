package org.cobbzilla.wizard.exceptionmappers;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.validation.MultiViolationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class MultiViolationExceptionMapper implements ExceptionMapper<MultiViolationException> {

    @Override
    public Response toResponse(MultiViolationException e) {
        return Response.status(HttpStatusCodes.UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(e.getViolations())
                .build();
    }

}
