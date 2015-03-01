package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class LdapConfiguration {

    @Getter @Setter private String password;
    @Getter @Setter private String domain;
    @Getter @Setter private String baseDN;

}
