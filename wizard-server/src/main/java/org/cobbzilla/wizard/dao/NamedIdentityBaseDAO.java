package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.NamedIdentityBase;

import java.util.List;

public class NamedIdentityBaseDAO<E extends NamedIdentityBase> extends AbstractCRUDDAO<E> {

    public E findByName (String name) { return findByUniqueField("name", name); }
    public E findByUuid (String name) { return findByName(name); }

    public List<E> findByNameIn(List<String> names) { return findByFieldIn("name", names); }
    public List<E> findByNameIn(String[] names) { return findByFieldIn("name", names); }

}
