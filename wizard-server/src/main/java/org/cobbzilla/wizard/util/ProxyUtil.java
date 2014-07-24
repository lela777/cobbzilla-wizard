package org.cobbzilla.wizard.util;

import com.google.common.collect.Multimap;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.core.util.StringKeyStringValueIgnoreCaseMultivaluedMap;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cobbzilla.util.http.*;

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

        return proxyResponse(requestBean, callerContext, baseUri, cookieDomain, null);
    }

    public static BufferedResponse proxyResponse (HttpRequestBean<String> requestBean,
                                                  HttpContext callerContext,
                                                  String baseUri,
                                                  String cookieDomain,
                                                  CookieJar cookieJar) throws IOException {
        if (cookieJar == null) cookieJar = new CookieJar();

        @Cleanup final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpUriRequest request = HttpUtil.initHttpRequest(requestBean);

        // wow i hate java sometimes. who names shit like this? sorry paul.
        final StringKeyStringValueIgnoreCaseMultivaluedMap requestHeaders = new StringKeyStringValueIgnoreCaseMultivaluedMap();

        // copy callerContext headers into map, these are the 'defaults'
        final MultivaluedMap<String, String> contextHeaders = callerContext.getRequest().getRequestHeaders();
        for (String key : contextHeaders.keySet()) {
            if (!key.equalsIgnoreCase(HttpHeaders.COOKIE)) { // skip cookies, use cookiejar
                for (String value : contextHeaders.get(key)) {
                    requestHeaders.add(key, value);
                }
            }
        }

        // then overwrite with request bean headers, which take precedence
        final Multimap<String, String> headers = requestBean.getHeaders();
        for (String key : headers.keySet()) {
            // skip Host header, we will write this at the end to match the URL
            if (!key.equals(HttpHeaders.HOST)) {
                for (String value : headers.get(key)) {
                    requestHeaders.add(key, value);
                }
            }
        }

        // copy finalized headers into the request, track request cookies
        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            for (String value : entry.getValue()) {
                request.setHeader(entry.getKey(), value);
            }
        }

        // set cookies if any
        if (!cookieJar.isEmpty()) request.setHeader(HttpHeaders.COOKIE, cookieJar.getRequestValue());

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
        buffered.setRequestUri(request.getURI().toString());

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

            } else if (headerName.equals(SET_COOKIE)) {
                // ensure that cookies are for the top-level domain, since they will be sent to cloudos
                final HttpCookieBean cookie = HttpCookieBean.parse(headerValue);
                if (cookieDomain != null) {
                    cookie.setDomain(cookieDomain);
                    log.info("rewriting cookie: " + headerValue + " with domain=" + cookie.getDomain());
                }

                cookieJar.add(cookie);
                continue; // we'll set all the cookies at the end

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

        // always set all cookies in the jar, in case this is the last response and will be
        // sent back to the end user, they'll need the entire cookie state. so we send (or resend)
        // all cookies on every call to this proxy.
        for (HttpCookieBean cookie : cookieJar.values()) buffered.setHeader(SET_COOKIE, cookie.toHeaderValue());

        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            buffered.setDocument(response.getEntity().getContent(), contentLength);
        }

        return buffered.build();
    }
}
