package org.cobbzilla.wizard.util;

import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpStatusCodes;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ProxyUtil {

    public static Response proxyResponse (HttpRequestBean<String> requestBean,
                                          HttpContext context,
                                          boolean bufferResponse,
                                          String baseUri) throws IOException {
        @Cleanup final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpUriRequest request = initHttpRequest(requestBean);
        // copy context headers into map, then overwrite with request bean headers (they take precedence)
        final MultivaluedMap<String, String> requestHeaders = new MultivaluedMapImpl(context.getRequest().getRequestHeaders());
        for (Map.Entry<String, String> entry : requestBean.getHeaders().entrySet()) {
            requestHeaders.add(entry.getKey(), entry.getValue());
        }

        // copy finalized headers into the request
        for ( Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                request.setHeader(entry.getKey(), value);
            }
        }

        final HttpResponse response;
        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            log.error("Error proxying response: "+request+": "+e, e);
            return Response.serverError().build();
        }

        // copy status - if the request was successful, and the request bean indicated that the request should
        // be treated as redirected, set the status accordingly ... this is
        // a workaround for situations where the request we're proxying is initiated by a server-side
        // API call whose base url doesn't match the one the client submitted. e.g., client submits a
        // request to example.com/api/email?blahblah which the API server turns into a request to
        // example.com/roundcube/somestuff. if we don't set the redirect, the relative links in
        // the response will all point to /api/ instead of /roundcube/, potentially breaking things.
        final int responseStatus = response.getStatusLine().getStatusCode();
        final int proxyStatus = responseStatus == HttpStatusCodes.OK && requestBean.isRedirect() ? 
                HttpStatusCodes.FOUND : responseStatus;
        Response.ResponseBuilder builder = Response.status(proxyStatus);
        builder.location(request.getURI());

        // copy headers, adding a location header if there isn't one already
        Integer contentLength = null;
        boolean foundLocationHeader = false;
        final Map<String, String> responseHeaders = new HashMap<>();
        for (final Header header : response.getAllHeaders()) {

            final String headerName = header.getName();
            String headerValue = header.getValue();

            if (headerName.equals(HttpHeaders.CONTENT_LENGTH)) {
                contentLength = Integer.valueOf(header.getValue());

            } else if (headerName.equals(HttpHeaders.LOCATION)) {
                foundLocationHeader = true;
                if (baseUri != null
                        && !headerValue.startsWith("/")
                        && !headerValue.startsWith("http://")
                        && !headerValue.startsWith("https://")) {
                    // rewrite relative redirects to be absolute, so they resolve
                    headerValue = baseUri + headerValue;
                }
            }

            builder.header(headerName, headerValue);
            responseHeaders.put(headerName, headerValue);
        }

        if (!foundLocationHeader) {
            responseHeaders.put(HttpHeaders.LOCATION, request.getURI().toString());
        }

        // buffer the entire response?
        if (bufferResponse) {
            return new BufferedResponseBuilder(builder, contentLength, response, responseHeaders).build();
        }

        if (contentLength != null) {
            return builder.entity(new StreamStreamingOutput(response.getEntity().getContent())).build();
        }

        return builder.build();
    }

    private static HttpUriRequest initHttpRequest(HttpRequestBean<String> requestBean) {
        log.info("initHttpRequest: requestBean.uri="+requestBean.getUri());
        try {
            final HttpUriRequest request;
            switch (requestBean.getMethod()) {
                case HttpMethods.GET:
                    request = new HttpGet(requestBean.getUri());
                    break;

                case HttpMethods.POST:
                    request = new HttpPost(requestBean.getUri());
                    if (requestBean.hasData()) ((HttpPost) request).setEntity(new StringEntity(requestBean.getData()));
                    break;

                case HttpMethods.PUT:
                    request = new HttpPut(requestBean.getUri());
                    if (requestBean.hasData()) ((HttpPut) request).setEntity(new StringEntity(requestBean.getData()));
                    break;

                case HttpMethods.DELETE:
                    request = new HttpDelete(requestBean.getUri());
                    break;

                default:
                    throw new IllegalStateException("Invalid request method: "+requestBean.getMethod());
            }
            return request;

        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("initHttpRequest: " + e, e);
        }
    }
}
