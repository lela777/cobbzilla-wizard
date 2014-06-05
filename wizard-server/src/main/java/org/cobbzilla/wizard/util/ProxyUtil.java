package org.cobbzilla.wizard.util;

import com.google.common.collect.Multimap;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cobbzilla.util.http.HttpCookieBean;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.http.HttpUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.*;

@Slf4j
public class ProxyUtil {

    private static final String TRANSFER_ENCODING = "transfer-encoding";

    public static BufferedResponse proxyResponse (HttpRequestBean<String> requestBean,
                                                  HttpContext callerContext,
                                                  String baseUri,
                                                  String cookieDomain) throws IOException {

        @Cleanup final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpUriRequest request = HttpUtil.initHttpRequest(requestBean);

        // copy callerContext headers into map, then overwrite with request bean headers (they take precedence)
        final MultivaluedMap<String, String> requestHeaders = new MultivaluedMapImpl(callerContext.getRequest().getRequestHeaders());
        final Multimap<String, String> headers = requestBean.getHeaders();
        for (String key : headers.keySet()) {
            // skip Host header, we will write this at the end to match the URL
            if (!key.equals(HttpHeaders.HOST)) {
                for (String value : headers.get(key)) {
                    requestHeaders.add(key, value);
                }
            }
        }

        // copy finalized headers into the request
        for ( Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                request.setHeader(entry.getKey(), value);
            }
        }

        // force Host header to match URL we are requesting
        request.setHeader(HttpHeaders.HOST, requestBean.getHost());

        final HttpResponse response;
        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            log.error("Error proxying response: "+request+": "+e, e);
            return new BufferedResponseBuilder(HttpStatusCodes.SERVER_ERROR).build();
        }

        final int responseStatus = response.getStatusLine().getStatusCode();
        BufferedResponseBuilder buffered = new BufferedResponseBuilder(responseStatus);

        // copy headers, adding a location header if there isn't one already
        Integer contentLength = null;

        for (final Header header : response.getAllHeaders()) {

            final String headerName = header.getName();
            String headerValue = header.getValue();

            if (headerName.equalsIgnoreCase(CONTENT_LENGTH)
                    || headerName.equalsIgnoreCase(TRANSFER_ENCODING)
                    || headerName.equalsIgnoreCase(CONTENT_ENCODING)) {
                log.info("skipping " + headerName + " (setDocument will handle this)");
                continue;

            } else if (cookieDomain != null && headerName.equals(SET_COOKIE)) {
                // ensure that cookies are for the top-level domain, since they will be sent to cloudos
                final HttpCookieBean cookie = HttpCookieBean.parse(headerValue);
                cookie.setDomain(cookieDomain);
                log.info("rewriting cookie: " + headerValue + " with domain=" + cookie.getDomain());
                headerValue = cookie.toHeaderValue();

            } else if (headerName.equalsIgnoreCase(LOCATION)) {
                if (baseUri != null
                        && !headerValue.startsWith("/")
                        && !headerValue.startsWith("http://")
                        && !headerValue.startsWith("https://")) {
                    // rewrite relative redirects to be absolute, so they resolve
                    headerValue = baseUri + headerValue;
                }
            }

            buffered.setHeader(headerName, headerValue);
        }

        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            buffered.setDocument(response.getEntity().getContent(), contentLength);
        }

        return buffered.build();
    }
}
