package org.cobbzilla.wizard.server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.RestServerConfigurationFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class RestServerHarness<C extends RestServerConfiguration, S extends RestServer<C>> {

    @Getter @Setter private Class<S> restServerClass;
    @Getter @Setter private List<ConfigurationSource> configurations = new ArrayList<>();
    @Getter private S server = null;

    public RestServerHarness(Class<S> restServerClass) {
        this.restServerClass = restServerClass;
    }

    public void addConfiguration(ConfigurationSource source) { configurations.add(source); }

    public synchronized void startServer() throws Exception {
        startServer(null);
    }

    public synchronized void startServer(Map<String, String> env) throws Exception {
        if (server == null) {
            server = getRestServerClass().newInstance();

            final Class<C> configurationClass = ReflectionUtil.getTypeParameter(getRestServerClass(), RestServerConfiguration.class);
            final RestServerConfigurationFactory<C> factory = new RestServerConfigurationFactory<>(configurationClass);
            final C configuration = factory.build(configurations, env);

            server.setConfiguration(configuration);
            log.info("starting "+configuration.getServerName()+": "+server.getClass().getName()+" with config: "+configuration);
            server.startServer();
        }
    }

    public synchronized void stopServer () throws Exception {
        if (server != null) {
            server.stopServer();
            server = null;
        }
    }
}
