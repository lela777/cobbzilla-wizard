package org.cobbzilla.wizard.resources;

import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.api.ForbiddenException;
import org.cobbzilla.wizard.api.NotFoundException;
import org.cobbzilla.wizard.api.ValidationException;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ValidationMessages;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.http.HttpStatusCodes.UNPROCESSABLE_ENTITY;

public class ResourceUtil {

    public static Response notFound() { return notFound(null); }

    public static Response notFound(String id) {
        if (id == null) id = "-unknown-";
        return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("resource", id)).build();
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
        List<ConstraintViolationBean> violations = new ArrayList<>();
        violations.add(new ConstraintViolationBean(messageTemplate, ValidationMessages.translateMessage(messageTemplate), null));
        return invalid(violations);
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
}
