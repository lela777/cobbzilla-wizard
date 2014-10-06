package org.cobbzilla.wizard.server;

public interface RestServerLifecycleListener<S extends RestServer> {

    public void beforeStart(S server);
    public void onStart(S server);

    public void beforeStop(S server);
    public void onStop (S server);

}
