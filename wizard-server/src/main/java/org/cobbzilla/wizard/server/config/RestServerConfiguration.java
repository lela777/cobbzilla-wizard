package org.cobbzilla.wizard.server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.util.SpringUtil;
import org.cobbzilla.wizard.validation.Validator;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.http.URIUtil.getHost;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

@Slf4j
public class RestServerConfiguration {

    @Getter @Setter private Map<String, String> environment = new HashMap<>();

    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private String springContextPath = "classpath:/spring.xml";
    @Getter @Setter private String springShardContextPath = "classpath:/spring-shard.xml";
    @Getter @Setter private int bcryptRounds = 12;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

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
        final boolean isQuery = sql.toLowerCase().trim().startsWith("select");

        @Cleanup Connection conn = config.getDatabase().getConnection();
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

    public String pgCommand(String command) {
        final HasDatabaseConfiguration config = validatePgConfig("pgCommand("+command+")");
        final String dbUser = config.getDatabase().getUser();
        final String dbUrl = config.getDatabase().getUrl();

        // here we assume URL is in the form 'jdbc:{driver}://{host}:{port}/{db_name}'
        final String host = getHost(dbUrl.substring(dbUrl.indexOf(":")+1));
        final String dbName = dbUrl.substring(dbUrl.lastIndexOf('/')+1);

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
}
