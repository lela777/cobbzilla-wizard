package org.cobbzilla.wizard.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.cobbzilla.util.http.*;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.api.ApiException;
import org.cobbzilla.wizard.api.ForbiddenException;
import org.cobbzilla.wizard.api.NotFoundException;
import org.cobbzilla.wizard.api.ValidationException;
import org.cobbzilla.wizard.model.entityconfig.ModelEntity;
import org.cobbzilla.wizard.util.RestResponse;

import java.io.*;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.http.HttpStatusCodes.*;
import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;

@Slf4j @NoArgsConstructor @ToString(of={"connectionInfo"})
public class ApiClientBase implements Cloneable {

    public static final ContentType CONTENT_TYPE_JSON = ContentType.APPLICATION_JSON;

    @SuppressWarnings("CloneDoesntCallSuperClone") // subclasses must have a copy constructor
    @Override public Object clone() { return instantiate(getClass(), this); }

    @Getter protected ApiConnectionInfo connectionInfo;
    @Getter protected String token;

    public String getSuperuserToken () { return null; } // subclasses may override

    // the server may be coming up, and either not accepting connections or issuing 503 Service Unavailable.
    @Getter @Setter protected int numTries = 5;
    @Getter @Setter protected long retryDelay = TimeUnit.SECONDS.toMillis(1);

    @Getter @Setter protected boolean captureHeaders = false;
    @Getter @Setter private HttpContext httpContext = null;
    @Getter private Map<String, String> headers = null;

    public void setHeaders(JsonNode jsonNode) {
        ObjectMapper mapper = new ObjectMapper();
        headers = mapper.convertValue(jsonNode, Map.class);
    }
    public void removeHeaders () { headers = null; }

    public void setToken(String token) {
        this.token = token;
        this.tokenCtime = empty(token) ? 0 : now();
    }

    private long tokenCtime = 0;
    public boolean hasToken () { return !empty(token); }
    public long getTokenAge () { return now() - tokenCtime; }

    public void logout () { setToken(null); }

    public ApiClientBase (ApiClientBase other) { this.connectionInfo = other.getConnectionInfo(); }
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

    protected HttpClient httpClient;
    public HttpClient getHttpClient() {
        if (httpClient == null) httpClient = HttpClients.createDefault();
        return httpClient;
    }
    public void setHttpClient(HttpClient httpClient) { this.httpClient = httpClient; }

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
                return die("Unsupported request method: "+requestBean.getMethod());
        }
    }

    public RestResponse process_raw(HttpRequestBean requestBean) throws Exception {
        switch (requestBean.getMethod()) {
            case HttpMethods.GET:
                return doGet(requestBean.getUri());
            case HttpMethods.POST:
                return doPost(requestBean.getUri(), requestBean.getEntity(), requestBean.getContentType());
            case HttpMethods.PUT:
                return doPut(requestBean.getUri(), requestBean.getEntity(), requestBean.getContentType());
            case HttpMethods.DELETE:
                return doDelete(requestBean.getUri());
            default:
                return die("Unsupported request method: "+requestBean.getMethod());
        }
    }

    protected void assertStatusOK(RestResponse response) {
        if (response.status != HttpStatusCodes.OK
                && response.status != HttpStatusCodes.CREATED
                && response.status != HttpStatusCodes.NO_CONTENT) throw new ApiException(response);
    }

    private String getJson(HttpRequestBean requestBean) throws Exception {
        Object data = requestBean.getEntity();
        if (data == null) return null;
        if (data instanceof String) return (String) data;
        return toJson(data);
    }

    protected ApiException specializeApiException(ApiException e) { return specializeApiException(null, e.getResponse()); }

    protected ApiException specializeApiException(HttpRequestBean request, RestResponse response) {
        if (response.isSuccess()) {
            die("specializeApiException: cannot specialize exception for a successful response: "+response);
        }
        switch (response.status) {
            case NOT_FOUND:
                return new NotFoundException(request, response);
            case FORBIDDEN:
                return new ForbiddenException(request, response);
            case UNPROCESSABLE_ENTITY:
                return new ValidationException(request, response);
            default: return new ApiException(request, response);
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
        if (!restResponse.isSuccess()) throw specializeApiException(HttpRequestBean.get(path), restResponse);
        return restResponse;
    }

    public <T> T get(String path, Class<T> responseClass) throws Exception {
        return fromJson(get(path).json, responseClass);
    }

    protected <T> void setRequestEntity(HttpEntityEnclosingRequest entityRequest, T data, ContentType contentType) {
        if (data != null) {
            if (data instanceof InputStream) {
                entityRequest.setEntity(new InputStreamEntity((InputStream) data, contentType));
                log.debug("setting entity=(InputStream)");
            } else {
                entityRequest.setEntity(new StringEntity(data.toString(), contentType));
                log.debug("setting entity=(" + data.toString().length()+" json chars)");
                log.trace(data.toString());
            }
        }
    }

    public RestResponse doPost(String path, String json) throws Exception {
        return doPost(path, json, CONTENT_TYPE_JSON);
    }

    public <T> RestResponse doPost(String path, T data, ContentType contentType) throws Exception {
        final HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPost httpPost = new HttpPost(url);
        setRequestEntity(httpPost, data, contentType);
        return getResponse(client, httpPost);
    }

    public <T> T post(String path, Object request, Class<T> responseClass) throws Exception {
        if (request instanceof String) return post(path, request, responseClass);
        if (request instanceof ModelEntity) return post(path, ((ModelEntity) request).getEntity(), responseClass);
        return fromJson(post(path, toJson(request)).json, responseClass);
    }

    public <T> T post(String path, T request) throws Exception {
        if (request instanceof ModelEntity) {
            return (T) post(path, ((ModelEntity) request).getEntity(), ((ModelEntity) request).getEntity().getClass());
        } else {
            return post(path, request, (Class<T>) request.getClass());
        }
    }

    public RestResponse post(String path, String json) throws Exception {
        return post(path, json, CONTENT_TYPE_JSON);
    }

    public RestResponse post(String path, String data, ContentType contentType) throws Exception {
        final RestResponse restResponse = doPost(path, data, contentType);
        if (!restResponse.isSuccess()) throw specializeApiException(HttpRequestBean.post(path, data), restResponse);
        return restResponse;
    }

    public RestResponse doPut(String path, String json) throws Exception {
        return doPut(path, json, CONTENT_TYPE_JSON);
    }

    public <T> RestResponse doPut(String path, T data, ContentType contentType) throws Exception {
        HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpPut httpPut = new HttpPut(url);
        setRequestEntity(httpPut, data, contentType);
        return getResponse(client, httpPut);
    }

    public <T> T put(String path, Object request, Class<T> responseClass) throws Exception {
        return fromJson(put(path, toJson(request)).json, responseClass);
    }

    public <T> T put(String path, T request) throws Exception {
        if (request instanceof ModelEntity) {
            return (T) put(path, ((ModelEntity) request).getEntity(), ((ModelEntity) request).getEntity().getClass());
        } else {
            return put(path, request, (Class<T>) request.getClass());
        }
    }

    public <T> T put(String path, String json, Class<T> responseClass) throws Exception {
        final RestResponse response = put(path, json);
        if (!response.isSuccess()) throw specializeApiException(HttpRequestBean.put(path, json), response);
        return fromJson(response.json, responseClass);
    }

    public RestResponse put(String path, String json) throws Exception {
        final RestResponse restResponse = doPut(path, json);
        if (!restResponse.isSuccess()) throw specializeApiException(HttpRequestBean.put(path, json), restResponse);
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
        if (!restResponse.isSuccess()) throw specializeApiException(HttpRequestBean.delete(path), restResponse);
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
        RestResponse restResponse = null;
        IOException exception = null;
        for (int i=0; i<numTries; i++) {
            if (i > 0) {
                sleep(retryDelay);
                retryDelay *= 2;
            }
            try {
                final HttpResponse response = client.execute(request, httpContext);
                final int statusCode = response.getStatusLine().getStatusCode();
                String responseJson = null;
                byte[] responseBytes = null;
                final HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream in = entity.getContent()) {
                        if (isCaptureHeaders() && response.containsHeader("content-disposition")) {
                            responseBytes = IOUtils.toByteArray(in);
                        } else {
                            responseJson = IOUtils.toString(in, UTF8cs);
                            log.debug("response: " + responseJson);
                        }
                    }
                } else {
                    responseJson = null;
                }

                restResponse = empty(responseBytes)
                        ? new RestResponse(statusCode, responseJson, getLocationHeader(response))
                        : new RestResponse(statusCode, responseBytes, getLocationHeader(response));
                if (isCaptureHeaders()) {
                    for (Header header : response.getAllHeaders()) {
                        restResponse.addHeader(header.getName(), header.getValue());
                    }
                }
                if (statusCode != SERVER_UNAVAILABLE) return restResponse;
                log.warn("getResponse("+request.getMethod()+" "+request.getURI().toASCIIString()+", attempt="+i+"/"+numTries+") returned "+SERVER_UNAVAILABLE+", will " + ((i+1)>=numTries ? "NOT":"sleep for "+formatDuration(retryDelay)+" then") + " retry the request");

            } catch (IOException e) {
                log.warn("getResponse("+request.getMethod()+" "+request.getURI().toASCIIString()+", attempt="+i+"/"+numTries+") threw exception "+e+", will " + ((i+1)>=numTries ? "NOT":"sleep for "+formatDuration(retryDelay)+" then") + " retry the request");
                exception = e;
            }
        }
        if (restResponse != null) return restResponse;
        throw exception;
    }

    public File getFile (String path) throws IOException {

        final HttpClient client = getHttpClient();
        final String url = getUrl(path, getBaseUri());
        @Cleanup("releaseConnection") HttpRequestBase request = new HttpGet(url);
        request = beforeSend(request);

        final HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == NOT_FOUND) return null;
        if (statusCode == FORBIDDEN) throw new ForbiddenException();
        if (!RestResponse.isSuccess(statusCode)) die("getFile("+url+"): error: "+statusCode);

        final HttpEntity entity = response.getEntity();
        if (entity == null) die("getFile("+url+"): No entity");

        final File file = File.createTempFile(getClass().getName()+"-", getTempFileSuffix(path, HttpUtil.getContentType(response)), getDefaultTempDir());
        try (InputStream in = entity.getContent()) {
            try (OutputStream out = new FileOutputStream(file)) {
                IOUtils.copyLarge(in, out);
            }
        }

        return file;
    }

    protected String getTempFileSuffix(String path, String contentType) {
        if (empty(contentType)) return ".temp";
        switch (contentType) {
            case "image/jpeg": return ".jpg";
            case "image/gif": return ".gif";
            case "image/png": return ".png";
            default: return ".temp";
        }
    }

    protected HttpRequestBase beforeSend(HttpRequestBase request) {
        if (!empty(token)) {
            final String tokenHeader = getTokenHeader();
            if (empty(tokenHeader)) die("token set but getTokenHeader returned null");
            request.addHeader(tokenHeader, token);
        }
        if (!empty(headers)) {
            for (Map.Entry<String, String> entry: headers.entrySet()){
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    public String getTokenHeader() { return null; }

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

    // call our own copy constructor, return new instance that is a copy of ourselves
    public ApiClientBase copy() { return ReflectionUtil.copy(this); }

}
