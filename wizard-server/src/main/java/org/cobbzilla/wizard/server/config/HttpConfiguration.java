package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpConfiguration {

    @Getter @Setter private int port;
    @Getter @Setter private String baseUri;

    public String getHost () throws URISyntaxException {
        return new URI(baseUri).getHost();
    }
}
