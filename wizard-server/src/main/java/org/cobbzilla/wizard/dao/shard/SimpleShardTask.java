package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.dao.shard.task.ShardResultCollector;
import org.cobbzilla.wizard.dao.shard.task.ShardTask;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Set;

public abstract class SimpleShardTask<E extends Identifiable, D extends SingleShardDAO<E>, T> extends ShardTask<E, D, T, T> {

    public SimpleShardTask(D dao, Set<ShardTask<E, D, T, T>> shardTasks, ShardResultCollector<T> resultCollector) {
        super(dao, shardTasks, resultCollector);
    }

}
