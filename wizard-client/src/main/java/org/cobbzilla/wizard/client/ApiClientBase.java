package org.cobbzilla.wizard.client;

import lombok.*;
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
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.util.RestResponse;

import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.io.InputStream;

@Slf4j @NoArgsConstructor
public class ApiClientBase {

    @Getter private ApiConnectionInfo connectionInfo;

    public ApiClientBase (ApiConnectionInfo connectionInfo) { this.connectionInfo = connectionInfo; }
    public ApiClientBase (String baseUri) { connectionInfo = new ApiConnectionInfo(baseUri); }

    public String getBaseUri () { return connectionInfo.getBaseUri(); }

    @Getter @Setter private HttpClient httpClient = new DefaultHttpClient();

    public RestResponse process(HttpRequestBean requestBean) throws Exception {
        switch (requestBean.getMethod()) {
            case HttpMethod.GET:
                return doGet(requestBean.getUri());
            case HttpMethod.POST:
                return doPost(requestBean.getUri(), getJson(requestBean));
            case HttpMethod.PUT:
                return doPut(requestBean.getUri(), getJson(requestBean));
            case HttpMethod.DELETE:
                return doDelete(requestBean.getUri());
            default:
                throw new IllegalArgumentException("Unsupported request method: "+requestBean.getMethod());
        }
    }

    protected void assertStatusOK(RestResponse response) {
        if (response.status != HttpStatusCodes.OK && response.status != HttpStatusCodes.CREATED) throw new ApiException(response);
    }

    private String getJson(HttpRequestBean requestBean) throws Exception {
        Object data = requestBean.getData();
        if (data == null) return null;
        if (data instanceof String) return data.toString();
        return JsonUtil.toJson(data);
    }

    public RestResponse doGet(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpGet httpGet = new HttpGet(url);
        return getResponse(client, httpGet);
    }

    public RestResponse doPost(String path, String json) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPost httpPost = new HttpPost(url);
        if (json != null) {
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPost("+url+") sending JSON=" + json);
        }
        return getResponse(client, httpPost);
    }

    public RestResponse doPut(String path, String json) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPut httpPut = new HttpPut(url);
        if (json != null) {
            httpPut.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            log.info("doPut sending JSON="+json);
        }
        return getResponse(client, httpPut);
    }

    public RestResponse doDelete(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
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

}
