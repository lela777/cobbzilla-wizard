package org.cobbzilla.wizard.client;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import org.cobbzilla.util.http.ApiConnectionInfo;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.util.RestResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

@Slf4j @NoArgsConstructor
public class ApiClientBase {

    public static final ContentType CONTENT_TYPE_JSON = ContentType.APPLICATION_JSON;

    @Getter protected ApiConnectionInfo connectionInfo;
    @Getter @Setter protected String token;

    public ApiClientBase (ApiConnectionInfo connectionInfo) { this.connectionInfo = connectionInfo; }
    public ApiClientBase (String baseUri) { connectionInfo = new ApiConnectionInfo(baseUri); }

    public ApiClientBase (ApiConnectionInfo connectionInfo, HttpClient httpClient) {
        this(connectionInfo);
        setHttpClient(httpClient);
    }

    public ApiClientBase (String baseUri, HttpClient httpClient) {
        this(baseUri);
        setHttpClient(httpClient);
    }

    public String getBaseUri () { return connectionInfo.getBaseUri(); }

    @Getter @Setter private HttpClient httpClient = new DefaultHttpClient();

    public RestResponse process(HttpRequestBean requestBean) throws Exception {
        switch (requestBean.getMethod()) {
            case HttpMethods.GET:
                return doGet(requestBean.getUri());
            case HttpMethods.POST:
                return doPost(requestBean.getUri(), getJson(requestBean));
            case HttpMethods.PUT:
                return doPut(requestBean.getUri(), getJson(requestBean));
            case HttpMethods.DELETE:
                return doDelete(requestBean.getUri());
            default:
                throw new IllegalArgumentException("Unsupported request method: "+requestBean.getMethod());
        }
    }

    protected void assertStatusOK(RestResponse response) {
        if (response.status != HttpStatusCodes.OK
                && response.status != HttpStatusCodes.CREATED
                && response.status != HttpStatusCodes.NO_CONTENT) throw new ApiException(response);
    }

    private String getJson(HttpRequestBean requestBean) throws Exception {
        Object data = requestBean.getData();
        if (data == null) return null;
        if (data instanceof String) return data.toString();
        return JsonUtil.toJson(data);
    }

    protected ApiException specializeApiException(ApiException e) {
        return specializeApiException(e.getResponse());
    }

    protected ApiException specializeApiException(RestResponse response) {
        if (response.isSuccess()) {
            throw new IllegalArgumentException("specializeApiException: cannot specialize exception for a successful response: "+response);
        }
        switch (response.status) {
            case HttpStatusCodes.NOT_FOUND:
                return new NotFoundException(response);
            case HttpStatusCodes.FORBIDDEN:
                return new ForbiddenException(response);
            case HttpStatusCodes.UNPROCESSABLE_ENTITY:
                return new ValidationException(response);
            default: return new ApiException(response);
        }
    }

    public RestResponse doGet(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpGet httpGet = new HttpGet(url);
        return getResponse(client, httpGet);
    }

    public RestResponse get(String path) throws Exception {
        final RestResponse restResponse = doGet(path);
        if (!restResponse.isSuccess()) throw specializeApiException(restResponse);
        return restResponse;
    }

    public RestResponse doPost(String path, String json) throws Exception {
        return doPost(path, json, CONTENT_TYPE_JSON);
    }

    public RestResponse doPost(String path, String data, ContentType contentType) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPost httpPost = new HttpPost(url);
        if (data != null) {
            httpPost.setEntity(new StringEntity(data, contentType));
            log.info("doPost("+url+") sending JSON=" + data);
        }
        return getResponse(client, httpPost);
    }

    public RestResponse post(String path, String json) throws Exception {
        return post(path, json, CONTENT_TYPE_JSON);
    }

    public RestResponse post(String path, String data, ContentType contentType) throws Exception {
        final RestResponse restResponse = doPost(path, data, contentType);
        if (!restResponse.isSuccess()) throw specializeApiException(restResponse);
        return restResponse;
    }

    public RestResponse doPut(String path, String json) throws Exception {
        return doPut(path, json, CONTENT_TYPE_JSON);
    }

    public RestResponse doPut(String path, String data, ContentType contentType) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPut httpPut = new HttpPut(url);
        if (data != null) {
            httpPut.setEntity(new StringEntity(data, contentType));
            log.info("doPut sending JSON="+data);
        }
        return getResponse(client, httpPut);
    }

    public RestResponse put(String path, String json) throws Exception {
        final RestResponse restResponse = doPut(path, json);
        if (!restResponse.isSuccess()) throw specializeApiException(restResponse);
        return restResponse;
    }

    public RestResponse doDelete(String path) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpDelete httpDelete = new HttpDelete(url);
        return getResponse(client, httpDelete);
    }

    public RestResponse delete(String path) throws Exception {
        final RestResponse restResponse = doDelete(path);
        if (!restResponse.isSuccess()) throw specializeApiException(restResponse);
        return restResponse;
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
        request = beforeSend(request);
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

    protected HttpRequestBase beforeSend(HttpRequestBase request) {
        if (!StringUtil.empty(token)) {
            final String tokenHeader = getTokenHeader();
            if (StringUtil.empty(tokenHeader)) throw new IllegalArgumentException("token set but getTokenHeader returned null");
            request.setHeader(tokenHeader, token);
        }
        return request;
    }

    protected String getTokenHeader() { return null; }

    protected final Stack<String> tokenStack = new Stack<>();
    public void pushToken(String token) {
        synchronized (tokenStack) {
            if (tokenStack.isEmpty()) tokenStack.push(getToken());
            tokenStack.push(token);
            setToken(token);
        }
    }

    /**
     * Pops the current token off the stack.
     * Now the top of the stack is the previous token, so it becomes the active one.
     * @return The current token popped off (NOT the current active token, call getToken() to get that)
     */
    public String popToken() {
        synchronized (tokenStack) {
            tokenStack.pop();
            setToken(tokenStack.peek());
            return getToken();
        }
    }

    public static final String LOCATION_HEADER = "Location";
    private String getLocationHeader(HttpResponse response) {
        final Header header = response.getFirstHeader(LOCATION_HEADER);
        return header == null ? null : header.getValue();
    }

}
