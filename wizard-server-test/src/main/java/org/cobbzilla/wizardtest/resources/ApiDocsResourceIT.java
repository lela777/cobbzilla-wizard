package org.cobbzilla.wizardtest.resources;

import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.cobbzilla.restex.RestexClientConnectionManager;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.junit.After;
import org.junit.AfterClass;

public abstract class ApiDocsResourceIT<C extends RestServerConfiguration, S extends RestServer<C>> extends AbstractResourceIT<C, S> {

    protected static TemplateCaptureTarget apiDocs = new TemplateCaptureTarget("target/api-examples");

    @Getter protected HttpClient httpClient = new RestexClientConnectionManager(apiDocs).getHttpClient();

    @After public void commitDocCapture () throws Exception { apiDocs.commit(); }

    @AfterClass public static void finalizeDocCapture () throws Exception { apiDocs.close(); }

}
