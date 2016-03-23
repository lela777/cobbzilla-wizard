package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.form.model.Form;
import org.cobbzilla.wizard.form.model.FormField;
import org.cobbzilla.wizard.form.model.FormFieldMembership;
import org.cobbzilla.wizard.form.model.FormFieldType;
import org.cobbzilla.wizard.form.resources.model.FormFieldMembershipRequest;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.http.HttpStatusCodes.CREATED;
import static org.cobbzilla.util.http.HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.assertEquals;

public class FormResourcesIT extends FormResourceITBase {

    public static final String DOC_TARGET = "form create/read/update/delete";

    @Test public void testCreateWithNoPayload () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "with no payload");
        final RestResponse response = doPost(FormApiEndpoints.FORMS_ENDPOINT, null);
        assertEquals(UNSUPPORTED_MEDIA_TYPE, response.status);
    }

    @Test public void testCreateWithEmptyPayload () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "with empty payload");
        final RestResponse response = doPost(FormApiEndpoints.FORMS_ENDPOINT, EMPTY_JSON);
        assertExpectedViolations(response, ERR_FORM_TYPE_EMPTY, ERR_FORM_NAME_EMPTY, ERR_FORM_DEFAULT_NAME_EMPTY);
    }

    @Test public void testFormCrud () throws Exception {
        final String owner = randomName(FORM_OWNER_MAXLEN);

        apiDocs.startRecording(DOC_TARGET, "full CRUD cycle");
        final Form form = randomForm(owner);

        apiDocs.addNote("create a random form");
        final RestResponse response = doPost(FormApiEndpoints.FORMS_ENDPOINT, toJson(form));
        assertEquals(CREATED, response.status);

        apiDocs.addNote("retrieve the form we just created");
        final Form created = fromJson(doGet(response.location).json, Form.class);
        assertEquals(form.getFormType(), created.getFormType());
        assertEquals(form.getNameKey(), created.getNameKey());
        assertEquals(form.getDefaultName(), created.getDefaultName());
        assertEquals(form.getOwner(), created.getOwner());

        apiDocs.addNote("update some the attributes");
        final Form update = randomForm(owner);
        assertEquals(HttpStatusCodes.NO_CONTENT, doPut(response.location, toJson(update)).status);

        apiDocs.addNote("re-read the form");
        final Form updated = fromJson(doGet(response.location).json, Form.class);
        assertEquals(update, updated);

        apiDocs.addNote("delete the form");
        final RestResponse deleteResponse = doDelete(response.location);
        assertEquals(HttpStatusCodes.NO_CONTENT, deleteResponse.status);

        apiDocs.addNote("now re-fetch the form, should not be found");
        final RestResponse notFound = doGet(response.location);
        assertEquals(HttpStatusCodes.NOT_FOUND, notFound.status);
    }

    @Test public void testAddRemoveFields () throws Exception {

        final String owner = randomName(FORM_OWNER_MAXLEN);

        apiDocs.startRecording(DOC_TARGET, "add/remove fields from a form");

        final Form form = randomForm(owner);

        apiDocs.addNote("create a new form");
        final RestResponse response = doPost(FormApiEndpoints.FORMS_ENDPOINT, toJson(form));
        assertEquals(CREATED, response.status);
        final String formLocation = response.location;
        final String membersPath = formLocation + FormApiEndpoints.FIELD_MEMBERS_ENDPOINT;
        final String availablePath = formLocation + FormApiEndpoints.AVAILABLE_FIELDS_ENDPOINT;

        apiDocs.addNote("verify there are no field members currently defined");
        FormFieldMembership[] fieldMemberships = fromJson(doGet(membersPath).json, FormFieldMembership[].class);
        assertEquals(0, fieldMemberships.length);

        apiDocs.addNote("verify there are no field members currently available to assign to forms");
        FormField[] available = fromJson(doGet(availablePath).json, FormField[].class);
        assertEquals(0, available.length);

        final int numFields = 6;
        final int numInitialFields = 2;
        final int numExtraFields = 2;

        apiDocs.addNote("create "+numFields+" fields");
        final Map<String, FormField> initialFields = new HashMap<>();
        final Map<String, FormField> extraFields = new HashMap<>();

        for (int i=0; i< numFields; i++) {
            final FormField field = randomField();
            apiDocs.addNote("create field "+i+" of "+numFields);
            final RestResponse fieldResponse = doPost(FormApiEndpoints.FIELDS_ENDPOINT, toJson(field));
            assertEquals(HttpStatusCodes.CREATED, fieldResponse.status);
            if (i < numInitialFields) initialFields.put(fieldResponse.location, field);
            else if (i < numInitialFields+numExtraFields) extraFields.put(fieldResponse.location, field);
        }

        apiDocs.addNote("verify there are now "+numFields+" field members currently available to assign to forms");
        available = fromJson(doGet(availablePath).json, FormField[].class);
        assertEquals(numFields, available.length);

        final List<String> fieldMembershipLocations = new ArrayList<>();
        apiDocs.addNote("associate "+numInitialFields+" of the fields with the new form");
        for (String fieldLocation : initialFields.keySet()) {
            fieldMembershipLocations.add(addFormFieldMember(membersPath, fieldLocation));
        }

        apiDocs.addNote("verify that "+numInitialFields+" fields are now associated with the form");
        fieldMemberships = fromJson(doGet(membersPath).json, FormFieldMembership[].class);
        assertEquals(numInitialFields, fieldMemberships.length);

        apiDocs.addNote("verify there are now "+(numFields-numInitialFields)+" field members currently available to assign to forms");
        available = fromJson(doGet(availablePath).json, FormField[].class);
        assertEquals(numFields-numInitialFields, available.length);

        apiDocs.addNote("associated another "+numExtraFields+" fields with the form");
        for (String fieldLocation : extraFields.keySet()) {
            fieldMembershipLocations.add(addFormFieldMember(membersPath, fieldLocation));
        }

        apiDocs.addNote("verify that "+(numInitialFields+numExtraFields)+" fields are now associated with the form");
        fieldMemberships = fromJson(doGet(membersPath).json, FormFieldMembership[].class);
        assertEquals(numInitialFields+numExtraFields, fieldMemberships.length);

        apiDocs.addNote("edit one field membership and add a 'placement' attribute");
        final String firstMembershipLocation = fieldMembershipLocations.get(0);
        final String placement = randomName();
        FormFieldMembershipRequest updateRequest = new FormFieldMembershipRequest();
        updateRequest.setPlacement(placement);
        assertEquals(HttpStatusCodes.OK, doPut(firstMembershipLocation, toJson(updateRequest)).status);

        apiDocs.addNote("re-read the field membership, verify the placement has been updated");
        FormFieldMembership updated = fromJson(doGet(firstMembershipLocation).json, FormFieldMembership.class);
        assertEquals(placement, updated.getPlacement());

        apiDocs.addNote("remove all but one of the field memberships from the form");
        final String spared = StringUtil.lastPathElement(fieldMembershipLocations.remove(0));
        for (String membershipLocation : fieldMembershipLocations) {
            assertEquals(HttpStatusCodes.OK, doDelete(membershipLocation).status);
        }

        apiDocs.addNote("verify that only one field is now associated with the form");
        fieldMemberships = fromJson(doGet(membersPath).json, FormFieldMembership[].class);
        assertEquals(1, fieldMemberships.length);
        assertEquals(spared, fieldMemberships[0].getUuid());

        apiDocs.addNote("verify there are now "+(numFields-1)+" field members currently available to assign to forms");
        available = fromJson(doGet(availablePath).json, FormField[].class);
        assertEquals(numFields-1, available.length);
    }

    private String addFormFieldMember(String membersPath, String fieldLocation) throws Exception {
        final String fieldUuid = StringUtil.lastPathElement(fieldLocation);
        final FormFieldMembershipRequest request = new FormFieldMembershipRequest();
        request.setFieldUuid(fieldUuid);
        final RestResponse response = doPost(membersPath, toJson(request));
        assertEquals(HttpStatusCodes.CREATED, response.status);
        return response.location;
    }

    public Form randomForm(String owner) { return randomForm(owner, null); }

    public Form randomForm(String owner, Integer length) {
        final Form form = new Form();
        form.setFormType(randomName(length == null ? FORM_TYPE_MAXLEN : length));
        form.setNameKey(randomName(length == null ? FORM_NAME_MAXLEN : length));
        form.setDefaultName(randomName(length == null ? FORM_DEFAULT_NAME_MAXLEN : length));
        form.setOwner(owner);
        return form;
    }

    public FormField randomField() { return randomField(null); }

    public FormField randomField(Integer length) {
        final FormField field = new FormField();
        field.setNameKey(randomName(length == null ? FIELD_NAME_MAXLEN : length));
        field.setDefaultName(randomName(length == null ? FIELD_DEFAULT_NAME_MAXLEN : length));
        field.setFieldType(FormFieldType.TEXT.name());
        field.setOwner(randomName(length == null ? FORM_OWNER_MAXLEN : length));
        return field;
    }

}
