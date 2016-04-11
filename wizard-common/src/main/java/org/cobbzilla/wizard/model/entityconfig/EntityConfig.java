package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class EntityConfig {

    @Getter @Setter private String name;
    @Setter private String displayName;
    @Setter private String pluralDisplayName;

    @Getter @Setter private String listUri;
    @Getter @Setter private List<String> listFields;

    public String getDisplayName() { return !empty(displayName) ? displayName : name; }

    public String getPluralDisplayName() { return !empty(pluralDisplayName) ? pluralDisplayName : StringUtil.pluralize(getDisplayName()); }

    @Getter @Setter private Map<String, EntityFieldConfig> fields;

    @Getter @Setter private List<String> createFields;
    @Getter @Setter private String createMethod = "PUT";
    @Getter @Setter private String createUri;

    @Getter @Setter private List<String> updateFields;
    @Getter @Setter private String updateMethod = "POST";
    @Getter @Setter private String updateUri;

    @Getter @Setter private String deleteMethod = "DELETE";
    @Setter private String deleteUri;
    public String getDeleteUri() { return !empty(deleteUri) ? deleteUri : getUpdateUri(); }

    public void addParent(EntityConfig parentConfig) {
        for (Map.Entry<String, EntityFieldConfig> fieldConfig : parentConfig.getFields().entrySet()) {
            if (!this.fields.containsKey(fieldConfig.getKey())) {
                this.fields.put(fieldConfig.getKey(), fieldConfig.getValue());
            }
        }
    }

}
