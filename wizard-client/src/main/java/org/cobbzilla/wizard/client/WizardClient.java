package org.cobbzilla.wizard.client;

import lombok.AllArgsConstructor;
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
import org.cobbzilla.wizard.util.RestResponse;

import java.io.IOException;
import java.io.InputStream;

@Slf4j @AllArgsConstructor
public class WizardClient {

    private String baseUri;

    public RestResponse doGet(String path) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(baseUri+path);
        return getResponse(client, httpGet);
    }

    public RestResponse doPost(String path, String json) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(baseUri+path);
        if (json != null) {
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPost sending JSON="+json);
        }
        return getResponse(client, httpPost);
    }

    public RestResponse doPut(String path, String json) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPut httpPut = new HttpPut(baseUri+path);
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


}
