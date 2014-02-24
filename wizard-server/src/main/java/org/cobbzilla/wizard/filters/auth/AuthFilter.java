package org.cobbzilla.wizard.filters.auth;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public abstract class AuthFilter<T extends TokenPrincipal> implements ContainerRequestFilter {

    @Getter @Setter private String authTokenHeader;
    @Getter @Setter private Set<String> skipAuthPaths;

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        final String uri = request.getRequestUri().getPath();
        if (skipAuthPaths.contains(uri)) return request;

        final String token = request.getHeaderValue(authTokenHeader);
        if (token == null) throw new AuthException();

        final T principal = getAuthProvider().find(token);
        if (principal == null) throw new AuthException();

        if (!isPermitted(principal, request)) throw new AuthException();

        principal.setApiToken(token);

        request.setSecurityContext(new SimpleSecurityContext(principal));

        return request;
    }

    protected abstract boolean isPermitted(T principal, ContainerRequest request);

    protected abstract AuthProvider<T> getAuthProvider();

}
