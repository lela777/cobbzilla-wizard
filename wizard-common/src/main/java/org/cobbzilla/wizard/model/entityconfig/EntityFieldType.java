package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityFieldType {

    string, integer, decimal, flag, date, epoch_time, reference, embedded;

    @JsonCreator public static EntityFieldType create (String val) { return valueOf(val.toLowerCase()); }
}
