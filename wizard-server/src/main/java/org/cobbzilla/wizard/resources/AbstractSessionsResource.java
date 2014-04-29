package org.cobbzilla.wizard.resources;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.cobbzilla.wizard.model.Identifiable;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public abstract class AbstractSessionsResource<T extends Identifiable> {

    protected abstract AbstractSessionDAO<T> getSessionDAO();

    @GET
    @Path("/{uuid}")
    public Response find (@PathParam("uuid") String uuid) {

        final T entity = getSessionDAO().find(uuid);
        if (entity == null) return ResourceUtil.notFound(uuid);

        return Response.ok(entity).build();
    }

    @PUT
    public Response create (T entity) {
        getSessionDAO().create(entity);
        return Response.ok(entity).build();
    }

    @POST
    @Path("/{uuid}")
    public Response update (@PathParam("uuid") String uuid, T entity) {
        entity.setUuid(uuid);
        getSessionDAO().update(uuid, entity);
        return Response.ok(entity).build();
    }

    @DELETE
    @Path("/{uuid}")
    public Response invalidate (@PathParam("uuid") String uuid) {
        getSessionDAO().invalidate(uuid);
        return Response.ok().build();
    }

}
