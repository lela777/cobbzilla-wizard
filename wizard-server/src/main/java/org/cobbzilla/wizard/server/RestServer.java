package org.cobbzilla.wizard.server;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public interface RestServer<C extends RestServerConfiguration> {

    public static final String ALL_ADDRS = "0.0.0.0";
    public static final String LOCALHOST = "127.0.0.1";

    public HttpServer startServer() throws IOException;

    public C getConfiguration();
    public void setConfiguration(C configuration);

    public void stopServer();

    public String getClientUri();

    public ApplicationContext getApplicationContext();

}
