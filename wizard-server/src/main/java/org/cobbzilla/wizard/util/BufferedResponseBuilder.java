package org.cobbzilla.wizard.util;

import lombok.Delegate;
import lombok.Getter;
import org.apache.http.HttpResponse;
import org.cobbzilla.util.io.StreamUtil;

import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BufferedResponseBuilder extends Response.ResponseBuilder {

    @Getter private final String document;
    @Getter private final Map<String, String> headers = new HashMap<>();

    public String getHeader (String name) { return headers.get(name); }

    private interface Excluded {
        public Response build();
    }
    @Delegate(types=Response.ResponseBuilder.class, excludes=Excluded.class)
    @Getter private final Response.ResponseBuilder builder;

    public Response.ResponseBuilder clone() { return new BufferedResponseBuilder(this); }

    public Response build() {
        final Response delegate = builder.build();
        return new BufferedResponse(this, delegate);
    }

    public BufferedResponseBuilder (BufferedResponseBuilder other) {
        this.document = other.document;
        this.headers.putAll(other.headers);
        this.builder = other.builder.clone();
    }

    public BufferedResponseBuilder(Response.ResponseBuilder builder,
                                   Integer length,
                                   HttpResponse response,
                                   Map<String, String> headers) throws IOException {

        if (length == null) length = new Integer(32 * 1024);
        final ByteArrayOutputStream out = new ByteArrayOutputStream(length);

        StreamUtil.copyLarge(response.getEntity().getContent(), out);

        document = out.toString();
        this.builder = builder.entity(document);
        this.headers.putAll(headers);
    }

}
