package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.entityconfig.EntityConfig;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStream;
import static org.cobbzilla.util.json.JsonUtil.FULL_MAPPER_ALLOW_COMMENTS;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.string.StringUtil.packagePath;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Slf4j
public abstract class AbstractEntityConfigsResource {

    public static final String ENTITY_CONFIG_BASE = "entity-config";

    protected abstract HasDatabaseConfiguration getConfiguration();

    protected long getConfigRefreshInterval() { return TimeUnit.DAYS.toMillis(30); }
    protected abstract boolean authorized(HttpContext ctx);
    protected File getLocalConfig(EntityConfig name) { return null; }

    @Getter(AccessLevel.PROTECTED) private final AutoRefreshingReference<Map<String, EntityConfig>> configs = new EntityConfigsMap();
    public boolean refresh() { configs.set(null); return true; }
    public boolean refresh(AutoRefreshingReference<Map<String, EntityConfig>> configs) { return refresh(configs); }

    @GET
    @Path("/{name}")
    public Response getConfig (@Context HttpContext ctx,
                               @PathParam("name") String name,
                               @QueryParam("debug") boolean debug,
                               @QueryParam("refresh") boolean refresh) {

        if (!authorized(ctx)) return forbidden();

        final AutoRefreshingReference<Map<String, EntityConfig>> configs = getConfigs();
        if (debug || refresh) {
            log.info("getConfig: refreshing");
            refresh(configs);
        }
        final Map<String, EntityConfig> configMap = configs.get();

        final EntityConfig config;
        synchronized (configMap) {
            config = configMap.get(capitalize(name));
        }

        if (debug && config != null) {
            EntityConfig localConfig = null;
            try {
                localConfig = toEntityConfig(forName(config.getClassName()));
            } catch (Exception e) {
                log.warn("getConfig(" + name + "): error loading entity config", e);
            }
            if (localConfig != null) return ok(localConfig);
        }

        return config == null ? notFound(name) : ok(config);
    }

    public class EntityConfigsMap extends AutoRefreshingReference<Map<String, EntityConfig>> {

        @Override public Map<String, EntityConfig> refresh() {
            final Map<String, EntityConfig> configMap = new HashMap<>();
            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Embeddable.class));
            final HashSet<Class<?>> classesWithoutConfigs = new HashSet<>();
            for (String pkg : getConfiguration().getDatabase().getHibernate().getEntityPackages()) {
                for (BeanDefinition def : scanner.findCandidateComponents(pkg)) {
                    final Class<?> clazz = forName(def.getBeanClassName());
                    final EntityConfig config = toEntityConfig(clazz);
                    if (config != null) {
                        configMap.put(clazz.getName(), config);
                        if (configMap.containsKey(clazz.getSimpleName())) {
                            log.warn("config already contains "+clazz.getSimpleName()+", not overwriting with "+clazz.getName());
                        } else {
                            configMap.put(clazz.getSimpleName(), config);
                        }
                    } else {
                        classesWithoutConfigs.add(clazz);
                    }
                }
            }

            if (classesWithoutConfigs.size() > 0) {
                log.warn("No config(s) found for class(es): " + StringUtil.toString(classesWithoutConfigs));
            }

            synchronized (configs) {
                configs.set(configMap);
            }
            return configs.get();
        }

        @Override public long getTimeout() { return getConfigRefreshInterval(); }
    }

    private EntityConfig getEntityConfig(Class<?> clazz) throws Exception {
        EntityConfig entityConfig;
        try {
            final InputStream in = loadResourceAsStream(ENTITY_CONFIG_BASE + "/" + packagePath(clazz) + "/" +
                                                        clazz.getSimpleName() + ".json");
            entityConfig = fromJson(in, EntityConfig.class, FULL_MAPPER_ALLOW_COMMENTS);
        } catch (Exception e) {
            log.debug("getEntityConfig(" + clazz.getName() + "): Exception while reading JSON entity config", e);
            entityConfig = new EntityConfig();
        }

        entityConfig.setClassName(clazz.getName());

        try {
            return entityConfig.updateWithAnnotations(clazz);
        } catch (Exception e) {
            return die("getEntityConfig(" + clazz.getName() + "): Exception while reading entity cfg annotations", e);
        }
    }

    private EntityConfig toEntityConfig(Class<?> clazz) {

        EntityConfig entityConfig = new EntityConfig();
        try {
            entityConfig = getEntityConfig(clazz);
            if (entityConfig == null) return null;

            Class<?> parent = clazz.getSuperclass();
            while (!parent.getName().equals(Object.class.getName())) {
                final EntityConfig parentConfig = getEntityConfig(clazz.getSuperclass());
                if (parentConfig != null) entityConfig.addParent(parentConfig);
                parent = parent.getSuperclass();
            }

            setNames(entityConfig);
        } catch (Exception e) {
            log.warn("toEntityConfig("+clazz.getName()+"): "+e);
        }

        return entityConfig;
    }

    protected void setNames(EntityConfig config) {
        for (Map.Entry<String, EntityFieldConfig> fieldConfig : config.getFields().entrySet()) {
            fieldConfig.getValue().setName(fieldConfig.getKey());
        }

        if (config.hasChildren()) {
            final Map<String, EntityConfig> children = config.getChildren();
            for (Map.Entry<String, EntityConfig> childConfig : children.entrySet()) {
                final EntityConfig child = childConfig.getValue();
                child.setName(childConfig.getKey());
                setNames(child);
            }
        }
    }
}
