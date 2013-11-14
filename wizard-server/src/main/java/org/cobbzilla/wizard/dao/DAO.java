package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.ResultPage;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface DAO<E> {

    public <T> List<T> query(ResultPage resultPage);

    public Class<? extends Map<String, String>> boundsClass();

    public E get(Serializable id);

    public List<E> findAll();
    public E findByUuid(String uuid);
    public List<E> search(ResultPage resultPage);
    public boolean exists(String uuid);
    public E create(@Valid E entity);
    public E createOrUpdate(@Valid E entity);
    public E update(@Valid E entity);
    public void delete(String uuid);
    public E findByUniqueField(String field, Object value);

}
