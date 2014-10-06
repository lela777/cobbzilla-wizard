package org.cobbzilla.wizard.server;

public interface RestServerLifecycleListener {

    public void beforeStart();
    public void onStart();

    public void beforeStop();
    public void onStop ();

}
