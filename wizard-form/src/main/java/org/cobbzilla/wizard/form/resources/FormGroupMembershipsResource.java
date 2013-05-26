package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractChildCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormGroupMembershipDAO;
import org.cobbzilla.wizard.form.model.Form;
import org.cobbzilla.wizard.form.model.FormGroupMembership;
import org.cobbzilla.wizard.resources.AbstractChildResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Path;

@Path(FormGroupMembershipsResource.ENDPOINT)
@Service
public class FormGroupMembershipsResource extends AbstractChildResource<FormGroupMembership, Form> {

    public static final String ENDPOINT = "formGroupMemberships";

    @Autowired private FormGroupMembershipDAO formGroupMembershipDAO;
    @Override protected AbstractChildCRUDDAO<FormGroupMembership, Form> dao() { return formGroupMembershipDAO; }

}
