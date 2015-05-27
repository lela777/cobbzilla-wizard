package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.api.ForbiddenException;
import org.cobbzilla.wizard.api.NotFoundException;
import org.cobbzilla.wizard.api.ValidationException;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ValidationMessages;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.cobbzilla.util.http.HttpStatusCodes.UNPROCESSABLE_ENTITY;

public class ResourceUtil {

    public static Response notFound() { return notFound(null); }

    public static Response notFound(String id) {
        if (id == null) id = "-unknown-";
        return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("resource", id)).build();
    }

    public static Response notFound_blank() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response forbidden() { return Response.status(Response.Status.FORBIDDEN).build(); }

    public static Response invalid() { return Response.status(UNPROCESSABLE_ENTITY).build(); }

    public static Response invalid(List<ConstraintViolationBean> violations) {
        return Response.status(UNPROCESSABLE_ENTITY).entity(violations).build();
    }

    public static Response invalid(ConstraintViolationBean violation) {
        List<ConstraintViolationBean> violations = new ArrayList<>();
        violations.add(violation);
        return invalid(violations);
    }

    public static Response invalid(String messageTemplate) {
        return invalid(messageTemplate, null);
    }

    public static Response invalid(String messageTemplate, String invalidValue) {
        List<ConstraintViolationBean> violations = new ArrayList<>();
        violations.add(new ConstraintViolationBean(messageTemplate, ValidationMessages.translateMessage(messageTemplate), invalidValue));
        return invalid(violations);
    }

    public static <T> T userPrincipal(HttpContext context) {
        try {
            return (T) context.getRequest().getUserPrincipal();
        } catch (UnsupportedOperationException e) {
            // expected
            return null;
        }
    }

    public static Response toResponse (ApiException e) {
        if (e instanceof NotFoundException) {
            return notFound(e.getResponse().json);
        } else if (e instanceof ForbiddenException) {
            return forbidden();
        } else if (e instanceof ValidationException) {
            return invalid(new ArrayList<>(((ValidationException) e).getViolations().values()));
        }
        return Response.serverError().build();
    }

    public static Response streamFile(final File f) {
        if (f == null) return notFound();
        if (!f.exists()) return notFound(f.getName());
        if (!f.canRead()) return forbidden();

        return Response.ok(new StreamingOutput() {
            @Override public void write(OutputStream out) throws IOException, WebApplicationException {
                try (InputStream in = new FileInputStream(f)) {
                    StreamUtil.copyLarge(in, out);
                }
            }
        })
                .header(HttpHeaders.CONTENT_TYPE, URLConnection.guessContentTypeFromName(f.getName()))
                .header(HttpHeaders.CONTENT_LENGTH, f.length())
                .build();
    }
}
