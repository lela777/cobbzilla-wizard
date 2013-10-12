package org.cobbzilla.wizardtest.resources;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cobbzilla.restex.RestexClientConnectionManager;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.junit.After;
import org.junit.AfterClass;

public abstract class ApiDocsResourceIT<C extends RestServerConfiguration, S extends RestServer<C>> extends AbstractResourceIT<C, S> {

    protected static TemplateCaptureTarget apiDocs = new TemplateCaptureTarget("target/api-examples");
    @After
    public void commitDocCapture () throws Exception { apiDocs.commit(); }
    @AfterClass
    public static void finalizeDocCapture () throws Exception { apiDocs.close(); }

    @Override public HttpClient getHttpClient() {
        return new DefaultHttpClient(new RestexClientConnectionManager(apiDocs));
    }

}
