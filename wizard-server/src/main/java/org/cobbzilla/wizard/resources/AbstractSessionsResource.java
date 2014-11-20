package org.cobbzilla.wizard.resources;

import com.qmino.miredot.annotations.ReturnType;
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

    /**
     * Lookup a session
     * @param uuid The session ID
     * @return The object associated with the session
     * @statuscode 404 session not found
     */
    @GET
    @Path("/{uuid}")
    @ReturnType("T")
    public Response find (@PathParam("uuid") String uuid) {

        final T entity = getSessionDAO().find(uuid);
        if (entity == null) return ResourceUtil.notFound(uuid);

        return Response.ok(entity).build();
    }

    /**
     * Create a session
     * @param entity The object to store for the session
     * @return The object that was stored
     */
    @PUT
    @ReturnType("T")
    public Response create (T entity) {
        getSessionDAO().create(entity);
        return Response.ok(entity).build();
    }

    /**
     * Update a session
     * @param entity The new object data
     * @return The object that was stored
     */
    @POST
    @Path("/{uuid}")
    @ReturnType("T")
    public Response update (@PathParam("uuid") String uuid, T entity) {
        entity.setUuid(uuid);
        getSessionDAO().update(uuid, entity);
        return Response.ok(entity).build();
    }

    /**
     * Delete a session
     * @return Just an HTTP code
     * @statuscode 200 in all cases
     */
    @DELETE
    @Path("/{uuid}")
    @ReturnType("java.lang.Void")
    public Response invalidate (@PathParam("uuid") String uuid) {
        getSessionDAO().invalidate(uuid);
        return Response.ok().build();
    }

}
