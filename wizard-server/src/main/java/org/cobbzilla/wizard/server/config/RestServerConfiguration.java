package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

public class RestServerConfiguration {

    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private String springContextPath;
    @Getter @Setter private int bcryptRounds;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;
    @Getter @Setter private ApplicationContext applicationContext;

    @Getter @Setter private StaticHttpConfiguration staticAssets;
    public boolean hasStaticAssets () { return staticAssets != null && staticAssets.hasAssetRoot(); }

    @Getter @Setter private ThriftConfiguration[] thrift;

}
