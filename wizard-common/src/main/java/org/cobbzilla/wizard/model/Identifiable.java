package org.cobbzilla.wizard.model;

public interface Identifiable {

    String[] UUID_ARRAY = {"uuid"};

    int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    String getUuid();
    void setUuid(String uuid);

    void beforeCreate();
    void beforeUpdate();
}
