package org.cobbzilla.wizard.model;

import org.cobbzilla.util.reflect.ReflectionUtil;

public interface FilterableSqlViewSearchResult extends SqlViewSearchResult {

    SqlViewField[] getFilterFields();

    default boolean matches(String filter) {
        for (SqlViewField field : getFilterFields()) {
            if (!field.isFilter()) continue;
            final Class<? extends Identifiable> type = field.getType();
            final Object target;
            if (type != null) {
                target = field.hasEntity() ? getRelated().entity(type, field.getEntity()) : getRelated().entity(type);
            } else {
                target = this;
            }
            final Object value = ReflectionUtil.get(target, field.getEntityProperty(), null);
            if (value != null && value.toString().contains(filter)) return true;
        }
        return false;
    }

}
