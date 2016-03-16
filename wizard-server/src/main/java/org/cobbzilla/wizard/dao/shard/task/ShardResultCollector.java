package org.cobbzilla.wizard.dao.shard.task;

import java.util.List;

public interface ShardResultCollector<R> {

    List<R> getResults();

    void addResult(Object thing);

    int size();

    int getMaxResults();

}
