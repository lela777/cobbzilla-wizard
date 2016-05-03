package org.cobbzilla.wizard.model.entityconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class EntityFieldOption {

    @Getter @Setter private String value;
    @Setter private String displayValue;

    public String getDisplayValue() { return empty(displayValue) ? value : displayValue; }

    public EntityFieldOption(String value) { this(value, value); }

}
