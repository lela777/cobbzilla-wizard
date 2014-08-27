package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.ResultPage;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface DAO<E> {

    public List<E> search(ResultPage resultPage);

    public Class<? extends Map<String, String>> boundsClass();

    public E get(Serializable id);

    public List<E> findAll();
    public E findByUuid(String uuid);
    public E findByUniqueField(String field, Object value);

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
