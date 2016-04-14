package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;

public class EntityFieldReference {

    public static final String REF_PARENT = ":parent";

    @Getter @Setter private String entity;
    @Getter @Setter private String field;
    @Getter @Setter private String displayField;
    @Getter @Setter private String finder;

}
