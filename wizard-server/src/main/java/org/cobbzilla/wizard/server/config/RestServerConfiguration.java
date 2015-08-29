package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.util.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class RestServerConfiguration {

    @Getter @Setter private Map<String, String> environment = new HashMap<>();

    @Getter @Setter private String serverName;
    @Getter @Setter private String publicUriBase;
    @Getter @Setter private String springContextPath = "classpath:/spring.xml";
    @Getter @Setter private int bcryptRounds = 12;

    @Getter @Setter private HttpConfiguration http;
    @Getter @Setter private JerseyConfiguration jersey;

    @Getter @Setter private ApplicationContext applicationContext;
    public <T> T autowire (T bean) { SpringUtil.autowire(applicationContext, bean); return bean; }

    @Getter @Setter private StaticHttpConfiguration staticAssets;
    public boolean hasStaticAssets () { return staticAssets != null && staticAssets.hasAssetRoot(); }

    @Getter @Setter private HttpHandlerConfiguration[] handlers;
    public boolean hasHandlers () { return !empty(handlers); }

    @Getter @Setter private ThriftConfiguration[] thrift;

}
