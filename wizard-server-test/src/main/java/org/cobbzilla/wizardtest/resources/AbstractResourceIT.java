package org.cobbzilla.wizardtest.resources;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.exceptionmappers.ConstraintViolationBean;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@Slf4j
public abstract class AbstractResourceIT<C extends RestServerConfiguration, S extends RestServer<C>> {

    public static final String EMPTY_JSON = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";

    protected abstract List<ConfigurationSource> getConfigurations();
    protected abstract Class<? extends S> getRestServerClass();

    protected static RestServerHarness<? extends RestServerConfiguration, ? extends RestServer> serverHarness = null;
    protected static RestServer server = null;

    public void beforeServerStart () throws Exception {}
    public boolean shouldCacheServer () { return true; }

    @Before
    public synchronized void startServer() throws Exception {
        if (serverHarness == null || !shouldCacheServer()) {
            serverHarness = new RestServerHarness<>(getRestServerClass());
            serverHarness.setConfigurations(getConfigurations());
            serverHarness.init(getServerEnvironment());
            server = serverHarness.getServer();
            beforeServerStart();
            serverHarness.startServer();
        }
    }

    protected Map<String, String> getServerEnvironment() { return null; }

    protected HttpClient getHttpClient () {
        return new DefaultHttpClient();
    }

    @After
    public void stopServer () throws Exception {
        if (server != null && !shouldCacheServer()) {
            server.stopServer();
            server = null;
        }
    }

    protected Map<String, ConstraintViolationBean> mapViolations(String json) throws Exception {
        Map<String, ConstraintViolationBean> map = new HashMap<>();
        ConstraintViolationBean[] violations = JsonUtil.FULL_MAPPER.readValue(json, ConstraintViolationBean[].class);
        for (ConstraintViolationBean violation : violations) {
            map.put(violation.getMessageTemplate(), violation);
        }
        return map;
    }

    protected void assertExpectedViolations(RestResponse response, String[] violationMessages) throws Exception{
        assertEquals(HttpStatusCodes.UNPROCESSABLE_ENTITY, response.status);
        final Map<String, ConstraintViolationBean> violations = mapViolations(response.json);
        assertEquals(violationMessages.length, violations.size());
        for (String message : violationMessages) {
            assertNotNull(violations.get(message));
        }
    }

    protected RestResponse doGet(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, server.getClientUri());
        @Cleanup("releaseConnection") HttpGet httpGet = new HttpGet(url);
        return getResponse(client, httpGet);
    }

    protected RestResponse doPost(String path, String json) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, server.getClientUri());
        @Cleanup("releaseConnection") HttpPost httpPost = new HttpPost(url);
        if (json != null) {
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPost("+url+") sending JSON=" + json);
        }
        return getResponse(client, httpPost);
    }

    protected RestResponse doPut(String path, String json) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, server.getClientUri());
        @Cleanup("releaseConnection") HttpPut httpPut = new HttpPut(url);
        if (json != null) {
            httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPut sending JSON="+json);
        }
        return getResponse(client, httpPut);
    }

    protected RestResponse doDelete(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, server.getClientUri());
        @Cleanup("releaseConnection") HttpDelete httpDelete = new HttpDelete(url);
        return getResponse(client, httpDelete);
    }

    private String getUrl(String path, String clientUri) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path; // caller has supplied an absolute path

        } else if (path.startsWith("/") && clientUri.endsWith("/")) {
            path = path.substring(1); // caller has supplied a relative path
        }
        return clientUri + path;
    }

    protected RestResponse getResponse(HttpClient client, HttpRequestBase request) throws IOException {
        final HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        final String responseJson;
        final HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (InputStream in = entity.getContent()) {
                responseJson = IOUtils.toString(in);
                log.info("read response callback server: "+responseJson);
            }
        } else {
            responseJson = null;
        }
        return new RestResponse(statusCode, responseJson, getLocationHeader(response));
    }

    public static final String LOCATION_HEADER = "Location";
    private String getLocationHeader(HttpResponse response) {
        final Header header = response.getFirstHeader(LOCATION_HEADER);
        return header == null ? null : header.getValue();
    }

    protected <R extends Identifiable> R getEntity(Class<R> clazz, String endpoint, String uuid) throws Exception {
        return JsonUtil.fromJson(doGet(endpoint+"/"+uuid).json, clazz);
    }

}
