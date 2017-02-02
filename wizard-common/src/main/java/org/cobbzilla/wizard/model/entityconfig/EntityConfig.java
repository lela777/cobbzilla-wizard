package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.entityconfig.annotations.*;
import org.springframework.util.ReflectionUtils;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.validation.constraints.Size;
import java.lang.reflect.Field;
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
@ToString(of="name") @Slf4j
public class EntityConfig {
    public static final String URI_CUSTOM = ":custom";
    public static final String URI_NOT_SUPPORTED = ":notSupported";

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

    /* -------------------------------------------------- */
    /* ----- Config updates from class annotations: ----- */

    /** Update properties with values from the annotations from the corresponding class. Doesn't override existing
     *  non-empty values!
     */
    public EntityConfig updateWithAnnotations() {
        String className = getClassName();

        Class<?> clazz = null;
        if (!empty(className)) {
            try {
                // Use Java's Class.forName method so we can catch (and ignore) ClassNotFoundException.
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.warn("Cannot find class with name " + className + " for entity condig");
            }
        }

        return updateWithAnnotations(clazz);
    }

    /** Update properties with values from the class' annotation. Doesn't override existing non-empty values! */
    public EntityConfig updateWithAnnotations(Class<?> clazz) {
        String clazzPackageName = null;
        if (clazz != null) {
            clazzPackageName = clazz.getPackage().getName();

            updateWithAnnotation(clazz.getAnnotation(ECTypeName.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeList.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeSearch.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeCreate.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeUpdate.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeDelete.class));
            updateWithAnnotation(clazz.getAnnotation(ECTypeURIs.class));

            updateWithAnnotation(clazz.getAnnotation(ECTypeFields.class), clazz);
        }

        for (Map.Entry<String, EntityConfig> childConfigEntry : getChildren().entrySet()) {
            EntityConfig childConfig = childConfigEntry.getValue();
            if (empty(childConfig.getClassName()) && clazzPackageName != null) {
                childConfig.setClassName(clazzPackageName + "." + childConfigEntry.getKey());
            }
            childConfig.updateWithAnnotations();
        }

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeName annotation) {
        if (annotation == null) return this;

        if (empty(name)) setName(annotation.name());
        if (empty(displayName)) setDisplayName(annotation.displayName());
        if (empty(pluralDisplayName)) setPluralDisplayName(annotation.pluralDisplayName());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeList annotation) {
        if (annotation == null) return this;

        if (empty(listFields)) setListFields(Arrays.asList(annotation.fields()));
        if (empty(listUri)) setListUri(annotation.uri());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeSearch annotation) {
        if (annotation == null) return this;

        if (empty(searchFields)) setSearchFields(Arrays.asList(annotation.fields()));
        if (empty(searchMethod)) setSearchMethod(annotation.method());
        if (empty(searchUri)) setSearchUri(annotation.uri());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeCreate annotation) {
        if (annotation == null) return this;

        if (empty(createMethod)) setCreateMethod(annotation.method());
        if (empty(createUri)) setCreateUri(annotation.uri());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeUpdate annotation) {
        if (annotation == null) return this;

        if (empty(updateMethod)) setUpdateMethod(annotation.method());
        if (empty(updateUri)) setUpdateUri(annotation.uri());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeDelete annotation) {
        if (annotation == null) return this;

        if (empty(deleteMethod)) setDeleteMethod(annotation.method());
        if (empty(deleteUri)) setDeleteUri(annotation.uri());

        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeURIs annotation) {
        if (annotation == null) return this;

        if (annotation.isListDefined()) {
            if (empty(listUri)) setListUri(annotation.baseURI());
            if (empty(listFields)) setListFields(Arrays.asList(annotation.listFields()));
        }
        if (empty(createUri) && annotation.isCreateDefined()) setCreateUri(annotation.baseURI());

        String identifiableURI = annotation.baseURI() + (annotation.baseURI().endsWith("/") ? "" : "/") +
                                 "{" + annotation.identifierInURI() + "}";

        if (empty(updateUri) && annotation.isUpdateDefined()) setUpdateUri(identifiableURI);
        if (empty(deleteUri) && annotation.isDeleteDefined()) setDeleteUri(identifiableURI);
        return this;
    }

    /** Update properties with values from the given annotation. Doesn't override existing non-empty values! */
    private EntityConfig updateWithAnnotation(ECTypeFields annotation, Class<?> clazz) {
        if (annotation == null) return this;

        if (!empty(annotation.list())) {
            final List<String> fieldNames = Arrays.asList(annotation.list());
            if (fields == null) fields = new HashMap<>(fieldNames.size());

            ReflectionUtils.doWithFields(
                    clazz,
                    new ReflectionUtils.FieldCallback() {
                        @Override public void doWith(Field field) throws IllegalArgumentException,
                                                                         IllegalAccessException {
                            EntityFieldConfig cfg = buildFieldConfig(field);
                            if (cfg != null) fields.put(field.getName(), cfg);
                        }
                    },
                    new ReflectionUtils.FieldFilter() {
                        @Override public boolean matches(Field field) {
                            String fieldName = field.getName();
                            return fieldNames.contains(fieldName) && !fields.containsKey(fieldName);
                        }
                    });
        }
        return this;
    }

    private EntityFieldConfig buildFieldConfig(Field field) {
        String fieldName = field.getName();
        EntityFieldConfig cfg = EntityFieldConfig.field(fieldName);

        if (field.isAnnotationPresent(Embedded.class)) {
            return cfg.setType(EntityFieldType.embedded).setObjectType(field.getType().getSimpleName());
        }

        if (field.isAnnotationPresent(Id.class)) {
            return cfg.setMode(EntityFieldMode.readOnly).setControl(EntityFieldControl.hidden);
        }

        if (field.getType().equals(boolean.class)) return cfg.setType(EntityFieldType.flag);

        Column columnAnnotation = field.getAnnotation(Column.class);
        if (columnAnnotation != null) {
            cfg.setLength(columnAnnotation.length());
            if (!columnAnnotation.updatable()) cfg.setMode(EntityFieldMode.createOnly);
        }

        Size sizeAnnotation = field.getAnnotation(Size.class);
        if (sizeAnnotation != null) {
            if (!cfg.hasLength() || cfg.getLength() > sizeAnnotation.max()) cfg.setLength(sizeAnnotation.max());
        }

        ECFieldReference refAnnotation = field.getAnnotation(ECFieldReference.class);
        if (refAnnotation == null) return cfg;

        cfg.setType(EntityFieldType.reference).setControl(EntityFieldControl.create(refAnnotation.control()));

        EntityFieldReference ref = new EntityFieldReference();
        ref.setEntity(refAnnotation.refEntity());
        ref.setField(refAnnotation.refField());
        ref.setDisplayField(refAnnotation.refDisplayField());
        cfg.setReference(ref);

        return cfg;
    }
}
