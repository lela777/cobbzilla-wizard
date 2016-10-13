package org.cobbzilla.wizard.model.anon;

import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.CaseInsensitiveStringSet;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.spring.config.rdbms.RdbmsConfig;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.jdbc.ResultSetBean.row2map;

@Accessors(chain=true) @Slf4j
public class AnonScrubber {

    @Getter @Setter private AnonTable[] tables;

    public void anonymize(HasDatabaseConfiguration readConfig,
                          HasDatabaseConfiguration writeConfig,
                          boolean ignoreUnknownColumns) {

        final HibernatePBEStringEncryptor decryptor = getCryptor(readConfig);
        final HibernatePBEStringEncryptor encryptor = getCryptor(writeConfig);

        try {
            @Cleanup final Connection connection = readConfig.getDatabase().getConnection();
            for (AnonTable table : tables) {
                log.info("anonymize: "+table);
                if (table.isTruncate()) {
                    @Cleanup final PreparedStatement s = connection.prepareStatement(table.sqlUpdate());
                    s.execute();

                } else {
                    if (ignoreUnknownColumns) {
                        // We need to know which columns to ignore

                        // Find all columns in the DB
                        @Cleanup final PreparedStatement s = connection.prepareStatement("select * from "+table.getTable());
                        s.setMaxRows(1);
                        @Cleanup final ResultSet rs = s.executeQuery();
                        final Set<String> dbColumns = new CaseInsensitiveStringSet(ResultSetBean.getColumns(rs, rs.getMetaData()));
                        if (empty(dbColumns)) die("no columns in table "+table.getTable());

                        // Only keep columns that exist in the DB
                        final Set<String> requestedColumns = new CaseInsensitiveStringSet(table.getColumnNames());
                        requestedColumns.retainAll(dbColumns);
                        table.retainColumns(requestedColumns);
                    }
                    @Cleanup final PreparedStatement s = connection.prepareStatement(table.sqlSelect());
                    @Cleanup final ResultSet rs = s.executeQuery();
                    final ResultSetMetaData rsMetaData = rs.getMetaData();
                    final int numColumns = rsMetaData.getColumnCount();
                    while (rs.next()) {
                        final Map<String, Object> row = row2map(rs, rsMetaData, numColumns);
                        @Cleanup final PreparedStatement update = connection.prepareStatement(table.sqlUpdate());
                        final AnonColumn[] columns = table.getColumns();
                        for (int i = 0; i < columns.length; i++) {
                            final AnonColumn col = columns[i];
                            final Object value = row.get(col.getName());
                            try {
                                col.setParam(update, decryptor, encryptor, i + 1, value == null ? null : value);
                            } catch (Exception e) {
                                final String errColumn = table + "." + col;
                                die("anonymize: error handling table.column: " + errColumn);
                            }
                        }
                        update.setString(columns.length + 1, row.get("uuid").toString());
                        if (update.executeUpdate() != 1) {
                            die("anonymize: error updating");
                        }
                    }
                }
            }

        } catch (Exception e) {
            die("anonymize: error scrubbing: "+e, e);
        }
    }

    public HibernatePBEStringEncryptor getCryptor(HasDatabaseConfiguration readConfig) {
        final RdbmsConfig config = new RdbmsConfig();
        config.setConfiguration(readConfig);
        return config.hibernateEncryptor();
    }

}
