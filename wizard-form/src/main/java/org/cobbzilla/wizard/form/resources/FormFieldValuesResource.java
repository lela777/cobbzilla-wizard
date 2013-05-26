package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormFieldValueDAO;
import org.cobbzilla.wizard.form.model.FormFieldValue;
import org.cobbzilla.wizard.resources.AbstractResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormFieldValuesResource.ENDPOINT)
@Service
public class FormFieldValuesResource extends AbstractResource<FormFieldValue> {

    @Autowired private FormFieldValueDAO formFieldValueDAO;
    @Override protected AbstractCRUDDAO<FormFieldValue> dao() { return formFieldValueDAO; }

    public static final String ENDPOINT = "formFieldValues";
    @Override protected String getEndpoint() { return ENDPOINT; }

}
