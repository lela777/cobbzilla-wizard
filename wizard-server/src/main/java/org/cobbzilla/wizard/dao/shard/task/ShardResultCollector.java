package org.cobbzilla.wizard.dao.shard.task;

import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.util.ResultCollector;

public interface ShardResultCollector<E> extends ResultCollector {

    DAO<E> getDAO();
    void setDAO(DAO<E> dao);

}
