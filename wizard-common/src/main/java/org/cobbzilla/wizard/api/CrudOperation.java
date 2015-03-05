package org.cobbzilla.wizard.api;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum CrudOperation {

    create, read, update, delete;

    @JsonCreator public CrudOperation create(String s) { return valueOf(s.toLowerCase()); }

}
