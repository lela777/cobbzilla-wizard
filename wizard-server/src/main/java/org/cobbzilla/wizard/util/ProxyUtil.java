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

        // copy status
        Response.ResponseBuilder builder = Response.status(response.getStatusLine().getStatusCode());

        // copy headers
        Integer contentLength = null;
        final Map<String, String> responseHeaders = new HashMap<>();
        for (final Header header : response.getAllHeaders()) {

            final String headerName = header.getName();
            String headerValue = header.getValue();

            if (headerName.equals(HttpHeaders.CONTENT_LENGTH)) {
                contentLength = Integer.valueOf(header.getValue());

            } else if (headerName.equals(HttpHeaders.LOCATION)) {
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
