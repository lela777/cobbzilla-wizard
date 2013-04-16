package org.cobbzilla.wizard.exceptionmappers;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public abstract class AbstractConstraintViolationExceptionMapper<E extends Exception> {

    public static final String VALIDATION_MESSAGES = "ValidationMessages";

    public static final int UNPROCESSABLE_ENTITY = 422;

    protected Response buildResponse(E e) {
        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(exception2json(e))
                .build();
    }

    protected List<ConstraintViolationBean> exception2json(E e) {
        return Collections.singletonList(mapGenericExceptionToConstraintViolationBean(e));
    }

    protected ConstraintViolationBean mapGenericExceptionToConstraintViolationBean(E e) {
        return new ConstraintViolationBean(scrubMessage(e.getMessage()), translateMessage(e.getMessage()), getInvalidValue(e));
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

    protected String translateMessage(String messageTemplate) {

        // strip leading/trailing curlies if they are there
        if (messageTemplate.startsWith("{")) messageTemplate = messageTemplate.substring(1);
        if (messageTemplate.endsWith("}")) messageTemplate = messageTemplate.substring(messageTemplate.length()-1);

        return ResourceBundle.getBundle(VALIDATION_MESSAGES).getString(messageTemplate);
    }

}
