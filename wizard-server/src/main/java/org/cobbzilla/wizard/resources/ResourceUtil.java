package org.cobbzilla.wizard.resources;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.exceptionmappers.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ValidationMessages;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourceUtil {

    public static Response notFound(String id) {
        if (id == null) id = "-unknown-";
        return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("resource", id)).build();
    }

    public static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    public static Response invalid() {
        return Response.status(HttpStatusCodes.UNPROCESSABLE_ENTITY).build();
    }

    public static Response invalid(List<ConstraintViolationBean> violations) {
        return Response.status(HttpStatusCodes.UNPROCESSABLE_ENTITY).entity(violations).build();
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
}
