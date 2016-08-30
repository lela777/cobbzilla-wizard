package org.cobbzilla.wizard.exceptionmappers;

import org.cobbzilla.wizard.server.RestServerBase;
import org.postgresql.util.PSQLException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PSQLExceptionMapper implements ExceptionMapper<PSQLException> {

    @Override public Response toResponse(PSQLException exception) {
        RestServerBase.report(exception);
        return Response.serverError().build();
    }
}
