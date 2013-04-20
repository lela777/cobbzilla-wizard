package org.cobbzilla.wizard.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.Identifiable;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public abstract class AbstractResource<T extends Identifiable> {

    public static final String UUID_PARAM = "uuid";
    public static final String UUID = "{"+UUID_PARAM+"}";

    protected abstract AbstractCRUDDAO<T> dao ();

    protected abstract String getEndpoint();

    @GET
    public List<T> index() {
        return dao().findAll();
    }

    @POST
    public Response create(@Valid T thing) {
        dao().create(thing);
        return Response.created(URI.create(getEndpoint() + "/" + thing.getUuid())).build();
    }

    @Path("/"+UUID)
    @GET
    public Response find(@PathParam(UUID_PARAM) String uuid) {
        final T thing = dao().findByUuid(uuid);
        return thing == null ? ResourceUtil.notFound(uuid) : Response.ok(postProcess(thing)).build();
    }

    protected T postProcess(T thing) { return thing; }

    @Path("/"+UUID)
    @PUT
    public Response update(@PathParam(UUID_PARAM) String uuid, @Valid T thing) {
        Response response;
        if (dao().exists(uuid)) {
            thing.setUuid(uuid);
            dao().update(thing);
            response = Response.noContent().build();
        } else {
            response = ResourceUtil.notFound(uuid);
        }
        return response;
    }

}
