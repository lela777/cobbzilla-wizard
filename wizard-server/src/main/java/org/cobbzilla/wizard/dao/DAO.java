package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.search.ResultPage;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface DAO<E extends Identifiable> {

    String SQL_QUERY = "SQL:";

    Class<E> getEntityClass();

    SearchResults<E> search(ResultPage resultPage);
    SearchResults<E> search(ResultPage resultPage, String entityType);

    E get(Serializable id);

    List<E> findAll();
    E findByUuid(String uuid);
    E findByUniqueField(String field, Object value);
    List<E> findByField(String field, Object value);
    List<E> findByFieldLike(String field, String value);
    List<E> findByFieldEqualAndFieldLike(String eqField, Object eqValue, String likeField, String likeValue);
    List<E> findByFieldIn(String field, Object[] values);
    List<E> findByFieldIn(String field, Collection<?> values);

    boolean exists(String uuid);

    Object preCreate(@Valid E entity);
    E create(@Valid E entity);
    E createOrUpdate(@Valid E entity);
    E postCreate(E entity, Object context);

    Object preUpdate(@Valid E entity);
    E update(@Valid E entity);
    E postUpdate(E entity, Object context);

    void delete(String uuid);
    void delete(Collection<E> entities);

}
