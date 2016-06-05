package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.wizard.dao.NamedIdentityBaseDAO;
import org.cobbzilla.wizard.model.NamedIdentityBase;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@SuppressWarnings("Duplicates")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public abstract class AbstractCachedNamedSubResource<R extends AbstractCachedSubResource,
                                                     E extends NamedIdentityBase>
        extends AbstractCachedSubResource<R> {

    public abstract NamedIdentityBaseDAO<E> getDao ();

    protected boolean canCreate(HttpContext ctx) { return true; }
    protected boolean canUpdate(HttpContext ctx) { return true; }
    protected boolean canDelete(HttpContext ctx) { return true; }

    @GET
    public Response findAll (@Context HttpContext ctx) {
        return ok(getDao().findAll());
    }

    @GET
    @Path("/{name}")
    public Response findOne (@Context HttpContext ctx,
                             @PathParam("name") String name) {
        final E found = getDao().findByName(name);
        return found != null ? ok(found) : notFound(name);
    }

    @PUT
    public Response create (@Context HttpContext ctx,
                            @Valid E thing) {
        if (!canCreate(ctx)) return forbidden();
        final E found = getDao().findByName(thing.getName());
        if (found != null) return invalid("err.name.notUnique");
        return ok(getDao().create(getDao().newEntity(thing)));
    }

    @POST
    @Path("/{name}")
    public Response update (@Context HttpContext ctx,
                            @PathParam("name") String name,
                            @Valid E thing) {
        if (!canUpdate(ctx)) return forbidden();
        if (!name.equals(thing.getName())) return invalid("err.name.mismatch");
        final E found = getDao().findByName(name);
        if (found == null) return notFound(name);
        return ok(getDao().update((E) found.update(thing)));
    }

    @DELETE
    @Path("/{name}")
    public Response delete (@Context HttpContext ctx,
                            @PathParam("name") String name) {
        if (!canDelete(ctx)) return forbidden();
        final E found = getDao().findByName(name);
        if (found == null) return notFound(name);
        getDao().delete(name);
        return ok();
    }

}
