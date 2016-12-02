package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import org.cobbzilla.util.daemon.Await;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.NamedEntity;
import org.cobbzilla.wizard.util.RestResponse;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.http.HttpStatusCodes.OK;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.json.JsonUtil.jsonWithComments;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

@Slf4j
public class ModelSetup {

    public static final String ALLOW_UPDATE_PROPERTY = "_update";
    public static final String PERFORM_SUBST_PROPERTY = "_subst";

    // use 1 + half of all other processors, max of 10
    public static int maxConcurrency = Math.min(10, 1 + Math.max(1, Runtime.getRuntime().availableProcessors()/2));
    public static final long CHILD_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
    static { log.info("ModelSetup: maxConcurrency="+maxConcurrency); }

    public static final Map<Integer, Map<Identifiable, Identifiable>> entityCache = new HashMap<>();

    public static LinkedHashMap<String, String> buildModel(File manifest) {
        final String[] models = json(FileUtil.toStringOrDie(manifest), String[].class, JsonUtil.FULL_MAPPER_ALLOW_COMMENTS);
        final LinkedHashMap<String, String> modelJson = new LinkedHashMap<>(models.length);
        for (String model : models) {
            modelJson.put(model, FileUtil.toStringOrDie(abs(manifest.getParentFile()) + "/" + model + ".json"));
        }
        return modelJson;
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api,
                                                           String entityConfigsEndpoint,
                                                           String prefix,
                                                           ModelSetupListener listener) throws Exception {
        return setupModel(api, entityConfigsEndpoint, prefix, "manifest", listener);
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api,
                                                           String entityConfigsEndpoint,
                                                           String prefix,
                                                           String manifest,
                                                           ModelSetupListener listener) throws Exception {
        final String[] models = json(stream2string(prefix + manifest + ".json"), String[].class, JsonUtil.FULL_MAPPER_ALLOW_COMMENTS);
        final LinkedHashMap<String, String> modelJson = new LinkedHashMap<>(models.length);
        for (String model : models) {
            modelJson.put(model, stream2string(prefix + model + ".json"));
        }
        return setupModel(api, entityConfigsEndpoint, modelJson, listener);
    }

    public static LinkedHashMap<String, String> setupModel(ApiClientBase api,
                                                           String entityConfigsEndpoint,
                                                           LinkedHashMap<String, String> models,
                                                           ModelSetupListener listener) throws Exception {
        for (Map.Entry<String, String> model : models.entrySet()) {
            String modelName = model.getKey();
            final String json = model.getValue();
            final String entityType = getEntityTypeFromString(modelName);

            setupJson(api, entityConfigsEndpoint, entityType, json, listener);
        }
        return models;
    }

    private static Map<String, String> modelHashCache = new HashMap<>();

    public static String modelHash(String prefix, String manifest) {
        final String cacheKey = prefix + "/" + manifest;
        String hash = modelHashCache.get(cacheKey);
        if (hash == null) {
            final String[] models = json(stream2string(prefix + manifest + ".json"), String[].class, JsonUtil.FULL_MAPPER_ALLOW_COMMENTS);
            StringBuilder b = new StringBuilder();
            for (String model : models) {
                b.append(stream2string(prefix + model + ".json"));
            }
            hash = sha256_hex(b.toString());
            modelHashCache.put(cacheKey, hash);
        }
        return hash;
    }

    public static void setupJson(ApiClientBase api,
                                 String entityConfigsEndpoint,
                                 String entityType,
                                 String json,
                                 ModelSetupListener listener) throws Exception {
        if (listener != null) listener.preEntityConfig(entityType);
        final EntityConfig entityConfig = api.get(entityConfigsEndpoint + "/" + entityType, EntityConfig.class);
        if (listener != null) listener.postEntityConfig(entityType, entityConfig);

        final Class<? extends Identifiable> entityClass = forName(entityConfig.getClassName());
        final ModelEntity[] entities = parseEntities(json, entityClass);
        for (ModelEntity entity : entities) {
            final LinkedHashMap<String, Identifiable> context = new LinkedHashMap<>();
            createEntity(api, entityConfig, entity, context, listener);
        }
    }

    public static ModelEntity[] parseEntities(String json,
                                              Class<? extends Identifiable> entityClass) {
        final JsonNode[] nodes = jsonWithComments(json, JsonNode[].class);
        final ModelEntity[] entities = new ModelEntity[nodes.length];
        for (int i=0; i<nodes.length; i++) {
            final JsonNode node = nodes[i];
            entities[i] = buildModelEntity(node, entityClass);
        }
        return entities;
    }

    public static ModelEntity buildModelEntity(JsonNode node,
                                               Class<? extends Identifiable> entityClass) {
        final Enhancer enhancer = new Enhancer();
        enhancer.setInterfaces(new Class[]{ModelEntity.class});
        enhancer.setSuperclass(entityClass);
        enhancer.setCallback(new ModelEntityInvocationHandler(node, entityClass));
        return (ModelEntity) enhancer.create();
    }

    // strip off anything after the first underscore (or period, in case a ".json" file is given)
    public static String getEntityTypeFromString(String entityType) {
        if (entityType.contains("_")) return entityType.substring(0, entityType.indexOf("_"));
        if (entityType.contains(".")) return entityType.substring(0, entityType.indexOf("."));
        return entityType;
    }

    protected static void createEntity(final ApiClientBase api,
                                       EntityConfig entityConfig,
                                       ModelEntity request,
                                       final LinkedHashMap<String, Identifiable> context,
                                       final ModelSetupListener listener) throws Exception {

        Identifiable entity = request;

        // does it already exist?
        final String entityType = getRawClass(entity.getClass().getSimpleName());
        final String updateUri = entityConfig.getUpdateUri();
        if (updateUri != null && !updateUri.equals(":notSupported")) {
            final String getUri = processUri(context, entity, updateUri);
            if (getUri != null) {
                if (listener != null) listener.preLookup(entity);
                final RestResponse response = api.doGet(getUri);
                if (listener != null) listener.postLookup(entity, request, response);
                switch (response.status) {
                    case OK:
                        if (request.allowUpdate()) {
                            final Identifiable existing = getCached(api, entity);
                            if (existing != null) ReflectionUtil.copy(existing, entity);
                            log.info("createEntity: "+entityType+" already exists, updating");
                            entity = update(api, context, entityConfig, entity, listener);
                        } else {
                            log.info("createEntity: "+entityType+" already exists: "+getUri);
                            entity = json(response.json, request.getEntity().getClass());
                        }
                        break;
                    case NOT_FOUND:
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
        addToCache(api, entity);

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

                    final ExecutorService exec = Executors.newFixedThreadPool(maxConcurrency);
                    final Set<Future> futures = new HashSet<>();
                    for (final JsonNode child : children) {
                         futures.add(exec.submit(new Runnable() {
                            @Override public void run() {
                                try {
                                    createEntity(api, childConfig, buildModelEntity(child, childClass), context, listener);
                                } catch (Exception e) {
                                    die("run: "+e, e);
                                }
                            }
                        }));
                    }
                    Await.awaitAll(futures, CHILD_TIMEOUT);
                }
            }
        }
    }

    private static void addToCache(ApiClientBase api, Identifiable entity) {
        Map<Identifiable, Identifiable> cache = entityCache.get(api.hashCode());
        if (cache == null) {
            cache = new HashMap<>();
            entityCache.put(api.hashCode(), cache);
        }
        cache.put(entity, entity);
    }

    private static Identifiable getCached(ApiClientBase api, Identifiable entity) {
        final Map<Identifiable, Identifiable> cache = entityCache.get(api.hashCode());
        return cache == null ? null : cache.get(entity);
    }

    protected static <T extends Identifiable> T create(ApiClientBase api,
                                                       LinkedHashMap<String, Identifiable> ctx,
                                                       EntityConfig entityConfig,
                                                       T entity,
                                                       ModelSetupListener listener) throws Exception {
        final String uri = processUri(ctx, entity, entityConfig.getCreateUri());

        // if the entity has a parent, it will want that parent's UUID in that field
        setParentFields(ctx, entityConfig, entity);

        if (listener != null) {
            if (entity instanceof ModelEntity && ((ModelEntity) entity).performSubstitutions()) {
                entity = listener.subst(entity);
            }
            listener.preCreate(entityConfig, entity);
        }
        log.info("create: creating " + entityConfig.getName() + (entity instanceof NamedEntity ? ": " + ((NamedEntity) entity).getName() : ""));
        final T created;
        switch (entityConfig.getCreateMethod().toLowerCase()) {
            case "put":  created = api.put(uri, entity); break;
            case "post": created = api.post(uri, entity); break;
            default: return die("invalid create method: "+entityConfig.getCreateMethod());
        }
        if (listener != null) listener.postCreate(entityConfig, entity, created);
        return created;
    }

    protected static <T extends Identifiable> T update(ApiClientBase api,
                                                       LinkedHashMap<String, Identifiable> ctx,
                                                       EntityConfig entityConfig,
                                                       T entity,
                                                       ModelSetupListener listener) throws Exception {
        final String uri = processUri(ctx, entity, entityConfig.getUpdateUri());

        // if the entity has a parent, it will want that parent's UUID in that field
        setParentFields(ctx, entityConfig, entity);

        if (listener != null) listener.preUpdate(entityConfig, entity);
        final T updated;
        switch (entityConfig.getUpdateMethod().toLowerCase()) {
            case "put":  updated = api.put(uri, entity); break;
            case "post": updated = api.post(uri, entity); break;
            default: return die("invalid update method: "+entityConfig.getCreateMethod());
        }
        if (listener != null) listener.postUpdate(entityConfig, entity, updated);
        return updated;
    }

    private static <T extends Identifiable> void setParentFields(LinkedHashMap<String, Identifiable> ctx, EntityConfig entityConfig, T entity) {
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
    }

    private static String processUri(LinkedHashMap<String, Identifiable> ctx, Identifiable entity, String uri) {

        if (entity instanceof ModelEntity) entity = ((ModelEntity) entity).getEntity();

        for (Map.Entry<String, Identifiable> entry : ctx.entrySet()) {
            final String type = getRawClass(entry.getKey());
            final Identifiable value = entry.getValue();
            final Map<String, Object> ctxEntryProps = ReflectionUtil.toMap(value instanceof ModelEntity ? ((ModelEntity) value).getEntity() : value);
            for (String name : ctxEntryProps.keySet()) {
                uri = uri.replace("{" + type + "." + name + "}", urlEncode(ctxEntryProps.get(name).toString()));
            }
        }
        final Map<String, Object> entityProps = ReflectionUtil.toMap(entity);
        for (String name : entityProps.keySet()) {
            if (name.contains("$$")) name = name.substring(0, name.indexOf("$$"));
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

    private static String getRawClass(String className) {
        return className.contains("$$") ? className.substring(0, className.indexOf("$$")) : className;
    }

    private static class ModelEntityInvocationHandler implements InvocationHandler {

        private final boolean update;
        private final boolean subst;
        @Getter private final Identifiable entity;

        public ModelEntityInvocationHandler(JsonNode node, Class<? extends Identifiable> entityClass) {
            update = hasSpecialProperty(node, ALLOW_UPDATE_PROPERTY);
            subst = hasSpecialProperty(node, PERFORM_SUBST_PROPERTY);
            this.entity = json(node, entityClass);
        }

        private boolean hasSpecialProperty(JsonNode node, String prop) {
            boolean val = node.has(prop) && node.get(prop).booleanValue();
            ((ObjectNode) node).remove(prop);
            return val;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "allowUpdate": return update;
                case "performSubstitutions": return subst;
                case "getEntity": return entity;
                default: return method.invoke(entity, args);
            }
        }
    }
}
