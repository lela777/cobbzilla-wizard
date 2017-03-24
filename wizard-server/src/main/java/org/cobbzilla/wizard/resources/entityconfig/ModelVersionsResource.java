package org.cobbzilla.wizard.resources.entityconfig;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.wizard.dao.entityconfig.ModelVersionDAO;
import org.cobbzilla.wizard.model.entityconfig.ModelVersion;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.model.entityconfig.ModelVersion.CURRENT;
import static org.cobbzilla.wizard.model.entityconfig.ModelVersion.ERR_MODEL_VERSION_ALREADY_SUCCESSFULLY_APPLIED;
import static org.cobbzilla.wizard.model.entityconfig.ModelVersion.ERR_MODEL_VERSION_INVALID;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public abstract class ModelVersionsResource<V extends ModelVersion> {

    protected abstract ModelVersionDAO<V> getVersionDAO ();

    protected abstract boolean isPermitted(HttpContext context, V request);

    @GET
    public Response findAllVersions (@Context HttpContext ctx) {
        if (!isPermitted(ctx, null)) return forbidden();
        return ok(getVersionDAO().findAll());
    }

    @GET @Path("/{id}")
    public Response findVersion (@Context HttpContext ctx,
                                 @PathParam("id") String id) {
        if (!isPermitted(ctx, null)) return forbidden();
        final ModelVersion version = CURRENT.equals(id) ? getVersionDAO().findCurrentVersion() : getVersionDAO().findByUuidOrVersion(id);
        return version == null ? notFound(id) : ok(version);
    }

    @PUT
    public Response addVersion (@Context HttpContext ctx,
                                V request) {
        if (!isPermitted(ctx, request)) return forbidden();
        final V version = getVersionDAO().findCurrentVersion();
        if (version != null) {
            if (version.getVersion() > request.getVersion()) {
                return invalid(ERR_MODEL_VERSION_INVALID, "cannot create version before current version ("+version.getVersion()+")", String.valueOf(request.getVersion()));
            }
            if (version.getVersion() == request.getVersion()) {
                if (version.isSuccess()) {
                    return invalid(ERR_MODEL_VERSION_ALREADY_SUCCESSFULLY_APPLIED, "model version was already successfully applied", String.valueOf(request.getVersion()));
                } else {
                    version.update(request);
                    return ok(getVersionDAO().update(version));
                }
            }
        }
        return ok(getVersionDAO().create(getVersionDAO().newEntity(request)));
    }

}
