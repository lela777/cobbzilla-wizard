package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormFieldDAO;
import org.cobbzilla.wizard.form.model.FormField;
import org.cobbzilla.wizard.resources.AbstractResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormApiEndpoints.FIELDS_ENDPOINT)
@Service
public class FormFieldsResource extends AbstractResource<FormField> {

    @Autowired private FormFieldDAO formFieldDAO;
    @Override protected AbstractCRUDDAO<FormField> dao() { return formFieldDAO; }

    @Override protected String getEndpoint() { return FormApiEndpoints.FIELDS_ENDPOINT; }

}
