package org.cobbzilla.wizard.dao.shard;

import org.cobbzilla.wizard.dao.shard.task.ShardTask;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.util.ResultCollector;

import java.util.Set;

public abstract class SimpleShardTask<E extends Identifiable, D extends SingleShardDAO<E>, R> extends ShardTask<E, D, R> {

    public SimpleShardTask(D dao, Set<ShardTask<E, D, R>> shardTasks, ResultCollector resultCollector) {
        super(dao, shardTasks, resultCollector, true);
    }

}
