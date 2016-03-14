package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.wizard.dao.shard.ShardMapDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.cobbzilla.wizard.model.shard.ShardSetStatus;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

public abstract class AbstractShardsResource<E extends ShardMap, A extends Identifiable> {

    private static final String[] UPDATE_FIELDS = {"range", "url", "allowRead", "allowWrite"};

    protected abstract boolean isAuthorized(HttpContext ctx, A account);
    protected abstract ShardMapDAO<E> getShardDAO();

    @Autowired private HasDatabaseConfiguration configuration;

    class ShardContext {
        public A account;
        public E shard;
        public Response response;
        public boolean hasResponse () { return response != null; }
        public ShardContext (HttpContext ctx) { this(ctx, null); }
        public ShardContext (HttpContext ctx, String shardUuid) {
            account = userPrincipal(ctx);
            if (!isAuthorized(ctx, account)) response = forbidden();
            if (shardUuid != null) {
                shard = getShardDAO().findByUuid(shardUuid);
                if (shard == null) response = notFound(shardUuid);
            }
        }
    }

    private List<ShardSetStatus> toShardSetStatus(Collection<String> names) {
        final List<ShardSetStatus> list = new ArrayList<>();
        for (String name : names) {
            list.add(getShardDAO().validate(name));
        }
        return list;
    }

    protected Collection<String> getConfiguredShardSets() { return configuration.getDatabase().getShardSetNames(); }

    @GET
    public Response findAllShardSets (@Context HttpContext context) {
        final ShardContext ctx = new ShardContext(context);
        if (ctx.hasResponse()) return ctx.response;
        return ok(toShardSetStatus(getConfiguredShardSets()));
    }

    @GET
    @Path("/{shardSet}")
    public Response findShardSet(@Context HttpContext context,
                                 @PathParam("shardSet") String shardSet) {
        final ShardContext ctx = new ShardContext(context);
        if (ctx.hasResponse()) return ctx.response;

        if (!getConfiguredShardSets().contains(shardSet)) return notFound(shardSet);
        return ok(getShardDAO().validate(shardSet));
    }

    @GET
    @Path("/{shardSet}/shard/{uuid}")
    public Response findShard(@Context HttpContext context,
                              @PathParam("uuid") String uuid,
                              @PathParam("shardSet") String shardSet) {
        final ShardContext ctx = new ShardContext(context);
        if (ctx.hasResponse()) return ctx.response;

        if (!getConfiguredShardSets().contains(shardSet)) return notFound(shardSet);
        if (!ctx.shard.getShardSet().equals(shardSet)) invalid("err.shardSet.mismatch");
        return ok(ctx.shard);
    }

    @PUT
    @Path("/{shardSet}")
    public Response createShard (@Context HttpContext context,
                                 @PathParam("shardSet") String shardSet,
                                 @Valid ShardMap request) {
        final ShardContext ctx = new ShardContext(context);
        if (ctx.hasResponse()) return ctx.response;
        if (!getConfiguredShardSets().contains(shardSet)) return notFound(shardSet);
        if (!request.getShardSet().equals(shardSet)) return invalid("err.shardSet.mismatch");
        final E shard = getShardDAO().newEntity();
        copy(shard, request, UPDATE_FIELDS);
        shard.setShardSet(shardSet);
        return ok(getShardDAO().create(shard));
    }

    @POST
    @Path("/{shardSet}/shard/{uuid}")
    public Response updateShard (@Context HttpContext context,
                                 @PathParam("shardSet") String shardSet,
                                 @PathParam("uuid") String uuid,
                                 @Valid ShardMap request) {
        final ShardContext ctx = new ShardContext(context, uuid);
        if (ctx.hasResponse()) return ctx.response;

        if (!getConfiguredShardSets().contains(shardSet)) return notFound(shardSet);
        if (!request.getShardSet().equals(shardSet)) return invalid("err.shardSet.mismatch");
        if (!ctx.shard.getShardSet().equals(shardSet)) return invalid("err.shardSet.mismatch");
        copy(ctx.shard, request, UPDATE_FIELDS);
        return ok(getShardDAO().update(ctx.shard));
    }

    @DELETE
    @Path("/{shardSet}/shard/{uuid}")
    public Response deleteShard (@Context HttpContext context,
                                 @PathParam("shardSet") String shardSet,
                                 @PathParam("uuid") String uuid) {
        final ShardContext ctx = new ShardContext(context, uuid);
        if (ctx.hasResponse()) return ctx.response;

        if (!getConfiguredShardSets().contains(shardSet)) return notFound(shardSet);

        getShardDAO().refreshCache(true);

        if (!getShardDAO().validateWithShardRemoved(shardSet, ctx.shard).isValid()) {
            return invalid("err.deleteWouldCreateInvalidShardSet");
        }

        getShardDAO().delete(uuid);
        return ok();
    }
}