package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityFieldMode {

    standard, create_only, read_only;

    @JsonCreator public static EntityFieldMode create (String val) { return valueOf(val.toLowerCase()); }

}
