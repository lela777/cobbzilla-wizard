package org.cobbzilla.wizard.server;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;

public interface RestServerLifecycleListener<S extends RestServer<C>, C extends RestServerConfiguration> {

    public void beforeStart(S server);
    public void onStart(S server);

    public void beforeStop(S server);
    public void onStop (S server);

}
