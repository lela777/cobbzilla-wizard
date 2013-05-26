package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormFieldGroupDAO;
import org.cobbzilla.wizard.form.model.FormFieldGroup;
import org.cobbzilla.wizard.resources.AbstractResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormFieldGroupsResource.ENDPOINT)
@Service
public class FormFieldGroupsResource extends AbstractResource<FormFieldGroup> {

    @Autowired private FormFieldGroupDAO formFieldGroupDAO;
    @Override protected AbstractCRUDDAO<FormFieldGroup> dao() { return formFieldGroupDAO; }

    public static final String ENDPOINT = "formFieldGroups";
    @Override protected String getEndpoint() { return ENDPOINT; }

    @Autowired private FormFieldGroupMembershipsResource formFieldGroupMembershipsResource;
    @Path(FormFieldGroupMembershipsResource.ENDPOINT)
    public FormFieldGroupMembershipsResource formFieldGroupMembershipsResource () { return formFieldGroupMembershipsResource; }

}
