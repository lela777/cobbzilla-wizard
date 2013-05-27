package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.dao.FormDAO;
import org.cobbzilla.wizard.form.dao.FormFieldDAO;
import org.cobbzilla.wizard.form.dao.FormFieldMembershipDAO;
import org.cobbzilla.wizard.form.model.Form;
import org.cobbzilla.wizard.form.model.FormField;
import org.cobbzilla.wizard.form.model.FormFieldMembership;
import org.cobbzilla.wizard.form.resources.model.FormFieldMembershipRequest;
import org.cobbzilla.wizard.resources.AbstractResource;
import org.cobbzilla.wizard.resources.ResourceUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Path(FormsResource.ENDPOINT)
@Service
public class FormsResource extends AbstractResource<Form> {

    public static final String ENDPOINT = "/forms";
    public static final String MEMBERSHIPS_ENDPOINT = "/" + UUID + FormFieldMembershipsResource.ENDPOINT;

    public static final String MEMBERSHIP_UUID_PARAM = "membershipUuid";
    public static final String MEMBERSHIP_UUID = "{" + MEMBERSHIP_UUID_PARAM + "}";
    public static final String AVAILABLE_FIELDS_ENDPOINT = "/availableFields";

    @Override protected String getEndpoint() { return ENDPOINT; }

    @Autowired private FormDAO formDAO;
    @Autowired private FormFieldDAO formFieldDAO;
    @Autowired private FormFieldMembershipDAO formFieldMembershipDAO;

    @Override protected AbstractCRUDDAO<Form> dao() { return formDAO; }

    @GET @Path(MEMBERSHIPS_ENDPOINT)
    public Response getFieldMemberships (@PathParam(UUID_PARAM) String formUuid) {
        return Response.ok(formFieldMembershipDAO.findByFormUuid(formUuid)).build();
    }

    @GET @Path("/"+UUID+AVAILABLE_FIELDS_ENDPOINT)
    public Response getAvailableFieldMemberships (@PathParam(UUID_PARAM) String formUuid) {
        List<FormField> available = formFieldDAO.findAll();
        Form form = formDAO.findByUuid(formUuid);
        for (FormFieldMembership membership : formFieldMembershipDAO.findByOwner(form.getOwner())) {
            available.remove(membership.getField());
        }
        return Response.ok(available).build();
    }

    @POST @Path(MEMBERSHIPS_ENDPOINT)
    public Response addFieldMemberships (@PathParam(UUID_PARAM) String formUuid, FormFieldMembershipRequest request) {
        Form form = formDAO.findByUuid(formUuid);
        if (form == null) return ResourceUtil.notFound(formUuid);

        FormField formField = formFieldDAO.findByUuid(request.getFieldUuid());
        if (formField == null) return ResourceUtil.notFound(request.getFieldUuid());

        FormFieldMembership membership = new FormFieldMembership();
        membership.setForm(form);
        membership.setField(formField);
        membership.setPlacement(request.getPlacement());

        membership = formFieldMembershipDAO.create(membership);
        return Response.created(URI.create(membership.getUuid())).build();
    }

    @GET @Path(MEMBERSHIPS_ENDPOINT+"/"+MEMBERSHIP_UUID)
    public Response getFieldMembership (@PathParam(UUID_PARAM) String formUuid,
                                        @PathParam(MEMBERSHIP_UUID_PARAM) String membershipUuid) {

        FormFieldMembership membership = formFieldMembershipDAO.findByUuid(membershipUuid);
        if (membership == null) return ResourceUtil.notFound(membershipUuid);

        return Response.ok(membership).build();
    }

    @PUT @Path(MEMBERSHIPS_ENDPOINT+"/"+MEMBERSHIP_UUID)
    public Response editFieldMembership (@PathParam(UUID_PARAM) String formUuid,
                                         @PathParam(MEMBERSHIP_UUID_PARAM) String membershipUuid,
                                         FormFieldMembershipRequest request) {

        FormFieldMembership membership = formFieldMembershipDAO.findByUuid(membershipUuid);
        if (membership == null) return ResourceUtil.notFound(membershipUuid);

        membership.setPlacement(request.getPlacement());
        formFieldMembershipDAO.update(membership);

        return Response.ok().build();
    }

    @DELETE @Path(MEMBERSHIPS_ENDPOINT+"/"+MEMBERSHIP_UUID)
    public Response removeFieldMembership (@PathParam(UUID_PARAM) String formUuid,
                                           @PathParam(MEMBERSHIP_UUID_PARAM) String membershipUuid) {

        FormFieldMembership membership = formFieldMembershipDAO.findByUuid(membershipUuid);
        if (membership == null) return ResourceUtil.notFound(membershipUuid);
        if (!membership.getForm().getUuid().equals(formUuid)) return ResourceUtil.notFound();

        formFieldMembershipDAO.delete(membership.getUuid());
        return Response.ok().build();
    }
}
