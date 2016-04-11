package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityFieldType {

    string, integer, decimal, epoch_time, reference;

    @JsonCreator public static EntityFieldType create (String val) { return valueOf(val.toLowerCase()); }
}
