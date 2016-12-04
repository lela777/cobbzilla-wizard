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

    // If this returns a non-null value, we'll set the value of "tmpdir" in the QbisConfiguration and in FileUtil
    // If this returns a null value, we'll look for a value in an env var based on the server name (converted to snake case, upper-cased). It will be logged if not found.
    // If nothing else if found, we'll use FileUtil.defaultTempDir
    String getDefaultTmpdirEnvVar();

    ConfigurableApplicationContext buildSpringApplicationContext();
    ConfigurableApplicationContext buildSpringApplicationContext(final ApplicationContextConfig ctxConfig);

    void addLifecycleListener (RestServerLifecycleListener<C> listener);
    void removeLifecycleListener (RestServerLifecycleListener<C> listener);

    void stopServer();

    String getClientUri();

    ApplicationContext getApplicationContext();

    URI getBaseUri();

}
