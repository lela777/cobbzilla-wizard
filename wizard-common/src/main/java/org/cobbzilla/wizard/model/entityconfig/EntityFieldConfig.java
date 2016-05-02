package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
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

    // value of 'options' may be:
    //  * a comma-separated string of values
    //  * JSON representing an array of EntityFieldOption objects
    //  * a special string 'uri:api-path:value:displayValue'
    //      this means:
    //        - do a GET of api-path
    //        - expect the response to be a JSON array of objects
    //        - for each object in the array, use the 'value' field for the option value, and the 'displayValue' field for the option's display value
    @Getter @Setter private String options;

    // this is a special option that indicates no selection has been made
    @Getter @Setter private String emptyDisplayValue;

    // Not exposed in JSON, since 'options' should be represented as a String when writing JSON
    @JsonIgnore public EntityFieldOption[] getOptionsArray() {

        if (empty(options)) return EMPTY_OPTIONS_ARRAY;
        if (options.startsWith("uri:"))  die("getOptionsArray: cannot convert uri-style options to array: "+options);

        if (options.trim().startsWith("[")) {
            return json(options, EntityFieldOption[].class);
        } else {
            final List<EntityFieldOption> opts = new ArrayList<>();
            for (String opt : options.split(",")) opts.add(new EntityFieldOption(opt.trim()));
            return opts.toArray(new EntityFieldOption[opts.size()]);
        }
    }

    // We do allow inbound-JSON to set options using a JSON array
    // Much nicer than nesting JSON-in-JSON, which would otherwise be required
    public void setOptionsList(EntityFieldOption[] options) { this.options = json(options); }

    @Getter @Setter private EntityFieldReference reference = null;
    @JsonIgnore public boolean isParentReference () {
        return getType() == EntityFieldType.reference && getReference().getEntity().equals(EntityFieldReference.REF_PARENT);
    }

    @Getter @Setter private String objectType;
}
