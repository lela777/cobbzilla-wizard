package org.cobbzilla.wizardtest.resources;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.exceptionmappers.ConstraintViolationBean;
import org.cobbzilla.wizard.exceptionmappers.InvalidEntityExceptionMapper;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.junit.AfterClass;
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

    @Before
    public synchronized void startServer() throws Exception {
        if (serverHarness == null) {
            serverHarness = new RestServerHarness<>(getRestServerClass());
            serverHarness.setConfigurations(getConfigurations());
            serverHarness.startServer();
            server = serverHarness.getServer();
        }
    }

    @AfterClass
    public static void stopServer () throws Exception {
//        if (server != null) {
//            server.stopServer();
//            server = null;
//        }
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
        assertEquals(InvalidEntityExceptionMapper.UNPROCESSABLE_ENTITY, response.status);
        final Map<String, ConstraintViolationBean> violations = mapViolations(response.json);
        assertEquals(violationMessages.length, violations.size());
        for (String message : violationMessages) {
            assertNotNull(violations.get(message));
        }
    }

    protected RestResponse doGet(String path) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(server.getClientUri()+path);
        return getResponse(client, httpGet);
    }

    protected RestResponse doPost(String path, String json) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(server.getClientUri()+path);
        if (json != null) {
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPost sending JSON="+json);
        }
        return getResponse(client, httpPost);
    }

    protected RestResponse doPut(String path, String json) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPut httpPut = new HttpPut(server.getClientUri()+path);
        if (json != null) {
            httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPut sending JSON="+json);
        }
        return getResponse(client, httpPut);
    }

    protected RestResponse getResponse(HttpClient client, HttpRequestBase request) throws IOException {
        final HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        String responseJson;
        try (InputStream in = response.getEntity().getContent()) {
            responseJson = IOUtils.toString(in);
            log.info("read response callback server: "+responseJson);
        }
        return new RestResponse(statusCode, responseJson);
    }

    protected <R extends Identifiable> R getEntity(Class<R> clazz, String endpoint, String uuid) throws Exception {
        return JsonUtil.fromJson(doGet(endpoint+"/"+uuid).json, clazz);
    }

}
