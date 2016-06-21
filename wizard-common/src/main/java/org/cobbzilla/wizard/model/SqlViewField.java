package org.cobbzilla.wizard.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static org.cobbzilla.util.string.StringUtil.snakeCaseToCamelCase;

@AllArgsConstructor @ToString
public class SqlViewField {

    @Getter @Setter private Class<? extends Identifiable> type;
    @Getter @Setter private String name;
    @Getter @Setter private String property;
    @Getter @Setter private boolean encrypted;

    public SqlViewField(String name)                    { this(null, name, snakeCaseToCamelCase(name), false); }
    public SqlViewField(String name, String property)   { this(null, name, property, false); }
    public SqlViewField(String name, boolean encrypted) { this(null, name, snakeCaseToCamelCase(name), encrypted); }
    public SqlViewField(String name, String property, boolean encrypted) { this(null, name, property, encrypted); }

    public SqlViewField(Class<? extends Identifiable> type, String name, String property)   { this(type, name, property, false); }
    public SqlViewField(Class<? extends Identifiable> type, String name, boolean encrypted) { this(type, name, snakeCaseToCamelCase(name), encrypted); }

    public String getEntity () {
        final int dotPos = property.indexOf('.');
        return dotPos == -1 ? null : property.substring(0, dotPos-1);
    }

    public boolean hasEntity () { return getEntity() != null; }

    public String getEntityProperty () {
        final int dotPos = property.indexOf('.');
        return dotPos == -1 ? property : property.substring(dotPos);
    }

}
