package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.ResultPage;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;

public interface DAO<E> {

    public SearchResults<E> search(ResultPage resultPage);

    public E get(Serializable id);

    public List<E> findAll();
    public E findByUuid(String uuid);
    public E findByUniqueField(String field, Object value);
    public List<E> findByField(String field, Object value);

    public boolean exists(String uuid);

    public Object preCreate(@Valid E entity);
    public E create(@Valid E entity);
    public E createOrUpdate(@Valid E entity);
    public E postCreate(E entity, Object context);

    public Object preUpdate(@Valid E entity);
    public E update(@Valid E entity);
    public E postUpdate(@Valid E entity, Object context);

    public void delete(String uuid);

}
