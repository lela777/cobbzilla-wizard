package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractChildCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormFieldGroupMembershipDAO;
import org.cobbzilla.wizard.form.model.FormFieldGroup;
import org.cobbzilla.wizard.form.model.FormFieldGroupMembership;
import org.cobbzilla.wizard.resources.AbstractChildResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormFieldGroupMembershipsResource.ENDPOINT)
@Service
public class FormFieldGroupMembershipsResource extends AbstractChildResource<FormFieldGroupMembership, FormFieldGroup> {

    public static final String ENDPOINT = "formFieldGroupMemberships";

    @Autowired private FormFieldGroupMembershipDAO formFieldGroupMembershipDAO;
    @Override protected AbstractChildCRUDDAO<FormFieldGroupMembership, FormFieldGroup> dao() { return formFieldGroupMembershipDAO; }

}
