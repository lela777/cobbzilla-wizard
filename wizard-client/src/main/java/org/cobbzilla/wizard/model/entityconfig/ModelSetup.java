package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.util.RestResponse;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.http.HttpStatusCodes.OK;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

@Slf4j
public class ModelSetup {

    public static LinkedHashMap<String, String> buildModel(File manifest) {
        final String[] models = json(FileUtil.toStringOrDie(manifest), String[].class, JsonUtil.FULL_MAPPER_ALLOW_COMMENTS);
        final LinkedHashMap<String, String> modelJson = new LinkedHashMap<>(models.length);
        for (String model : models) {
            modelJson.put(model, FileUtil.toStringOrDie(abs(manifest.getParentFile()) + "/" + model + ".json"));
        }
        return modelJson;
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api, String entityConfigsEndpoint, String prefix, ModelSetupListener listener) throws Exception {
        return setupModel(api, entityConfigsEndpoint, prefix, "manifest", listener);
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api, String entityConfigsEndpoint, String prefix, String manifest, ModelSetupListener listener) throws Exception {
        final String[] models = json(stream2string(prefix + manifest + ".json"), String[].class, JsonUtil.FULL_MAPPER_ALLOW_COMMENTS);
        final LinkedHashMap<String, String> modelJson = new LinkedHashMap<>(models.length);
        for (String model : models) {
            modelJson.put(model, stream2string(prefix + model + ".json"));
        }
        return setupModel(api, entityConfigsEndpoint, modelJson, listener);
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api, String entityConfigsEndpoint, LinkedHashMap<String, String> models, ModelSetupListener listener) throws Exception {
        for (Map.Entry<String, String> model : models.entrySet()) {

            String entityType = model.getKey();

            // strip off anything after the first underscore
            if (entityType.contains("_")) entityType = entityType.substring(0, entityType.indexOf("_"));

            if (listener != null) listener.preEntityConfig(entityType);
            final EntityConfig entityConfig = api.get(entityConfigsEndpoint + "/" + entityType, EntityConfig.class);
            if (listener != null) listener.postEntityConfig(entityType, entityConfig);

            final Class<? extends Identifiable> entityClass = forName(entityConfig.getClassName());
            final Identifiable[] entities = (Identifiable[]) jsonWithComments(model.getValue(), arrayClass(entityClass));
            for (Identifiable entity : entities) {
                final LinkedHashMap<String, Identifiable> context = new LinkedHashMap<>();
                createEntity(api, entityConfig, entity, context, listener);
            }
        }
        return models;
    }

    protected static void createEntity(ApiClientBase api,
                                       EntityConfig entityConfig,
                                       Identifiable request,
                                       LinkedHashMap<String, Identifiable> context,
                                       ModelSetupListener listener) throws Exception {

        Identifiable entity = request;

        // does it already exist?
        final String entityType = entity.getClass().getSimpleName();
        final String updateUri = entityConfig.getUpdateUri();
        if (updateUri != null && !updateUri.equals(":notSupported")) {
            final String getUri = processUri(context, entity, updateUri);
            if (getUri != null) {
                if (listener != null) listener.preLookup(entity);
                final RestResponse response = api.doGet(getUri);
                if (listener != null) listener.postLookup(entity, request, response);
                switch (response.status) {
                    case OK:
                        log.info("createEntity: "+entityType+" already exists");
                        entity = json(response.json, request.getClass());
                        break;
                    case NOT_FOUND:
                        log.info("createEntity: creating " + entityType);
                        entity = create(api, context, entityConfig, entity, listener);
                        break;
                    default:
                        die("createEntity: error creating " + entityType + ": " + response);
                }
            } else {
                entity = create(api, context, entityConfig, entity, listener);
            }
        } else {
            entity = create(api, context, entityConfig, entity, listener);
        }

        // copy children if present in request (they wouldn't be in object returned from server)
        if (entity instanceof ParentEntity) {
            ((ParentEntity) entity).setChildren(((ParentEntity) request).getChildren());
        }

        // create and add to context
        context.put(entityType, entity);

        // check for child objects
        if (entity instanceof ParentEntity) {
            final ParentEntity parent = (ParentEntity) entity;
            if (parent.hasChildren()) {
                // sanity check
                if (!entityConfig.hasChildren()) die("input data has children but entity config does not support them: "+entityConfig.getClassName());

                for (String childEntityType : entityConfig.getChildren().keySet()) {

                    // these are the objects we want to create
                    final JsonNode[] children = parent.getChildren().get(childEntityType);
                    if (children == null || children.length == 0) continue;

                    // this tells us how to create them
                    final EntityConfig childConfig = entityConfig.getChildren().get(childEntityType);

                    // needed to read/write JSON correctly
                    String childClassName = childConfig.getClassName();
                    if (childClassName == null) childClassName = entity.getClass().getPackage().getName() + "." + childEntityType;
                    final Class<? extends Identifiable> childClass = forName(childClassName);

                    for (JsonNode child : children) {
                        createEntity(api, childConfig, fromJson(child, childClass), context, listener);
                    }
                }
            }
        }
    }

    protected static <T extends Identifiable> T create(ApiClientBase api,
                                                       LinkedHashMap<String, Identifiable> ctx,
                                                       EntityConfig entityConfig,
                                                       T entity,
                                                       ModelSetupListener listener) throws Exception {
        final String uri = processUri(ctx, entity, entityConfig.getCreateUri());

        // if the entity has a parent, it will want that parent's UUID in that field
        if (entityConfig.hasParentField()) {
            final EntityFieldConfig parentField = entityConfig.getParentField();

            String parentFieldName = parentField.getName();
            if (parentFieldName != null) {
                String parentEntityType = parentField.getReference().getEntity();
                if (parentEntityType.equals(":parent")) parentEntityType = parentFieldName;

                boolean ok = false;
                for (Identifiable candidate : ctx.values()) {
                    if (candidate.getClass().getSimpleName().equalsIgnoreCase(parentEntityType)) {
                        ReflectionUtil.set(entity, parentFieldName, ReflectionUtil.get(candidate, parentField.getReference().getField()));
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    die("create: could not find parent (type=" + parentEntityType + ", field=" + parentFieldName + ") of entity (" + entity.getClass().getSimpleName() + "): " + entity);
                }
            } else {
                log.debug("no parentFieldName found for " + entity.getClass().getSimpleName() + ", not setting");
            }
        }

        if (listener != null) listener.preCreate(entityConfig, entity);
        final T created;
        switch (entityConfig.getCreateMethod().toLowerCase()) {
            case "put":  created = api.put(uri, entity); break;
            case "post": created = api.post(uri, entity); break;
            default: return die("invalid create method: "+entityConfig.getCreateMethod());
        }
        if (listener != null) listener.postCreate(entityConfig, entity, created);
        return created;
    }

    private static String processUri(LinkedHashMap<String, Identifiable> ctx, Identifiable entity, String uri) {

        for (Map.Entry<String, Identifiable> entry : ctx.entrySet()) {
            Map<String, Object> ctxEntryProps = ReflectionUtil.toMap(entry.getValue());
            for (String name : ctxEntryProps.keySet()) {
                uri = uri.replace("{" + entry.getKey() + "." + name + "}", urlEncode(ctxEntryProps.get(name).toString()));
            }
        }
        final Map<String, Object> entityProps = ReflectionUtil.toMap(entity);
        for (String name : entityProps.keySet()) {
            uri = uri.replace("{" + name + "}", urlEncode(entityProps.get(name).toString()));
        }
        // if a {uuid} remains, try putting in the name, if we have one
        if (uri.contains("{uuid}") && entityProps.containsKey("name")) {
            uri = uri.replace("{uuid}", urlEncode(entityProps.get("name").toString()));
        }
        if (uri.contains("{uuid}")) {
            log.debug("Could not replace {uuid} found in URL, returning null: "+uri);
            return null;
        }
        if (uri.contains("{")) die("Could not replace all variables in URL: "+uri);
        return uri.startsWith("/") ? uri : "/" + uri;
    }
}
