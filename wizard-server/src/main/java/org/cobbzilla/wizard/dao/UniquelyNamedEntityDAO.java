package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.UniquelyNamedEntity;

public class UniquelyNamedEntityDAO<E extends UniquelyNamedEntity> extends AbstractCRUDDAO<E> {

    public boolean forceLowercase () { return true; }

    public E findByName (String name) { return findByUniqueField("name", forceLowercase() ? name.toLowerCase() : name); }

}
