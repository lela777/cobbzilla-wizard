package org.cobbzilla.wizard.server;

import org.glassfish.grizzly.http.server.HttpServer;

public interface RestServerLifecycleListener<S> {

    public S beforeStart(S server);

    public void onStart(S server);

    public void onStop (S server);
}
