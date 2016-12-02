package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class DatabaseConnectionPoolConfiguration {

    @Getter @Setter private int min = 5;
    @Getter @Setter private int max = 100;
    @Getter @Setter private int increment = 5;

    @Getter @Setter private Integer idleTest;
    public boolean hasIdleTest () { return idleTest != null; }

    @Getter @Setter private Integer retryAttempts;
    public boolean hasRetryAttempts() { return retryAttempts != null; }

    @Getter @Setter private Integer retryDelay;
    public boolean hasRetryDelay() { return retryDelay != null; }

}
