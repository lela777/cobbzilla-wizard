package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cobbzilla.util.string.StringUtil;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.camelCaseToString;

@ToString(of="name")
public class EntityConfig {

    @Getter @Setter private String className;
    @Getter @Setter private String name;

    @Setter private String displayName;
    public String getDisplayName() { return !empty(displayName) ? displayName : camelCaseToString(name); }

    @Setter private String pluralDisplayName;
    public String getPluralDisplayName() { return !empty(pluralDisplayName) ? pluralDisplayName : StringUtil.pluralize(getDisplayName()); }

    @Getter @Setter private String listUri;
    @Setter private List<String> listFields;
    public List<String> getListFields() { return !empty(listFields) ? listFields : getFieldNames(); }

    @Getter @Setter private Map<String, EntityFieldConfig> fields = new LinkedHashMap<>();
    @Setter private List<String> fieldNames;
    public List<String> getFieldNames() { return !empty(fieldNames) ? fieldNames : new ArrayList<>(getFields().keySet()); }

    @Setter private EntityFieldConfig parentField;
    public EntityFieldConfig getParentField () {
        if (parentField != null) return parentField;
        for (EntityFieldConfig fieldConfig : fields.values()) {
            if (fieldConfig.isParentReference()) return fieldConfig;
        }
        return null;
    }

    @Getter @Setter private String createMethod = "PUT";
    @Getter @Setter private String createUri;

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

    @Getter @Setter private Map<String, EntityConfig> children = new HashMap<>();
    public boolean hasChildren () { return !children.isEmpty(); }

}
