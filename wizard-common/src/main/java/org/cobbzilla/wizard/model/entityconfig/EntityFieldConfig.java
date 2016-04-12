package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;

public class EntityFieldConfig {

    @Getter @Setter private String displayName;
    @Getter @Setter private EntityFieldMode mode = EntityFieldMode.standard;
    @Getter @Setter private EntityFieldType type = EntityFieldType.string;

    @Setter private EntityFieldControl control;
    public EntityFieldControl getControl() {
        if (control != null) return control;
        if (type == EntityFieldType.flag) return EntityFieldControl.checkbox;
        return EntityFieldControl.text;
    }

    @Getter @Setter private EntityFieldReference reference = null;
    @Getter @Setter private Integer length = null;

}
