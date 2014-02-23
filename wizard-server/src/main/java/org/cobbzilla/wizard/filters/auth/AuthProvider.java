package org.cobbzilla.wizard.filters.auth;

public interface AuthProvider<T> {

    public T find(String token);

}
