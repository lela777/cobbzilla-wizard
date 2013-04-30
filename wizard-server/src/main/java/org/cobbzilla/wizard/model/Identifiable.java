package org.cobbzilla.wizard.model;

public interface Identifiable {

    public Long getId();
    public void setId(Long id);

    public String getUuid();
    public void setUuid(String uuid);

    public void beforeCreate();

}
