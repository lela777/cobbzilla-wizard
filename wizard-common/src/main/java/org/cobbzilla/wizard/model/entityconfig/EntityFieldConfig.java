package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.camelCaseToString;

public class EntityFieldConfig {

    @Getter @Setter private String name;

    @Setter private String displayName;
    public String getDisplayName() { return !empty(displayName) ? displayName : camelCaseToString(name); }

    @Getter @Setter private EntityFieldMode mode = EntityFieldMode.standard;
    @Getter @Setter private EntityFieldType type = EntityFieldType.string;
    @Getter @Setter private Integer length = null;

    @Setter private EntityFieldControl control;
    public EntityFieldControl getControl() {
        if (control != null) return control;
        if (type == EntityFieldType.flag) return EntityFieldControl.checkbox;
        return EntityFieldControl.text;
    }

    @Getter @Setter private String options;

    @Getter @Setter private EntityFieldReference reference = null;

    @JsonIgnore public boolean isParentReference () {
        return getType() == EntityFieldType.reference && getReference().getEntity().equals(EntityFieldReference.REF_PARENT);
    }
}
