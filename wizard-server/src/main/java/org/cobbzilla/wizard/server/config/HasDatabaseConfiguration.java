package org.cobbzilla.wizard.server.config;

public interface HasDatabaseConfiguration {

    DatabaseConfiguration getDatabase();
    void setDatabase(DatabaseConfiguration config);

}
