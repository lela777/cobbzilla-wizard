package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cobbzilla.util.string.StringUtil;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.camelCaseToString;

/**
 * Defines API interactions for an entity.
 * <br/>
 * An EntityConfig describes how to use an API to work with instances of a particular REST resource class.
 * <br/>
 * Typically, an EntityConfig specifies:
 * <ul><li>
 *   The fields contained by the class, including lots of metadata.
 * </li><li>
 *   How to create, read, update and delete instances of the resource class.
 * </li><li>
 *   If the resource class has sub-resources (also known as child resources), there will be sub-EntityConfigs to describe those children
 * </li></ul>
 */
@ToString(of="name")
public class EntityConfig {

    /**
     * The name of the Java class for the EntityConfig.
     * Default value: the path to the EntityConfig resource is transformed into a Java class name.
     * For example, the entity config defined in `com/acme/model/User.json` would have a default className of `com.acme.model.User`
     */
    @Getter @Setter private String className;

    /**
     * The name of the entity class. This is typically the same as the file name. This is only required for top-level EntityConfigs,
     * it may be omitted from child resources.
     * Default value: none (required) for root EntityConfigs. For child configs, the default value is what the parent refers to that child as.
     */
    @Getter @Setter private String name;

    @Setter private String displayName;
    /**
     * The display name of the entity.
     * Default value: the value of the `name` field
     * @return The display name of the entity
     */
    public String getDisplayName() { return !empty(displayName) ? displayName : camelCaseToString(name); }

    @Setter private String pluralDisplayName;
    /**
     * The plural display name of the entity.
     * Default value: the value of `displayName` is pluralized, using some basic pluralization rules
     * @return The plural display name of the entity
     */
    public String getPluralDisplayName() { return !empty(pluralDisplayName) ? pluralDisplayName : StringUtil.pluralize(getDisplayName()); }

    /** The API endpoint to list instances of the entity class. This always assumes a GET request. */
    @Getter @Setter private String listUri;

    @Setter private List<String> listFields;

    /**
     * After using the `listUri` to obtain some entities, the `listFields` tells which fields should be
     * "columns" in the resulting data table. Fields not listed in `listFields` will not be shown.
     * Default value: all fields
     * @return a List of field names (keys found in the `fields` map) to use when displaying a list of entities.
     */
    public List<String> getListFields() { return !empty(listFields) ? listFields : getFieldNames(); }

    /**
     * A map of `name` -\> [EntityFieldConfig](EntityFieldConfig.md), with each object describing one field of this EntityConfig.
     * Default value: none (required)
     */
    @Getter @Setter private Map<String, EntityFieldConfig> fields = new LinkedHashMap<>();
    @Setter private List<String> fieldNames;

    /**
     * Get a list of all field names from the `fields` map.
     * @return the keySet of the fields map, as a List
     */
    public List<String> getFieldNames() { return !empty(fieldNames) ? fieldNames : new ArrayList<>(getFields().keySet()); }

    @Setter private EntityFieldConfig parentField;

    /**
     * For child entities, `parentField` indicates which EntityFieldConfig in the `fields` map is the one that
     * refers back to its parent. Default value: if one of the EntityFieldConfigs is a reference field with the
     * special reference value of `:parent`, then this field is used.
     * @return the field within the `fields` map that refers back to the parent entity of this entity.
     */
    public EntityFieldConfig getParentField () {
        if (parentField != null) return parentField;
        for (EntityFieldConfig fieldConfig : fields.values()) {
            if (fieldConfig.isParentReference()) return fieldConfig;
        }
        return null;
    }
    public boolean hasParentField () { return getParentField() != null; }

    /** The HTTP method to use when creating a new entity. Default value: `PUT` */
    @Getter @Setter private String createMethod = "PUT";
    /** The API endpoint to use when creating a new entity. Default value: none */
    @Getter @Setter private String createUri;

    /** The HTTP method to use when updating an entity. Default value: `POST` */
    @Getter @Setter private String updateMethod = "POST";
    /** The API endpoint to use when updating an entity. Default value: none */
    @Getter @Setter private String updateUri;

    /** The HTTP method to use when searching entities. Default value: `POST` */
    @Getter @Setter private String searchMethod = "POST";
    /** The API endpoint to use when searching entities. Default value: none */
    @Getter @Setter private String searchUri;

    /**
     * After using the `searchUri` to obtain some entities, the `searchFields` tells which fields should be
     * "columns" in the resulting data table. Fields not listed in `searchFields` will not be shown.
     * Default value: the same as listFields
     * @return a List of field names (keys found in the `fields` map) to use when displaying a list of entities.
     */
    @Setter private List<String> searchFields;
    public List<String> getSearchFields() { return !empty(searchFields) ? searchFields : getListFields(); }

    /** The HTTP method to use when deleting an entity. Default value: `DELETE` */
    @Getter @Setter private String deleteMethod = "DELETE";
    @Setter private String deleteUri;
    /** The API endpoint to use when deleting an entity. Default value: Default value: the value of `updateUri` */
    public String getDeleteUri() { return !empty(deleteUri) ? deleteUri : getUpdateUri(); }

    public void addParent(EntityConfig parentConfig) {
        for (Map.Entry<String, EntityFieldConfig> fieldConfig : parentConfig.getFields().entrySet()) {
            if (!this.fields.containsKey(fieldConfig.getKey())) {
                this.fields.put(fieldConfig.getKey(), fieldConfig.getValue());
            }
        }
    }

    /** Describes child resources of the entity. This is a map of EntityConfig name to EntityConfig. */
    @Getter @Setter private Map<String, EntityConfig> children = new HashMap<>();
    public boolean hasChildren () { return !children.isEmpty(); }

}
