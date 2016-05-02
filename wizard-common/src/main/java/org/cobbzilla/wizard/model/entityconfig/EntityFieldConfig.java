package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.camelCaseToString;

public class EntityFieldConfig {

    public static final EntityFieldOption[] EMPTY_OPTIONS_ARRAY = new EntityFieldOption[0];
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
    @Getter @Setter private String emptyDisplayValue;

    @JsonIgnore public EntityFieldOption[] getOptionsList() {
        if (empty(options)) return EMPTY_OPTIONS_ARRAY;
        if (options.trim().startsWith("[")) {
            return json(options, EntityFieldOption[].class);
        } else {
            final List<EntityFieldOption> opts = new ArrayList<>();
            for (String opt : options.split(",")) {
                opts.add(new EntityFieldOption(opt));
            }
            return opts.toArray(new EntityFieldOption[opts.size()]);
        }
    }
    public void setOptionsList(EntityFieldOption[] options) { this.options = json(options); }

    @Getter @Setter private EntityFieldReference reference = null;
    @JsonIgnore public boolean isParentReference () {
        return getType() == EntityFieldType.reference && getReference().getEntity().equals(EntityFieldReference.REF_PARENT);
    }

    @Getter @Setter private String objectType;
}
