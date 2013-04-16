package org.cobbzilla.wizard.exceptionmappers;

import org.postgresql.util.PSQLException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PSQLExceptionMapper implements ExceptionMapper<PSQLException> {

    @Override
    public Response toResponse(PSQLException exception) {
        return Response.serverError().build();
    }
}
