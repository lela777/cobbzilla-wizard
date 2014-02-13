package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class RestServerConfiguration {

    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private int restServerPort;
    @Getter @Setter private String restServerBaseUri;
    @Getter @Setter private String springContextPath;
    @Getter @Setter private int bcryptRounds;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

    @Getter @Setter private String staticAssetBaseUri;
    @Getter @Setter private String[] staticAssetRoots;
    public boolean hasStaticAssets () { return staticAssetRoots != null && staticAssetRoots.length > 0; }

    @Getter @Setter private ThriftConfiguration[] thrift;

}
