package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.form.model.Form;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.Test;

import static org.cobbzilla.util.http.HttpStatusCodes.CREATED;
import static org.cobbzilla.util.http.HttpStatusCodes.UNSUPPORTED_MEDIA_TYPE;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.assertEquals;

public class FormResourcesIT extends FormResourceITBase {

    public static final String DOC_TARGET = "form create/read/update/delete";

    @Test
    public void testCreateWithNoPayload () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "with no payload");
        final RestResponse response = doPost(FormsResource.ENDPOINT, null);
        assertEquals(UNSUPPORTED_MEDIA_TYPE, response.status);
    }

    @Test
    public void testCreateWithEmptyPayload () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "with empty payload");
        final RestResponse response = doPost(FormsResource.ENDPOINT, EMPTY_JSON);
        assertExpectedViolations(response, new String[] {
                ERR_FORM_TYPE_EMPTY, ERR_FORM_NAME_EMPTY
        });
    }

    @Test
    public void testFormCrud () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "full CRUD cycle");
        final Form form = randomForm();

        apiDocs.addNote("create a random form");
        final RestResponse response = doPost(FormsResource.ENDPOINT, toJson(form));
        assertEquals(CREATED, response.status);

        apiDocs.addNote("retrieve the form we just created");
        final Form created = fromJson(doGet(response.location).json, Form.class);
        assertEquals(form.getFormType(), created.getFormType());
        assertEquals(form.getNameKey(), created.getNameKey());
        assertEquals(form.getOwner(), created.getOwner());

        apiDocs.addNote("update some the attributes");
        final Form update = randomForm();
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

    public Form randomForm() { return randomForm(null); }

    public Form randomForm(Integer length) {
        final Form form = new Form();
        form.setFormType(randomName(length == null ? FORM_TYPE_MAXLEN : length));
        form.setNameKey(randomName(length == null ? FORM_NAME_MAXLEN : length));
        form.setOwner(randomName(length == null ? FORM_OWNER_MAXLEN : length));
        return form;
    }

}
