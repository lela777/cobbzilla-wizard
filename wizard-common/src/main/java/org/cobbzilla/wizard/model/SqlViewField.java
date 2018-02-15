package org.cobbzilla.wizard.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.string.StringUtil.snakeCaseToCamelCase;

@Accessors(chain=true) @ToString
public class SqlViewField {

    @Getter @Setter private Class<? extends Identifiable> type;
    @Getter @Setter private String name;
    @Getter @Setter private String property;
    @Getter @Setter private boolean encrypted;
    @Getter @Setter private boolean filter;
    @Getter @Setter private Class fieldType;

    @Getter @Setter private SqlViewFieldSetter setter;
    public boolean hasSetter () { return setter != null; }

    public SqlViewField(String name) {
        this.name = name;
        this.property = snakeCaseToCamelCase(name);
    }

    public SqlViewField(Class<? extends Identifiable> type, String name) {
        this.type = type;
        this.name = name;
    }

    public SqlViewField property(String property) { this.property = property; return this; }
    public SqlViewField setter(SqlViewFieldSetter setter) { this.setter = setter; return this; }
    public SqlViewField encrypted() { encrypted = true; return this; }
    public SqlViewField encrypted(Class type) { encrypted(); this.fieldType = type; return this; }
    public SqlViewField encrypted(boolean encrypted) { this.encrypted = encrypted; return this; }
    public SqlViewField entity(String entity) { this.entity = entity; return this; }
    public SqlViewField filter () { filter = true; return this; }
    public SqlViewField filter (boolean filter) { this.filter = filter; return this; }
    public SqlViewField fieldType (Class type) { this.fieldType = type; return this; }

    private String entity;

    public String getEntity () {
        if (entity != null) return entity;
        if (type == null) return null;
        final int dotPos = property.indexOf('.');
        return dotPos == -1 ? null : property.substring(0, dotPos);
    }

    public boolean hasEntity () { return getEntity() != null; }

    public String getEntityProperty () {
        if (type == null) return property;
        final int dotPos = property.indexOf('.');
        return dotPos == -1 ? property : property.substring(dotPos+1);
    }

}
