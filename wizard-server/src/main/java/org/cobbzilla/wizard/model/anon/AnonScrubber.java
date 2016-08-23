package org.cobbzilla.wizard.model.anon;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.spring.config.rdbms.RdbmsConfig;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.jdbc.ResultSetBean.row2map;

@Accessors(chain=true) @Slf4j
public class AnonScrubber {

    @Getter @Setter private AnonTable[] tables;

    public void anonymize(HasDatabaseConfiguration configuration) {

        final RdbmsConfig config = new RdbmsConfig();
        config.setConfiguration(configuration);
        final HibernatePBEStringEncryptor encryptor = config.hibernateEncryptor();

        try {
            @Cleanup final Connection connection = configuration.getDatabase().getConnection();
            for (AnonTable table : tables) {
                log.info("anonymize: "+table);
                @Cleanup final PreparedStatement s = connection.prepareStatement(table.sqlSelect());
                @Cleanup final ResultSet rs = s.executeQuery();
                final ResultSetMetaData rsMetaData = rs.getMetaData();
                final int numColumns = rsMetaData.getColumnCount();
                while (rs.next()) {
                    final Map<String, Object> row = row2map(rs, rsMetaData, numColumns);
                    @Cleanup final PreparedStatement update = connection.prepareCall(table.sqlUpdate());
                    final AnonColumn[] columns = table.getColumns();
                    for (int i = 0; i<columns.length; i++) {
                        final AnonColumn col = columns[i];
                        final Object value = row.get(col.getName());
                        col.setParam(update, encryptor, i+1, value == null ? null : value.toString());
                    }
                    update.setString(columns.length+1, row.get("uuid").toString());
                    if (update.executeUpdate() != 1) {
                        die("scrub: error updating");
                    }
                }
            }

        } catch (Exception e) {
            die("scrub: error scrubbing: "+e, e);
        }
    }

}
