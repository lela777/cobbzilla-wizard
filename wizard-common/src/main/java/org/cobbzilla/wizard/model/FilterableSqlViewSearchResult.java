package org.cobbzilla.wizard.model;

import org.cobbzilla.util.reflect.ReflectionUtil;

public interface FilterableSqlViewSearchResult extends SqlViewSearchResult {

    String[] getMatchFields();

    default boolean matches(String filter) {
        for (String field : getMatchFields()) {
            final Object value = ReflectionUtil.get(this, field, null);
            if (value != null && value.toString().contains(filter)) return true;
        }
        return false;
    }

}
