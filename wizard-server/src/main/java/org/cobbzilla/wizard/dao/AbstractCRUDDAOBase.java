package org.cobbzilla.wizard.dao;

public interface AbstractCRUDDAOBase<E> {

    public E findByUniqueField(String field, Object fieldValue);

}
