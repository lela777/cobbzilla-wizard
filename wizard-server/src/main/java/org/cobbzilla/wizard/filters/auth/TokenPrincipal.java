package org.cobbzilla.wizard.filters.auth;

import java.security.Principal;

public interface TokenPrincipal extends Principal {

    public String getApiToken ();
    public void setApiToken (String token);
}
