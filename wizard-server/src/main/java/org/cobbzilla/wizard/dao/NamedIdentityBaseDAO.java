package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.NamedIdentityBase;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class NamedIdentityBaseDAO<E extends NamedIdentityBase> extends AbstractCRUDDAO<E> {

    private final Map<String, E> cache = new ConcurrentHashMap<>();

    @Override public Object preCreate(@Valid E entity) {
        cache.remove(entity.getName());
        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid E entity) {
        cache.remove(entity.getName());
        return super.preUpdate(entity);
    }

    public E findByName (String name) {
        if (empty(name)) return null;
        E thing = cache.get(name);
        if (thing == null) {
            thing = findByUniqueField("name", name);
            if (thing == null) return null;
            cache.put(name, thing);
        }
        return thing;
    }

    public E findByUuid (String name) { return findByName(name); }

    public List<E> findByNameIn(List<String> names) {
        final List<E> found = new ArrayList<>();
        if (empty(names)) return found;
        final List<String> notFoundNames = new ArrayList<>();
         for (String name : names) {
            if (cache.containsKey(name)) {
                found.add(cache.get(name));
            } else {
                notFoundNames.add(name);
            }
        }
        if (found.size() != names.size()) {
            final List<E> lookedUp = findByFieldIn("name", notFoundNames);
            for (E toCache : lookedUp) cache.put(toCache.getName(), toCache);
            found.addAll(lookedUp);
        }
        return found;
    }

    public List<E> findByNameIn(String[] names) { return findByFieldIn("name", names); }

}
