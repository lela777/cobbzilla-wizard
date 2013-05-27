package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.form.dao.FormFieldMembershipDAO;
import org.springframework.beans.factory.annotation.Autowired;

public class FormFieldMembershipsResource {

    public static final String ENDPOINT = "/fieldMembers";

    @Autowired private FormFieldMembershipDAO formFieldMembershipDAO;


}
