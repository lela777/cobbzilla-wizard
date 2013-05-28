package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.resources.AbstractResource;

public class FormApiEndpoints {

    public static final String FORMS_ENDPOINT = "/forms";
    public static final String FIELDS_ENDPOINT = "/formFields";
    public static final String FIELD_MEMBERS_ENDPOINT = "/fieldMembers";
    public static final String FORM_MEMBERSHIPS_ENDPOINT = FORMS_ENDPOINT + "/" + AbstractResource.UUID + FIELD_MEMBERS_ENDPOINT;

    public static final String MEMBERSHIPS_ENDPOINT = "/" + AbstractResource.UUID + FIELD_MEMBERS_ENDPOINT;
    public static final String MEMBERSHIP_UUID_PARAM = "membershipUuid";
    public static final String MEMBERSHIP_UUID = "{" + MEMBERSHIP_UUID_PARAM + "}";
    public static final String AVAILABLE_FIELDS_ENDPOINT = "/availableFields";

}
