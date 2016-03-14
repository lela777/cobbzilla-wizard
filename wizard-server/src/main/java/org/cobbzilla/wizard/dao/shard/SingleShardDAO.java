package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.springframework.orm.hibernate4.HibernateTemplate;

import java.io.Serializable;
import java.util.List;

public interface SingleShardDAO<E extends Identifiable> {

    void setDatabase(DatabaseConfiguration configuration);

    DatabaseConfiguration getDatabase();

    void setHibernateTemplate(HibernateTemplate hibernateTemplate);

    E get(Serializable id);

    List<E> findByField(String field, Object value);

    E findByUniqueFields(String f1, Object v1, String f2, Object v2);

    E create(E entity);

    E createOrUpdate(E entity);

    E update(E entity);

    void delete(String uuid);

}
