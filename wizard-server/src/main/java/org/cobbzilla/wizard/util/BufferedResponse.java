package org.cobbzilla.wizard.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class BufferedResponse extends Response {

    public static final Pattern META_URL_PATTERN = Pattern.compile("URL=([^\"]+)\"");

    @Delegate @Getter @Setter private Response response;

    public BufferedResponse(int status) { this.statusCode = status; }

    @Getter @Setter private int statusCode;

    @JsonIgnore public boolean is1xx () { return statusCode / 100 == 1; }
    @JsonIgnore public boolean is2xx () { return statusCode / 100 == 2; }
    @JsonIgnore public boolean is3xx () { return statusCode / 100 == 3; }
    @JsonIgnore public boolean is4xx () { return statusCode / 100 == 4; }
    @JsonIgnore public boolean is5xx () { return statusCode / 100 == 5; }

    @JsonIgnore
    public boolean isSuccess() {
        final int statusClass = statusCode / 100;
        return statusClass >= 1 && statusClass <= 3;
    }

    @Getter @Setter private String requestUri;

    @Getter private Multimap<String, String> headers = ArrayListMultimap.create();
    public void setHeader (String name, String value) { headers.put(name, value); }

    public Collection<String> getHeaderValues(String name) { return headers.get(name); }

    public String getFirstHeaderValue(String name) {
        final Collection<String> values = headers.get(name);
        if (values == null) return null;
        final Iterator<String> iterator = values.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Getter @Setter private String document;
    public boolean hasDocument () { return document != null; }

    @JsonIgnore public InputStream getDocumentStream() { return new ByteArrayInputStream(getDocument().getBytes()); }

    public String getMetaRedirect() {
        if (document == null || !document.contains("http-equiv=\"refresh\"")) return null;
        try {
            final int metaPos = document.indexOf("<meta http-equiv=\"refresh\"");
            if (metaPos != -1) {
                final int metaEnd = document.indexOf(">", metaPos);
                final String metaTag = document.substring(metaPos, metaEnd);
                final Matcher matcher = META_URL_PATTERN.matcher(metaTag);
                if (matcher.find()) return matcher.group(1);
            }
            return null;

        } catch (Exception e) {
            log.error("getMetaRedirect: "+e);
            return null;
        }
    }

    public String getRedirectUri() {
        if (is3xx()) return getFirstHeaderValue(HttpHeaders.LOCATION);
        if (is2xx()) return getMetaRedirect();
        return null;
    }

    public Response withNewDocument(String document) {
        final Response orig = getResponse();
        final ResponseBuilder updated = Response.status(orig.getStatus()).entity(document);
        final Multimap<String, String> origHeaders = getHeaders();
        for (String headerName : origHeaders.keySet()) {
            if (headerName.equals(HttpHeaders.CONTENT_LENGTH)) {
                updated.header(HttpHeaders.CONTENT_LENGTH, document.length());
            } else {
                for (String headerValue : origHeaders.get(headerName)) {
                    updated.header(headerName, headerValue);
                }
            }
        }
        return updated.build();
    }

}
