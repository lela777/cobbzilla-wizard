package org.cobbzilla.wizard.server.listener;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;

@Slf4j
public class FlywayMigrationListener<C extends RestServerConfiguration> extends RestServerLifecycleListenerBase<C> {

    @Override public void beforeStart(RestServer server) {
        HasDatabaseConfiguration configuration = (HasDatabaseConfiguration) server.getConfiguration();
        migrate(configuration.getDatabase());
        super.beforeStart(server);
    }

    public void migrate(DatabaseConfiguration dbConfig) {
        final Flyway flyway = new Flyway();

        flyway.setDataSource(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword());

        // this is only needed when the database has no flyway-specific schema_version table
        // once we have been running with flyway for a while, remove this line since the schema_version table
        // should always be there, it should be an error if it is not
        flyway.setBaselineOnMigrate(true);

        int applied;
        try {
            applied = flyway.migrate();
        } catch (FlywayException e) {
            if (e.getMessage().contains("Migration checksum mismatch")) {
                log.warn("migrate: checksum mismatch; attempting to repair");
                flyway.repair();

                log.info("migrate: successfully repaired, re-trying migrate...");
                applied = flyway.migrate();

            } else {
                throw e;
            }
        }
        log.info("migrate: successfully applied "+applied+" migrations");
    }

}
