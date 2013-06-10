package org.cobbzilla.wizard.resources;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;

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
    public List<T> index(@QueryParam(ResultPage.PARAM_USE_PAGINATION) Boolean usePagination,
                         @QueryParam(ResultPage.PARAM_PAGE_NUMBER) Integer pageNumber,
                         @QueryParam(ResultPage.PARAM_PAGE_SIZE) Integer pageSize,
                         @QueryParam(ResultPage.PARAM_SORT_FIELD) String sortField,
                         @QueryParam(ResultPage.PARAM_SORT_ORDER) String sortOrder,
                         @QueryParam(ResultPage.PARAM_FILTER) String filter) {
        if (usePagination == null || !usePagination) {
            return dao().findAll();
        } else {
            return dao().query(new ResultPage(pageNumber, pageSize, sortField, sortOrder, filter));
        }
    }

    @POST
    public Response create(@Valid T thing) {
        thing = dao().create(thing);
//        final URI location = URI.create(getEndpoint() + "/" + thing.getUuid());
        final URI location = URI.create(thing.getUuid());
        return Response.created(location).build();
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
        final T found = dao().findByUuid(uuid);
        if (found != null) {
            thing.setUuid(uuid);
            dao().update(thing);
            response = Response.noContent().build();
        } else {
            response = ResourceUtil.notFound(uuid);
        }
        return response;
    }

    @Path("/"+UUID)
    @DELETE
    public Response delete(@PathParam(UUID_PARAM) String uuid) {
        final T found = dao().findByUuid(uuid);
        if (found == null) return ResourceUtil.notFound();
        dao().delete(uuid);
        return Response.noContent().build();
    }

}
