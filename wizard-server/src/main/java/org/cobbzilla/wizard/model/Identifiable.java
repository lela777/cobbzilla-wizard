package org.cobbzilla.wizard.model;

public interface Identifiable {

    public static final int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    public String getUuid();
    public void setUuid(String uuid);

    public void beforeCreate();
}
