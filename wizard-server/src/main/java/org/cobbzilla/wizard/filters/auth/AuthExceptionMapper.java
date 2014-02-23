package org.cobbzilla.wizard.filters.auth;

import org.cobbzilla.wizard.resources.ResourceUtil;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthExceptionMapper implements ExceptionMapper<AuthException> {

    @Override public Response toResponse(AuthException e) { return ResourceUtil.forbidden(); }

}
