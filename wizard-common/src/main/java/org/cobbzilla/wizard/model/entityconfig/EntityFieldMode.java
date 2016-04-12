package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public enum EntityFieldMode {

    standard, createOnly, readOnly;

    @JsonCreator public static EntityFieldMode create (String val) {
        for (EntityFieldMode m : values()) if (m.name().equalsIgnoreCase(val)) return m;
        return die("create("+val+"): invalid");
    }

}
