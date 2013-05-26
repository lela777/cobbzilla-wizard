package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormDAO;
import org.cobbzilla.wizard.form.model.Form;

import org.cobbzilla.wizard.resources.AbstractResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormsResource.ENDPOINT)
@Service
public class FormsResource extends AbstractResource<Form> {

    public static final String ENDPOINT = "/forms";
    @Override protected String getEndpoint() { return ENDPOINT; }

    @Autowired private FormDAO formDAO;
    @Override protected AbstractCRUDDAO<Form> dao() { return formDAO; }

//    @Autowired private FormFieldGroupsResource formFieldGroupsResource;
//    @Path(FormFieldGroupsResource.ENDPOINT)
//    public FormFieldGroupsResource formFieldGroupsResource () { return formFieldGroupsResource; }

}
