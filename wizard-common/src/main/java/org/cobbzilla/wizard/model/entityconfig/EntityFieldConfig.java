package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class EntityFieldConfig {

    @Getter @Setter private String name;
    @Setter private String displayName;

    public String getDisplayName() { return !empty(displayName) ? displayName : getName(); }

    @Getter @Setter private EntityFieldMode mode = EntityFieldMode.standard;
    @Getter @Setter private EntityFieldType type = EntityFieldType.string;
    @Getter @Setter private EntityFieldControl control = EntityFieldControl.text;
    @Getter @Setter private EntityFieldReference reference = null;
    @Getter @Setter private Integer length = null;

}
