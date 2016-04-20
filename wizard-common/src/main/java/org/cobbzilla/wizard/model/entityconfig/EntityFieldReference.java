package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class EntityFieldReference {

    public static final String REF_PARENT = ":parent";

    @Getter @Setter private String entity;

    @Getter @Setter private String field;
    @Setter private String displayField;
    public String getDisplayField() { return empty(displayField) ? field : displayField; }

    @Getter @Setter private String finder;

}
