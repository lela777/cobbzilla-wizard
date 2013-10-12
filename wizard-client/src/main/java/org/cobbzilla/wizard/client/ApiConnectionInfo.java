package org.cobbzilla.wizard.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class ApiConnectionInfo {

    @Getter @Setter private String baseUri;
    @Getter @Setter private String user;
    @Getter @Setter private String password;

    public ApiConnectionInfo (String baseUri) {
        this.baseUri = baseUri;
    }
}
