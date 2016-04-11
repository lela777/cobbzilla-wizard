package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;

public class EntityFieldConfig {

    @Getter @Setter private String name;
    @Getter @Setter private EntityFieldType type = EntityFieldType.string;
    @Getter @Setter private EntityFieldControl control = EntityFieldControl.text;
    @Getter @Setter private EntityFieldReference reference = null;
    @Getter @Setter private Integer length = null;

}
