package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.http.HttpContentTypes;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.api.ForbiddenException;
import org.cobbzilla.wizard.api.NotFoundException;
import org.cobbzilla.wizard.api.ValidationException;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.util.StreamStreamingOutput;
import org.cobbzilla.wizard.validation.*;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static javax.ws.rs.core.HttpHeaders.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.HttpContentTypes.APPLICATION_JSON;
import static org.cobbzilla.util.http.HttpContentTypes.fileExt;
import static org.cobbzilla.util.http.HttpStatusCodes.*;

@Slf4j
public class ResourceUtil {

    public static Response ok() { return Response.ok().build(); }

    public static Response ok(Object o) { return Response.ok(o).build(); }

    public static Response ok_empty() { return Response.ok(Collections.emptyMap()).build(); }

    public static Response send(SendableResource resource) {
        return send(resource.getOut(), resource.getName(), resource.getContentType(), resource.getContentLength(), resource.getForceDownload());
    }

    public static Response send(StreamingOutput out, String name, String contentType, Long contentLength, Boolean forceDownload) {
        Response.ResponseBuilder builder = Response.ok(out).header(CONTENT_TYPE, contentType);
        if (name != null) {
            final String ext = fileExt(contentType);
            if (!name.endsWith(ext)) name += ext;
            if (forceDownload == null || !forceDownload) {
                builder = builder.header("Content-Disposition", "inline; filename=\"" + name + "\"");
            } else {
                builder = builder.header("Content-Disposition", "attachment; filename=\"" + name + "\"");
            }
        } else if (forceDownload != null && forceDownload) {
            return die("send: forceDownload was true but no filename was set");
        }
        if (contentLength != null) builder = builder.header(CONTENT_LENGTH, contentLength);
        return builder.build();
    }

    public static Response accepted() { return Response.status(ACCEPTED).build(); }

    public static Response serverError() { return Response.serverError().build(); }

    public static Response notFound() { return notFound(null); }

    public static Response notFound(String id) {
        if (id == null) id = "-unknown-";
        return status(Response.Status.NOT_FOUND, Collections.singletonMap("resource", id));
    }

    public static Response notFound_blank() { return status(NOT_FOUND); }

    public static EntityNotFoundException notFoundEx() { return notFoundEx(null); }

    public static EntityNotFoundException notFoundEx(String id) {
        if (id == null) id = "-unknown-";
        final EntityNotFoundException e = new EntityNotFoundException(id);
        log.info("Object not found", e);
        return e;
    }

    public static Response status (Response.Status status) { return status(status.getStatusCode()); }
    public static Response status (int status) { return Response.status(status).build(); }
    public static Response status (Response.Status status, Object entity) {
        return entity != null
                ? status(status.getStatusCode(), entity)
                : status(status.getStatusCode());
    }
    public static Response status (int status, Object entity) {
        return entity != null
                ? Response.status(status).type(APPLICATION_JSON).entity(entity).build()
                : status(status);
    }

    public static Response redirect (String location) { return redirect(FOUND, location); }
    public static Response redirect (int status, String location) {
        return Response.status(status).header(LOCATION, location).build();
    }

    public static Response forbidden() { return status(FORBIDDEN); }
    public static ResourceHttpException forbiddenEx() { return new ResourceHttpException(FORBIDDEN); }

    public static Response invalid() { return status(UNPROCESSABLE_ENTITY); }
    public static Response invalid(List<ConstraintViolationBean> violations) { return status(UNPROCESSABLE_ENTITY, violations); }

    public static Response invalid(ConstraintViolationBean violation) {
        List<ConstraintViolationBean> violations = new ArrayList<>();
        violations.add(violation);
        return invalid(violations);
    }

    public static Response invalid(String messageTemplate) { return invalid(messageTemplate, null); }

    public static Response invalid(String messageTemplate, String invalidValue) {
        return invalid(messageTemplate, ValidationMessages.translateMessage(messageTemplate), invalidValue);
    }

    public static Response invalid(String messageTemplate, String message, String invalidValue) {
        List<ConstraintViolationBean> violations = new ArrayList<>();
        violations.add(new ConstraintViolationBean(messageTemplate, message, invalidValue));
        return invalid(violations);
    }

    public static Response invalid(ValidationResult result) { return invalid(result.getViolationBeans()); }

    public static SimpleViolationException invalidEx(String messageTemplate) { return invalidEx(messageTemplate, null, null); }
    public static SimpleViolationException invalidEx(String messageTemplate, String message) { return invalidEx(messageTemplate, message, null); }
    public static SimpleViolationException invalidEx(String messageTemplate, String message, String invalidValue) {
        return invalidEx(messageTemplate, message, invalidValue, true);
    }
    public static SimpleViolationException invalidEx(String messageTemplate, String message, String invalidValue,
                                                     boolean logException) {
        final SimpleViolationException ex = new SimpleViolationException(messageTemplate, message, invalidValue);
        if (logException) log.warn("invalidEx: "+ex, ex);
        return ex;
    }

    public static MultiViolationException invalidEx(ValidationResult result) {
        return new MultiViolationException(result.getViolationBeans());
    }

    public static Response timeout () { return status(GATEWAY_TIMEOUT); }
    public static ResourceHttpException timeoutEx () { return new ResourceHttpException(GATEWAY_TIMEOUT); }

    public static Response unavailable() { return status(SERVER_UNAVAILABLE); }
    public static ResourceHttpException unavailableEx() { return new ResourceHttpException(SERVER_UNAVAILABLE); }

    public static <T> T userPrincipal(HttpContext context) { return userPrincipal(context, true); }

    public static <T> T optionalUserPrincipal(HttpContext context) {
        return userPrincipal(context, false);
    }

    public static <T> T userPrincipal(HttpContext context, boolean required) {
        T user;
        try {
            user = (T) context.getRequest().getUserPrincipal();
        } catch (UnsupportedOperationException e) {
            log.debug("userPrincipal: "+e);
            user = null;
        }
        if (required && user == null) throw forbiddenEx();
        return user;
    }

    public static Response toResponse (ApiException e) {
        if (e instanceof NotFoundException) {
            return notFound(e.getResponse().json);
        } else if (e instanceof ForbiddenException) {
            return forbidden();
        } else if (e instanceof ValidationException) {
            return invalid(new ArrayList<>(((ValidationException) e).getViolations().values()));
        }
        return serverError();
    }

    public static Response toResponse (RestResponse response) {
        Response.ResponseBuilder builder = Response.status(response.status);
        if (response.status/100 == 3) {
            builder = builder.header(LOCATION, response.location);
        }
        if (!empty(response.json)) {
            builder = builder.entity(response.json)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .header(CONTENT_LENGTH, response.json.length());
        }
        return builder.build();
    }

    public static Response toResponse (final HttpResponseBean response) {
        Response.ResponseBuilder builder = Response.status(response.getStatus());

        final List<NameAndValue> headers = response.getHeaders();
        for (NameAndValue header : headers) {
            builder = builder.header(header.getName(), header.getValue());
        }

        if (response.hasEntity()) {
            builder = builder.entity(new ByteStreamingOutput(response.getEntity()));
        }

        return builder.build();
    }

    public static Response streamFile(final File f) {
        if (f == null) return notFound();
        if (!f.exists()) return notFound(f.getName());
        if (!f.canRead()) return forbidden();

        return Response.ok(new FileStreamingOutput(f))
                .header(CONTENT_TYPE, URLConnection.guessContentTypeFromName(f.getName()))
                .header(CONTENT_LENGTH, f.length())
                .build();
    }

    public static Response stream(String contentType, InputStream s) {
        if (contentType == null) contentType = HttpContentTypes.UNKNOWN;
        return Response.ok(new StreamStreamingOutput(s))
                .header(CONTENT_TYPE, contentType)
                .build();
    }

}
