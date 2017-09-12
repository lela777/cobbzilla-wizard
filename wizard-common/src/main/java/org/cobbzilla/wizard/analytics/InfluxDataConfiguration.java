package org.cobbzilla.wizard.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class InfluxDataConfiguration {

    @Getter @Setter private String host;
    @Getter @Setter private String port;
    @Getter @Setter private String databaseName;
    @Getter @Setter private String username;
    @Getter @Setter private String password;

    public String getServerUrl() { return host + ":" + port; }
    public String getWriteUrl() {
        return getServerUrl() + "/write?db=" + databaseName + "&u=" + username + "&p=" + password + "&precision=ms";
    }
}
