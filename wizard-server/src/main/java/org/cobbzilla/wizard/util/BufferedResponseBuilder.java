package org.cobbzilla.wizard.util;

import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BufferedResponseBuilder {

    private Response.ResponseBuilder builder;
    private BufferedResponse buffered;

    public BufferedResponseBuilder (int status) {
        builder = Response.status(status);
        buffered = new BufferedResponse(status);
    }

    public void setHeader(String headerName, String headerValue) {
        builder = builder.header(headerName, headerValue);
        buffered.setHeader(headerName, headerValue);
    }

    public void setDocument (InputStream in, Integer length) throws IOException {
        int initialSize = (length != null) ? length : 32 * 1024;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(initialSize);
        StreamUtil.copyLarge(in, out);

        final String document = out.toString(StringUtil.UTF8);
        buffered.setDocument(document);
        builder.entity(document);

        if (document.length() > 0 && length == null) {
            setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(document.length()));
        }
    }

    public BufferedResponse build () {
        buffered.setResponse(builder.build());
        return buffered;
    }

}
