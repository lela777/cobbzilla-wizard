package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Iterator;
import java.util.List;

public interface SingleShardDAO<E extends Identifiable> extends DAO<E> {

    E findByUniqueFields(String f1, Object v1, String f2, Object v2);
    E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3);
    List<E> findByFields(String f1, Object v1, String f2, Object v2);

    void cleanup();

    Iterator<E> iterate(String hsql, Object... args);
    void closeIterator(Iterator<E> iterator);

}
