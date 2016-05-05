package org.cobbzilla.wizardtest.resources;

import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.cobbzilla.restex.RestexClientConnectionManager;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.junit.After;
import org.junit.AfterClass;

public abstract class ApiDocsResourceIT<C extends RestServerConfiguration, S extends RestServer<C>>
        extends AbstractResourceIT<C, S> {

    protected static boolean docsEnabled = true;

    protected static TemplateCaptureTarget apiDocs = new TemplateCaptureTarget("target/api-examples");

    @Getter(lazy=true) private final HttpClient httpClient = initHttpClient();
    protected HttpClient initHttpClient() {
        return docsEnabled ? new RestexClientConnectionManager(apiDocs).getHttpClient() : super.getHttpClient();
    }

    @After public void commitDocCapture () throws Exception { if (docsEnabled) apiDocs.commit(); }

    @AfterClass public static void finalizeDocCapture () throws Exception { if (docsEnabled) apiDocs.close(); }

    public static final ApiDocsApiRunnerListener apiDocsRunnerListener = new ApiDocsApiRunnerListener(apiDocs);

}
