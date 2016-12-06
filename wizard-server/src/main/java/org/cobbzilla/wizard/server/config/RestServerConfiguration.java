package org.cobbzilla.wizard.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.util.SpringUtil;
import org.cobbzilla.wizard.validation.Validator;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.URIUtil.getHost;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.system.CommandShell.execScript;

@Slf4j
public class RestServerConfiguration {

    @Getter(lazy=true) private final String id = initId();
    private String initId() { return getServerName() + "_" + RandomStringUtils.randomAlphanumeric(12); }

    @Getter @Setter RestServer server;

    @Getter @Setter private Map<String, String> environment = new HashMap<>();
    @Getter @Setter private File tmpdir = FileUtil.getDefaultTempDir();
    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private String springContextPath = "classpath:/spring.xml";
    @Getter @Setter private String springShardContextPath = "classpath:/spring-shard.xml";
    @Getter @Setter private int bcryptRounds = 12;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

    @Getter @Setter private ErrorApiConfiguration errorApi;
    public boolean hasErrorApi () { return errorApi != null && errorApi.isValid(); }

    @JsonIgnore @Getter @Setter private ApplicationContext applicationContext;

    public <T> T autowire (T bean) { return SpringUtil.autowire(applicationContext, bean); }
    public <T> T getBean (Class<T> clazz) { return SpringUtil.getBean(applicationContext, clazz); }
    public <T> T getBean (String clazz) { return (T) SpringUtil.getBean(applicationContext, forName(clazz)); }

    @Getter @Setter private StaticHttpConfiguration staticAssets;
    public boolean hasStaticAssets () { return staticAssets != null && staticAssets.hasAssetRoot(); }

    @Getter @Setter private HttpHandlerConfiguration[] handlers;
    public boolean hasHandlers () { return !empty(handlers); }

    @Getter @Setter private WebappConfiguration[] webapps;
    public boolean hasWebapps () { return !empty(webapps); }

    @JsonIgnore @Getter @Setter private Validator validator;

    @Getter @Setter private ThriftConfiguration[] thrift;

    public String getApiUriBase() { return getPublicUriBase() + getHttp().getBaseUri(); }

    public String getLoopbackApiBase() { return "http://127.0.0.1:" + getHttp().getPort() + getHttp().getBaseUri(); }

    public ResultSetBean execSql(String sql, Object[] args) throws SQLException {

        final HasDatabaseConfiguration config = validatePgConfig("execSql");

        @Cleanup Connection conn = config.getDatabase().getConnection();
        return execSql(conn, sql, args);
    }

    public ResultSetBean execSql(Connection conn, String sql, Object[] args) throws SQLException {
        @Cleanup PreparedStatement ps = conn.prepareStatement(sql);
        if (args != null) {
            int i = 1;
            for (Object o : args) {
                if (o == null) {
                    die("null arguments not supported. null value at parameter index=" + i + ", sql=" + sql);
                }
                if (o instanceof String) {
                    ps.setString(i++, (String) o);
                } else if (o instanceof Long) {
                    ps.setLong(i++, (Long) o);
                } else if (o instanceof Integer) {
                    ps.setInt(i++, (Integer) o);
                } else {
                    die("unsupported argument type: " + o.getClass().getName());
                }
            }
        }

        final boolean isQuery = sql.toLowerCase().trim().startsWith("select");
        if (isQuery) {
            @Cleanup ResultSet rs = ps.executeQuery();
            log.info("execSql (query): "+sql);
            return new ResultSetBean(rs);
        }

        ps.executeUpdate();
        log.info("execSql (update): "+sql);
        return ResultSetBean.EMPTY;
    }

    public String pgCommand() { return pgCommand("psql"); }

    public String pgCommand(String command)            { return pgCommand(command, null, null); }
    public String pgCommand(String command, String db) { return pgCommand(command, db, null); }

    public String pgCommand(String command, String db, String user) {
        final HasDatabaseConfiguration config = validatePgConfig("pgCommand("+command+")");
        final String dbUser = !empty(user) ? user : config.getDatabase().getUser();
        final String dbUrl = config.getDatabase().getUrl();

        // here we assume URL is in the form 'jdbc:{driver}://{host}:{port}/{db_name}'
        final String host = getHost(dbUrl.substring(dbUrl.indexOf(":")+1));
        final String dbName = !empty(db) ? db : dbUrl.substring(dbUrl.lastIndexOf('/')+1);

        return command + " -h " + host +" -U " + dbUser + " " + dbName;
    }

    public Map<String, String> pgEnv() {
        final HasDatabaseConfiguration config = validatePgConfig("pgEnv");
        String dbPass = config.getDatabase().getPassword();
        if (empty(dbPass)) dbPass = "";

        final Map<String, String> env = new HashMap<>();
        env.putAll(config.getEnvironment());
        env.put("PGPASSWORD", dbPass);
        String path = env.get("PATH");
        if (path == null) {
            path = "/bin:/usr/bin:/usr/local/bin";
        } else {
            path += ":/usr/local/bin";
        }
        env.put("PATH", path);

        return env;
    }

    private HasDatabaseConfiguration validatePgConfig(String method) {
        if (!(this instanceof HasDatabaseConfiguration)) die(method+": "+getClass().getName()+" is not an instance of HasDatabaseConfiguration");
        return (HasDatabaseConfiguration) this;
    }

    public void execSqlCommands(String sqlCommands) {
        for (String sql : StringUtil.split(sqlCommands, ";")) {
            try {
                execSql(sql, StringUtil.EMPTY_ARRAY);
            } catch (Exception e) {
                log.warn("onStart: "+e);
            }
        }
    }

    /**
     * Allows forever-reuse of subresources, each instantiated with a particular set of immutable objects.
     * Override to supply your own cache implementations on a per-resource-class basis.
     * @param <R> the type of resource we are, and that will be cached, so method calls can be typesafe.
     */
    private final Map<String, Map<String, Object>> subResourceCaches = new ConcurrentHashMap<>();
    public <R> Map<String, R> getSubResourceCache(Class<R> resourceClass) {
        Map cache = subResourceCaches.get(resourceClass.getName());
        if (cache == null) {
            synchronized (subResourceCaches) {
                cache = subResourceCaches.get(resourceClass.getName());
                if (cache == null) {
                    cache = new ConcurrentHashMap<>();
                    subResourceCaches.put(resourceClass.getName(), cache);
                }
            }
        }
        return cache;
    }

    public <R> R subResource(Class<R> resourceClass, Object... args) {
        final StringBuilder cacheKey = new StringBuilder(resourceClass.getName()).append(":").append(getId());
        for (Object o : args) {
            if (o == null) {
                log.warn("forContext("+ ArrayUtils.toString(args)+"): null arg");
                continue;
            }
            if (o instanceof Identifiable) {
                cacheKey.append(":").append(o.getClass().getName()).append("(").append(((Identifiable) o).getUuid()).append(")");
            } else if (!(o instanceof DAO)) {
                log.warn("forContext("+ArrayUtils.toString(args)+"): expected Identifiable or DAO, found "+o.getClass().getName()+": "+o);
            }
        }
        final Map<String, R> resourceCache = getSubResourceCache(resourceClass);
        R r = resourceCache.get(cacheKey.toString());
        if (r == null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (resourceCache) {
                r = resourceCache.get(cacheKey.toString());
                if (r == null) {
                    r = autowire(instantiate(resourceClass, args));
                    resourceCache.put(cacheKey.toString(), r);
                }
            }
        }
        return r;
    }

    public void copyDatabase(String targetDbName) { copyDatabase(targetDbName, null); }

    public void copyDatabase(String targetDbName, String user) {
        final String dbUser = !empty(user) ? user : "postgres";
        String copyCommand = null;
        try {
            copyCommand = pgCommand("dropdb", null, dbUser) + " ; " + pgCommand("createdb", null, dbUser)
                    + " && " + pgCommand("pg_dump", null, dbUser)
                    + " | " + pgCommand("psql", user, targetDbName);
            execScript(copyCommand, pgEnv());
        } catch (Exception e) {
            die("copyDatabase("+copyCommand+"): "+e);
        }
    }
}
