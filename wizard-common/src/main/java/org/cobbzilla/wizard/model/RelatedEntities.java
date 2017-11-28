package org.cobbzilla.wizard.model;

import java.util.HashMap;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.string.StringUtil.uncapitalize;

public class RelatedEntities extends HashMap<String, Identifiable> {

    public Identifiable entity(Class<? extends Identifiable> clazz) {
        return entity(clazz, uncapitalize(clazz.getSimpleName()));
    }

    public Identifiable entity(final Class<? extends Identifiable> clazz, String name) {
        return computeIfAbsent(name, k -> instantiate(clazz));
    }

}
