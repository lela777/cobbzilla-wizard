package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class JerseyConfiguration {

    @Getter @Setter private String[] resourcePackages;
    @Getter @Setter private String[] responseFilters;

}
