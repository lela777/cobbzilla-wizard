package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.UniquelyNamedEntity;

public abstract class UniquelyNamedEntityDAO<E extends UniquelyNamedEntity> extends AbstractUniqueCRUDDAO<E> {

    public boolean forceLowercase () { return true; }

    public E findByName (String name) { return findByUniqueField("name", forceLowercase() ? name.toLowerCase() : name); }

}
