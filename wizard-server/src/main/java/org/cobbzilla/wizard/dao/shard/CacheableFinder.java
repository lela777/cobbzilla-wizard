package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.model.Identifiable;

public interface CacheableFinder<E extends Identifiable> {

    public E find (Object... args);

}
