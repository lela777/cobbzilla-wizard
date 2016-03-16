package org.cobbzilla.wizard.server;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.URI;

public interface RestServer<C extends RestServerConfiguration> {

    String ALL_ADDRS = "0.0.0.0";
    String LOCALHOST = "127.0.0.1";

    HttpServer startServer() throws IOException;

    C getConfiguration();
    void setConfiguration(C configuration);

    ConfigurableApplicationContext buildSpringApplicationContext();
    ConfigurableApplicationContext buildSpringApplicationContext(final ApplicationContextConfig ctxConfig);

    void addLifecycleListener (RestServerLifecycleListener<C> listener);
    void removeLifecycleListener (RestServerLifecycleListener<C> listener);

    void stopServer();

    String getClientUri();

    ApplicationContext getApplicationContext();

    URI getBaseUri();

}
