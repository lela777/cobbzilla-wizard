package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.Session;
import org.springframework.orm.hibernate4.HibernateTemplate;

import java.util.List;

public interface SingleShardDAO<E extends Identifiable> extends DAO<E> {

    E findByUniqueFields(String f1, Object v1, String f2, Object v2);
    E findByUniqueFields(String f1, Object v1, String f2, Object v2, String f3, Object v3);
    List<E> findByFields(String f1, Object v1, String f2, Object v2);
    List<E> findByFields(String f1, Object v1, String f2, Object v2, String f3, Object v3);

    void initialize();
    void cleanup();

    List query(int maxResults, String hsql, Object... args);
    List query(int maxResults, String hsql, List<Object> args);

    HibernateTemplate getHibernateTemplate();
    Session readOnlySession();

    <R> List<R> search(ShardSearch search);

}
