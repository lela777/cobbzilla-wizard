package org.cobbzilla.wizard.exceptionmappers;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ValidationMessages;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractConstraintViolationExceptionMapper<E extends Exception> {

    protected Response buildResponse(E e) {
        return Response.status(HttpStatusCodes.UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(exception2json(e))
                .build();
    }

    protected List<ConstraintViolationBean> exception2json(E e) {
        return Collections.singletonList(mapGenericExceptionToConstraintViolationBean(e));
    }

    protected ConstraintViolationBean mapGenericExceptionToConstraintViolationBean(E e) {
        return new ConstraintViolationBean(scrubMessage(e.getMessage()), ValidationMessages.translateMessage(e.getMessage()), getInvalidValue(e));
    }

    protected List<ConstraintViolationBean> getConstraintViolationBeans(List<ConstraintViolation> violations) {
        final List<ConstraintViolationBean> jsonList = new ArrayList<>(violations.size());
        for (ConstraintViolation violation : violations) {
            jsonList.add(new ConstraintViolationBean(violation));
        }
        return jsonList;
    }

    protected String scrubMessage(String messageTemplate) {
        return messageTemplate.replace("'", "").replace("{", "").replace("}", "");
    }

    protected String getInvalidValue(Exception e) { return null; }

}
