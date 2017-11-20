package org.cobbzilla.wizard.model;

public interface Identifiable {

    String UUID = "uuid";
    String[] UUID_ARRAY = {UUID};

    int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    String ENTITY_TYPE_HEADER_NAME = "ZZ-TYPE";

    String getUuid();
    void setUuid(String uuid);

    void beforeCreate();
    void beforeUpdate();
    void update(Identifiable thing);

}
