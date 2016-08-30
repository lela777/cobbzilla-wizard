package org.cobbzilla.wizard.server.listener;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.ErrorApi;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

public class ErrbitConfigListener extends RestServerLifecycleListenerBase {

    @Override public void onStart(RestServer server) {
        RestServerBase.setErrorApi(new ErrbitApi(server.getConfiguration()));
    }

    @AllArgsConstructor @Slf4j
    static class ErrbitApi implements ErrorApi {

        private final RestServerConfiguration config;

        @Override public void report(Exception e) {
            if (config.hasErrorApi()) {
                config.getErrorApi().report(e);
            } else {
                log.warn("report: could not send exception to error reporting API: "+e, e);
            }
        }

    }

}
