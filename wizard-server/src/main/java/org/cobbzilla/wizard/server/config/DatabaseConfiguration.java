package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfiguration {

    @Getter @Setter private String driver;
    @Getter @Setter private String url;
    @Getter @Setter private String user;
    @Getter @Setter private String password;

    @Getter @Setter private boolean encryptionEnabled = false;
    @Getter @Setter private String encryptionKey;
    @Getter @Setter private int encryptorPoolSize = 5;

    @Getter @Setter private HibernateConfiguration hibernate;

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
