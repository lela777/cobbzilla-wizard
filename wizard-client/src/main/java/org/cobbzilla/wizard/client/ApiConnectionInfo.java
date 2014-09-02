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

    // alias for when this is used in json with snake_case naming conventions
    public String getBase_uri () { return getBaseUri(); }
    public void setBase_uri (String uri) { setBaseUri(uri); }

}
