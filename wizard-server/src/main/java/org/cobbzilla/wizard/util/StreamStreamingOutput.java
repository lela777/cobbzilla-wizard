package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.StreamUtil;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@AllArgsConstructor
public class StreamStreamingOutput implements StreamingOutput {

    private InputStream in;

    @Override
    public void write(OutputStream out) throws IOException, WebApplicationException {
        StreamUtil.copyLarge(in, out);
    }

}
