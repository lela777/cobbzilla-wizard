package org.cobbzilla.wizard.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.jdbc.DbDumpMode;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.jdbc.UncheckedSqlException;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.system.Command;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.wizard.analytics.AnalyticsConfiguration;
import org.cobbzilla.wizard.analytics.AnalyticsHandler;
import org.cobbzilla.wizard.asset.AssetStorageConfiguration;
import org.cobbzilla.wizard.asset.AssetStorageService;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.filters.ApiRateLimit;
import org.cobbzilla.wizard.log.LogRelayAppenderConfig;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.resources.ParentResource;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.util.SpringUtil;
import org.cobbzilla.wizard.validation.Validator;
import org.springframework.context.ApplicationContext;

import javax.persistence.Transient;
import java.io.File;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.collection.ArrayUtil.EMPTY_OBJECT_ARRAY;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.URIUtil.getHost;
import static org.cobbzilla.util.http.URIUtil.getPort;
import static org.cobbzilla.util.io.FileUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.system.CommandShell.exec;
import static org.cobbzilla.util.system.CommandShell.execScript;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public class RestServerConfiguration {

    public static final int MAX_DUMP_TRIES = 5;
    @Getter(lazy=true) private final String id = initId();
    private String initId() { return getServerName() + "_" + RandomStringUtils.randomAlphanumeric(12); }

    @Getter @Setter RestServer server;
    public boolean isRunning() { return getServer() != null && getServer().isRunning(); }

    @Getter @Setter private Map<String, String> environment = new HashMap<>();
    @Getter @Setter private File tmpdir = FileUtil.getDefaultTempDir();
    @Getter @Setter private String serverName;

    @Setter private String publicUriBase;
    public String getPublicUriBase () { return !empty(publicUriBase) && publicUriBase.endsWith("/") ? publicUriBase.substring(0, publicUriBase.length()-1) : publicUriBase; }

    @Getter @Setter private String springContextPath = "classpath:/spring.xml";
    @Getter @Setter private String springShardContextPath = "classpath:/spring-shard.xml";
    @Getter @Setter private int bcryptRounds = 12;
    @Getter @Setter private boolean testMode = false;
    @Getter @Setter private LogRelayAppenderConfig logRelay;

    private String appendPathToUriBase(String base, String... pathParts) {
        final StringBuilder b = new StringBuilder(base.endsWith("/") ? base.substring(0, base.length()-1) : base);
        for (String path : pathParts) {
            if (!path.startsWith("/")) b.append("/");
            b.append(path.endsWith("/") ? path.substring(0, path.length()-1) : path);
        }
        return b.toString();
    }

    public String uri(String path) { return appendPathToUriBase(getPublicUriBase(), path); }
    public String api(String path) { return appendPathToUriBase(getApiUriBase(), path); }

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

    @Getter @Setter private ErrorApiConfiguration errorApi;
    public boolean hasErrorApi () { return errorApi != null && errorApi.isValid(); }

    @JsonIgnore @Getter @Setter private ApplicationContext applicationContext;

    public <T> T autowire (T bean) { return SpringUtil.autowire(applicationContext, bean); }
    public <T> T getBean (Class<T> clazz) { return SpringUtil.getBean(applicationContext, clazz); }
    public <T> T getBean (String clazz) { return (T) SpringUtil.getBean(applicationContext, forName(clazz)); }
    public <T> Map<String, T> getBeans (Class<T> clazz) { return SpringUtil.getBeans(applicationContext, clazz); }

    @Getter @Setter private StaticHttpConfiguration staticAssets;
    public boolean hasStaticAssets () { return staticAssets != null && staticAssets.hasAssetRoot(); }

    @Getter @Setter private HttpHandlerConfiguration[] handlers;
    public boolean hasHandlers () { return !empty(handlers); }

    @Getter @Setter private WebappConfiguration[] webapps;
    public boolean hasWebapps () { return !empty(webapps); }

    @JsonIgnore @Getter @Setter private Validator validator;

    @Getter @Setter private AssetStorageConfiguration assetStorage;
    @Getter(lazy=true) private final AssetStorageService assetStorageService = initStorageService();
    public AssetStorageService initStorageService () { return AssetStorageService.build(assetStorage); }

    @Getter @Setter private ThriftConfiguration[] thrift;

    @Getter @Setter private ApiRateLimit[] rateLimits;
    public boolean hasRateLimits () { return !empty(rateLimits); }

    @Getter @Setter private AnalyticsConfiguration analytics;

    @Getter(lazy=true) private final AnalyticsHandler analyticsHandler = initAnalyticsHandler();
    private AnalyticsHandler initAnalyticsHandler() {
        if (analytics == null || !analytics.valid()) return null;
        final AnalyticsHandler handler = instantiate(analytics.getHandler());
        handler.init(analytics);
        return handler;
    }

    public String getApiUriBase() { return getPublicUriBase() + getHttp().getBaseUri(); }

    public String getLoopbackApiBase() { return "http://127.0.0.1:" + getHttp().getPort() + getHttp().getBaseUri(); }

    public ResultSetBean execSql(String sql) { return execSql(sql, EMPTY_OBJECT_ARRAY); }
    public ResultSetBean execSql(String sql, Object[] args) {

        final HasDatabaseConfiguration config = validatePgConfig("execSql");

        try {
            @Cleanup Connection conn = config.getDatabase().getConnection();
            return execSql(conn, sql, args);

        } catch (SQLException e) {
            throw new UncheckedSqlException(e);

        } catch (UncheckedSqlException e) {
            throw e;

        } catch (Exception e) {
            return die("Exception: "+e, e);
        }
    }

    @Transient @JsonIgnore @Getter @Setter private Boolean execSqlStrictStrings = null;

    public ResultSetBean execSql(Connection conn, String sql, Object[] args) {
        try {
            @Cleanup PreparedStatement ps = conn.prepareStatement(sql);
            if (args != null) {
                int i = 1;
                for (Object o : args) {
                    if (o == null) {
                        die("null arguments not supported. null value at parameter index=" + i + ", sql=" + sql);
                    }
                    if (o instanceof String) {
                        if (execSqlStrictStrings == null || execSqlStrictStrings == false) {
                            if (o.toString().equalsIgnoreCase(Boolean.TRUE.toString())) {
                                ps.setBoolean(i++, true);
                            } else if (o.toString().equalsIgnoreCase(Boolean.FALSE.toString())) {
                                ps.setBoolean(i++, false);
                            } else {
                                ps.setString(i++, (String) o);
                            }
                        } else {
                            ps.setString(i++, (String) o);
                        }
                    } else if (o instanceof Long) {
                        ps.setLong(i++, (Long) o);
                    } else if (o instanceof Integer) {
                        ps.setInt(i++, (Integer) o);
                    } else if (o instanceof Boolean) {
                        ps.setBoolean(i++, (Boolean) o);
                    } else if (o instanceof Object[]) {
                        Array arrayParam = conn.createArrayOf("varchar", (Object[]) o);
                        ps.setArray(i++, arrayParam);

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

        } catch (SQLException e) {
            throw new UncheckedSqlException(e);

        } catch (Exception e) {
            return die("Exception: "+e, e);
        }
    }

    public int rowCount(String table) throws SQLException {
        return execSql("select count(*) from " + table).count();
    }

    public int rowCountOrZero(String table) {
        try { return rowCount(table); } catch (Exception e) {
            log.warn("rowCountOrZero (returning 0): "+e);
            return 0;
        }
    }

    @Getter @Setter private String pgServerDir;

    public String pgCommand() { return pgCommand("psql"); }
    public String pgOptions() { return pgCommand(""); }

    public String pgCommand(String command)            { return pgCommand(command, null, null); }
    public String pgCommand(String command, String db) { return pgCommand(command, db, null); }

    public String pgCommand(String command, String db, String user) {
        final HasDatabaseConfiguration config = validatePgConfig("pgCommand("+command+")");
        final String dbUser = !empty(user) ? user : config.getDatabase().getUser();
        final String dbUrl = config.getDatabase().getUrl();

        // here we assume URL is in the form 'jdbc:{driver}://{host}:{port}/{db_name}'
        final int colonPos = dbUrl.indexOf(":");
        final String host = getHost(dbUrl.substring(colonPos+1));
        final int port = getPort(dbUrl.substring(colonPos+1));
        final int qPos = dbUrl.indexOf("?");
        final String dbName = !empty(db) ? db : qPos == -1 ? dbUrl.substring(dbUrl.lastIndexOf('/')+1) : dbUrl.substring(dbUrl.lastIndexOf("/")+1, qPos);

        final String options = " -h " + host + " -p " + port +  " -U " + dbUser + " " + dbName;

        if (empty(command)) return options;

        final String pgServerDir = getPgServerDir();
        if (!empty(pgServerDir)) command = abs(new File(pgServerDir + File.separator + "bin" + File.separator + command));

        return command + options;
    }

    public CommandLine pgCommandLine(String command) {
        if (empty(command)) return die("pgCommandLine: no command provided");
        final String pgServerDir = getPgServerDir();
        if (!empty(pgServerDir)) command = abs(new File(pgServerDir + File.separator + "bin" + File.separator + command));
        return new CommandLine(command).addArguments(pgOptions());
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

    private static final Pattern DROP_PATTERN = Pattern.compile("^drop ", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    public void execSqlCommands(String sqlCommands) {
        for (String sql : StringUtil.split(sqlCommands, ";")) {
            try {
                execSql(sql);
            } catch (Exception e) {
                if (DROP_PATTERN.matcher(sql).find()) {
                    log.info("execSqlCommands ("+sql+"): " + e);
                } else {
                    log.warn("execSqlCommands ("+sql+"): " + e);
                }
            }
        }
    }

    /**
     * Allows forever-reuse of subresources, each instantiated with a particular set of immutable objects.
     * @param <R> the type of resource to return, so method calls can be typesafe.
     */
    private final Map<String, Map<String, Object>> subResourceCaches = new ConcurrentHashMap<>();
    public <R> Map<String, R> getSubResourceCache(Class<R> resourceClass) {
        Map cache = subResourceCaches.get(resourceClass.getName());
        if (cache == null) {
            synchronized (subResourceCaches) {
                cache = subResourceCaches.get(resourceClass.getName());
                if (cache == null) {
                    cache = new ConcurrentHashMap();
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
            } else if (o instanceof String) {
                cacheKey.append(":").append(o.getClass().getName()).append("(").append(o).append(")");
            } else if (o instanceof Number) {
                cacheKey.append(":").append(o.getClass().getName()).append("(").append(o).append(")");
            } else if (o instanceof ParentResource) {
                cacheKey.append(":").append(o.getClass().getName()).append("(").append(o).append(")");
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
                    try {
                        r = autowire(instantiate(resourceClass, args));
                    } catch (Exception e) {
                        return die("subResource: "+e, e);
                    }
                    resourceCache.put(cacheKey.toString(), r);
                }
            }
        }
        return r;
    }

    private final Map<String, DAO<? extends Identifiable>> daoCache = new ConcurrentHashMap<>();

    @Getter(lazy=true) private final Collection<DAO> allDAOs = initAllDAOs();
    private Collection<DAO> initAllDAOs() { return getBeans(DAO.class).values(); }

    public DAO getDaoForEntityClass(Class entityClass) {
        final String name = entityClass.getName();
        return daoCache.computeIfAbsent(name, k -> getBean(name.replace(".model.", ".dao.") + "DAO"));
    }

    public DAO getDaoForEntityClass(String className) {
        return daoCache.computeIfAbsent(className, k -> {
            for (DAO dao : getAllDAOs()) {
                if ( dao.getEntityClass().getSimpleName().equalsIgnoreCase(className) ||
                     dao.getEntityClass().getName().equalsIgnoreCase(className) ) {
                    return dao;
                }
            }
            return die("getDaoForEntityClass("+className+"): DAO not found");
        });
    }

    public File pgDump() { return pgDump(temp("pgDump-out", ".sql")); }

    public File pgDump(File file) { return pgDump(file, null); }

    public File pgDump(File file, DbDumpMode dumpMode) {
        final File temp = temp("pgRestore-out", ".sql");
        final String dumpOptions;
        if (dumpMode == null) dumpMode = DbDumpMode.all;
        switch (dumpMode) {
            case all: dumpOptions = ""; break;
            case schema: dumpOptions = "--schema-only"; break;
            case data: dumpOptions = "--data-only"; break;
            default: return die("pgDump: invalid dumpMode: "+dumpMode);
        }
        for (int i=0; i<MAX_DUMP_TRIES; i++) {
            final String output;
            try {
                output = execScript(pgCommand("pg_dump " + dumpOptions) + " > " + abs(temp) + " || exit 1", pgEnv());
                if (output.contains("ERROR")) die("pgDump: error dumping DB:\n" + output);
                if (!temp.renameTo(file)) {
                    log.warn("pgDump: error renaming file, trying copy");
                    copyFile(temp, file);
                    if (!temp.delete()) log.warn("pgDump: error deleting temp file: " + abs(temp));
                }
                log.info("pgDump: dumped DB to snapshot: " + abs(file));
                return file;
            } catch (Exception e) {
                log.warn("pgDump: error occurred dumping to "+abs(file)+", "+(i<(MAX_DUMP_TRIES-1)?"will retry":"will NOT retry")+": "+e, e);
                sleep(SECONDS.toMillis(5));
            }
        }
        return die("pgDump: too many errors trying to dump DB to "+abs(file)+", bailing out");
    }

    public void pgRestore(File file) {
        for (int i=0; i<MAX_DUMP_TRIES; i++) {
            try {
                final CommandResult result = exec(new Command(pgCommandLine("psql"))
                        .setInput(FileUtil.toString(file))
                        .setEnv(pgEnv()));
                //if (result.getStderr().contains("ERROR")) die("pgRestore: error restoring DB:\n"+result.getStderr());
                log.info("pgRestore: restored DB from snapshot: " + abs(file));
                return;

            } catch (Exception e) {
                log.warn("pgRestore: error occurred restoring from "+abs(file)+", "+(i<(MAX_DUMP_TRIES-1)?"will retry":"will NOT retry")+": "+e, e);
                sleep(SECONDS.toMillis(5));
            }
        }
        die("pgRestore: too many errors trying to restore DB from "+abs(file)+", bailing out");
    }

}
