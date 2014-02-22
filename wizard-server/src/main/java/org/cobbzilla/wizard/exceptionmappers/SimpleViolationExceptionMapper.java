package org.cobbzilla.wizard.exceptionmappers;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.SimpleViolationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

@Provider
public class SimpleViolationExceptionMapper implements ExceptionMapper<SimpleViolationException> {

    @Override
    public Response toResponse(SimpleViolationException e) {
        return Response.status(HttpStatusCodes.UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(getEntity(e))
                .build();
    }

    private List<ConstraintViolationBean> getEntity(SimpleViolationException e) {
        final List<ConstraintViolationBean> jsonList = new ArrayList<>(1);
        jsonList.add(new ConstraintViolationBean(e.getMessageTemplate(), e.getMessage(), e.getInvalidValue()));
        return jsonList;
    }

}
