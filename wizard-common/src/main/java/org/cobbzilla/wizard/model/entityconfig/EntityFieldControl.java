package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityFieldControl {

    text, textarea, checkbox, select, multi_select, autocomplete, hidden;

    @JsonCreator public static EntityFieldControl create (String val) { return valueOf(val.toLowerCase()); }

}
