package org.cobbzilla.wizard.server;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;

import java.io.IOException;

public interface RestServer<C extends RestServerConfiguration> {

    public HttpServer startServer() throws IOException;

    public C getConfiguration();
    public void setConfiguration(C configuration);

    public void stopServer();

    public String getClientUri();

}
