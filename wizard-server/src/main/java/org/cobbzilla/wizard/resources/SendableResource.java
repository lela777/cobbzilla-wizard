package org.cobbzilla.wizard.resources;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.ws.rs.core.StreamingOutput;

@Accessors(chain=true)
public class SendableResource {

    public SendableResource (StreamingOutput out) { setOut(out); }

    @Getter @Setter private StreamingOutput out;
    @Getter @Setter private String name;
    @Getter @Setter private String contentType;
    @Getter @Setter private Long contentLength;
    @Getter @Setter private Boolean forceDownload;
}
