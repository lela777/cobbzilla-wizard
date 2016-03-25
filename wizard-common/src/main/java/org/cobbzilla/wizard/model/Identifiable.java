package org.cobbzilla.wizard.model;

public interface Identifiable {

    int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    String getUuid();
    void setUuid(String uuid);

    void beforeCreate();
    void beforeUpdate();
}
