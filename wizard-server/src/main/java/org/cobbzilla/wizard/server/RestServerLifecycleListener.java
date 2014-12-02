package org.cobbzilla.wizard.server;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;

public interface RestServerLifecycleListener<C extends RestServerConfiguration> {

    public void beforeStart(RestServer<C> server);
    public void onStart(RestServer<C> server);

    public void beforeStop(RestServer<C> server);
    public void onStop (RestServer<C> server);

}
