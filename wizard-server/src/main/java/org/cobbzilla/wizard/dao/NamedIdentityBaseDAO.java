package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.NamedIdentityBase;

public class NamedIdentityBaseDAO<E extends NamedIdentityBase> extends AbstractCRUDDAO<E> {

    public E findByName (String name) { return findByUniqueField("name", name); }
    public E findByUuid (String name) { return findByName(name); }

}
