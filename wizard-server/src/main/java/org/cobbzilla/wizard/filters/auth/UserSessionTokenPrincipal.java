package org.cobbzilla.wizard.filters.auth;

public interface UserSessionTokenPrincipal extends TokenPrincipal {

    String getUserSessionToken();
    void setUserSessionToken(String userSessionToken);

}
