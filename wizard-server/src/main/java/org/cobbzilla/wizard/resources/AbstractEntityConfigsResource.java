package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.wizard.model.entityconfig.EntityConfig;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStream;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.string.StringUtil.packagePath;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Slf4j
public abstract class AbstractEntityConfigsResource {

    public static final String ENTITY_CONFIG_BASE = "entity-config";

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private HasDatabaseConfiguration configuration;

    protected long getConfigRefreshInterval() { return TimeUnit.DAYS.toMillis(30); }
    protected abstract boolean authorized(HttpContext ctx);

    @Getter(AccessLevel.PROTECTED) private final AutoRefreshingReference<Map<String, EntityConfig>> configs = new EntityConfigsMap();

    protected EntityConfig getEntityConfig(Class<?> clazz) throws Exception {
        final InputStream in;
        try {
            in = loadResourceAsStream(ENTITY_CONFIG_BASE + "/" + packagePath(clazz) + "/" + clazz.getSimpleName() + ".json");
        } catch (Exception e) {
            log.warn("getEntityConfig("+clazz.getName()+"): "+e);
            return null;
        }
        try {
            return fromJson(in, EntityConfig.class);
        } catch (Exception e) {
            return die("getEntityConfig: "+e, e);
        }
    }

    @GET
    @Path("/{name}")
    public Response getConfig (@Context HttpContext ctx,
                               @PathParam("name") String name,
                               @QueryParam("refresh") boolean refresh) {

        if (!authorized(ctx)) return forbidden();

        if (refresh) {
            log.info("getConfig: refreshing");
            configs.set(null);
        }

        final EntityConfig config;
        synchronized (configs) {
            config = configs.get().get(capitalize(name));
        }

        return config == null ? notFound(name) : ok(config);
    }

    public class EntityConfigsMap extends AutoRefreshingReference<Map<String, EntityConfig>> {

        @Override public Map<String, EntityConfig> refresh() {
            final Map<String, EntityConfig> configMap = new HashMap<>();
            final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            scanner.addIncludeFilter(new AnnotationTypeFilter(Embeddable.class));
            for (String pkg : configuration.getDatabase().getHibernate().getEntityPackages()) {
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
                        log.warn("No config found for "+clazz);
                    }
                }
            }
            synchronized (configs) {
                configs.set(configMap);
            }
            return configs.get();
        }

        // todo: default information can come from parsing the javax.persistence and javax.validation annotations
        private EntityConfig toEntityConfig(Class<?> clazz) {

            final EntityConfig entityConfig;
            try {
                entityConfig = getEntityConfig(clazz);
                if (entityConfig == null) return null;

                Class<?> parent = clazz.getSuperclass();
                while (!parent.getName().equals(Object.class.getName())) {
                    EntityConfig parentConfig = getEntityConfig(clazz.getSuperclass());
                    if (parentConfig != null) entityConfig.addParent(parentConfig);
                    parent = parent.getSuperclass();
                }

                setNames(entityConfig);

                return entityConfig;

            } catch (Exception e) {
                log.warn("toEntityConfig("+clazz.getName()+"): "+e);
                return null;
            }
        }

        private void setNames(EntityConfig config) {
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

        @Override public long getTimeout() { return getConfigRefreshInterval(); }
    }
}
