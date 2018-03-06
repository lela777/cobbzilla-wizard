package org.cobbzilla.wizard.filters.auth;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import java.util.Set;

public abstract class AuthFilter<T extends TokenPrincipal> implements ContainerRequestFilter {

    protected abstract String getAuthTokenHeader();
    protected String getSubUserHeader() { return null; }
    protected abstract Set<String> getSkipAuthPaths();
    protected abstract Set<String> getSkipAuthPrefixes();

    @Override public ContainerRequest filter(ContainerRequest request) {

        final String uri = request.getRequestUri().getPath();

        boolean canSkip = canSkip(uri);

        final String token = request.getHeaderValue(getAuthTokenHeader());
        if (token == null) {
            if (canSkip) return request;
            throw new AuthException();
        }

        final T principal = getAuthProvider().find(token);
        if (principal == null) {
            if (!canSkip) throw new AuthException();
            return request;
        }

        if (!isPermitted(principal, request)) throw new AuthException();

        principal.setApiToken(token);
        request.setSecurityContext(getSecurityContext(request, principal));

        return request;
    }

    protected boolean canSkip(String uri) {
        return getSkipAuthPaths().contains(uri) || startsWith(uri, getSkipAuthPrefixes());
    }

    protected SimpleSecurityContext getSecurityContext(ContainerRequest request, T principal) {
        final String subUserHeader = getSubUserHeader();
        if (subUserHeader != null) {
            final String subUserId = request.getHeaderValue(subUserHeader);
            if (subUserId != null) {
                if (principal instanceof UserSessionTokenPrincipal) {
                    ((UserSessionTokenPrincipal)principal).setSubUser(subUserId);
                }
            }
        }
        return new SimpleSecurityContext(principal);
    }

    protected boolean startsWith(String uri, Set<String> prefixes) {
        for (String path : prefixes) {
            if (uri.startsWith(path)) return true;
        }
        return false;
    }

    protected abstract boolean isPermitted(T principal, ContainerRequest request);

    protected abstract AuthProvider<T> getAuthProvider();

}
