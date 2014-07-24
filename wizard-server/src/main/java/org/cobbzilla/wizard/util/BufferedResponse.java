package org.cobbzilla.wizard.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.Delegate;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Iterator;

public class BufferedResponse extends Response {

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

}
