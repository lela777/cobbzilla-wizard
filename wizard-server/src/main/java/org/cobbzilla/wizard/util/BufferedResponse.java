package org.cobbzilla.wizard.util;

import lombok.Delegate;

import javax.ws.rs.core.Response;
import java.util.Map;

public class BufferedResponse extends Response {

    private final BufferedResponseBuilder builder;

    public Map<String, String> getHeaders () { return builder.getHeaders(); }
    public String getHeader (String name) { return builder.getHeader(name); }
    public String getDocument () { return builder.getDocument(); }

    @Delegate private final Response response;

    public BufferedResponse(BufferedResponseBuilder bufferedResponseBuilder, Response delegate) {
        this.builder = bufferedResponseBuilder;
        this.response = delegate;
    }
}
