package org.cobbzilla.wizard.server.config;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.wizard.util.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class RestServerConfiguration {

    @Getter @Setter private Map<String, String> environment = new HashMap<>();

    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private String springContextPath = "classpath:/spring.xml";
    @Getter @Setter private int bcryptRounds = 12;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

    @Getter @Setter private ApplicationContext applicationContext;
    public <T> T autowire (T bean) { return SpringUtil.autowire(applicationContext, bean); }
    public <T> T getBean (Class<T> clazz) { return applicationContext.getBean(clazz); }

    @Getter @Setter private StaticHttpConfiguration staticAssets;
    public boolean hasStaticAssets () { return staticAssets != null && staticAssets.hasAssetRoot(); }

    @Getter @Setter private HttpHandlerConfiguration[] handlers;
    public boolean hasHandlers () { return !empty(handlers); }

    @Getter @Setter private ThriftConfiguration[] thrift;

    public ResultSetBean execSql(String sql, Object[] args) throws SQLException {

        if (!(this instanceof HasDatabaseConfiguration)) die("execSql: "+getClass().getName()+" is not an instance of HasDatabaseConfiguration");

        final boolean isQuery = sql.toLowerCase().trim().startsWith("select");
        @Cleanup Connection conn = ((HasDatabaseConfiguration) this).getDatabase().getConnection();
        @Cleanup PreparedStatement ps = conn.prepareStatement(sql);
        int i = 1;
        for (Object o : args) {
            if (o instanceof String) {
                ps.setString(i++, (String) o);
            } else if (o instanceof Long) {
                ps.setLong(i++, (Long) o);
            } else if (o instanceof Integer) {
                ps.setInt(i++, (Integer) o);
            } else {
                die("unsupported argument type: "+o.getClass().getName());
            }
        }

        if (isQuery) {
            @Cleanup ResultSet rs = ps.executeQuery();
            return new ResultSetBean(rs);
        }

        ps.executeUpdate();
        return ResultSetBean.EMPTY;
    }

}
