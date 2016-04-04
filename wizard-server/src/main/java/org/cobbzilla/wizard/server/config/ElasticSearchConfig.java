package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class ElasticSearchConfig {

    @Getter @Setter private String cluster = "elasticsearch";
    @Getter @Setter private String[] servers;

}
