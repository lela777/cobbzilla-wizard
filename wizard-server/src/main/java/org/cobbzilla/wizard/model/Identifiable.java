package org.cobbzilla.wizard.model;

public interface Identifiable {

    public String getUuid();
    public void setUuid(String uuid);

    public void beforeCreate();
}
