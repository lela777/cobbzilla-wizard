package org.cobbzilla.wizard.model;

import java.util.HashMap;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public class RelatedEntities extends HashMap<String, Identifiable> {

    public Identifiable entity(Class<? extends Identifiable> clazz) {
        Identifiable found = get(clazz.getSimpleName());
        if (found == null) {
            found = instantiate(clazz);
            put(clazz.getSimpleName(), found);
        }
        return found;
    }

}
