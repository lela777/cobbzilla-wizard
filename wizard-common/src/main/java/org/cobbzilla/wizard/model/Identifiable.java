package org.cobbzilla.wizard.model;

import org.cobbzilla.util.string.StringUtil;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

public interface Identifiable {

    String UUID = "uuid";
    String[] UUID_ARRAY = {UUID};

    int UUID_MAXLEN = BasicConstraintConstants.UUID_MAXLEN;

    String ENTITY_TYPE_HEADER_NAME = "ZZ-TYPE";

    String[] IGNORABLE_UPDATE_FIELDS = { "uuid", "name", "children", "ctime", "mtime" };
    default String[] excludeUpdateFields(boolean strict) { return StringUtil.EMPTY_ARRAY; }

    String getUuid();
    void setUuid(String uuid);

    void beforeCreate();
    void beforeUpdate();
    default void update(Identifiable thing) { copy(this, thing, null, excludeUpdateFields(true)); }

}
