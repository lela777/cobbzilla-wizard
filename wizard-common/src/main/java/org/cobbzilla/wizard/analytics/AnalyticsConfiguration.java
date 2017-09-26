package org.cobbzilla.wizard.analytics;

import lombok.Getter;
import lombok.Setter;

public class AnalyticsConfiguration {

    @Getter @Setter private String handler;

    @Getter @Setter private String host;
    @Setter private Integer port;
    public Integer getPort () { return port == null ? 443 : port; }

    @Getter @Setter private String username;
    @Getter @Setter private String password;
    @Getter @Setter private String env;

}
