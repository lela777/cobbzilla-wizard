package org.cobbzilla.wizard.server.listener;

import lombok.Cleanup;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class FlywayShardMigrationListener<C extends RestServerConfiguration> extends FlywayMigrationListener<C> {

    public static final String DEFAULT_SHARD_TABLE_NAME = "shard";

    protected String getShardTableName() { return DEFAULT_SHARD_TABLE_NAME; }

    @Override public void migrate(DatabaseConfiguration dbConfig) {
        // migrate master
        super.migrate(dbConfig);

        // find and migrate shards
        try {
            @Cleanup final Connection conn = dbConfig.getConnection();
            @Cleanup final PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT url FROM " + getShardTableName());
            @Cleanup final ResultSet rs = ps.executeQuery();
            final ResultSetBean results = new ResultSetBean(rs);
            for (Map<String, Object> row : results.getRows()) {
                final String jdbcUrl = (String) row.get("url");
                if (jdbcUrl != null && !jdbcUrl.equals(dbConfig.getUrl())) {
                    final DatabaseConfiguration shardDbConfig = new DatabaseConfiguration(dbConfig);
                    shardDbConfig.setUrl(jdbcUrl);
                    super.migrate(shardDbConfig);
                }
            }
        } catch (SQLException e) {
            die("migrate: "+e, e);
        }
    }

}
