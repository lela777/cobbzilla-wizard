package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class HttpConfiguration {

    @Getter @Setter private int port;
    @Getter @Setter private String baseUri;

}
