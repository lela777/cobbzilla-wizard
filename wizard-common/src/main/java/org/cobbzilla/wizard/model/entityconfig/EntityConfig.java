package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.camelCaseToString;

public class EntityConfig {

    @Getter @Setter private String name;

    @Setter private String displayName;
    public String getDisplayName() { return !empty(displayName) ? displayName : camelCaseToString(name); }

    @Setter private String pluralDisplayName;
    public String getPluralDisplayName() { return !empty(pluralDisplayName) ? pluralDisplayName : StringUtil.pluralize(getDisplayName()); }

    @Getter @Setter private String listUri;
    @Getter @Setter private List<String> listFields;

    @Getter @Setter private Map<String, EntityFieldConfig> fields = new LinkedHashMap<>();
    @Setter private List<String> fieldNames;

    public List<String> getFieldNames() { return !empty(fieldNames) ? fieldNames : new ArrayList<>(getFields().keySet()); }

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
