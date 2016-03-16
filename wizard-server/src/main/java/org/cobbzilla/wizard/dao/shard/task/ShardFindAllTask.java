package org.cobbzilla.wizard.dao.shard.task;

import org.cobbzilla.wizard.dao.shard.SimpleShardTask;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.List;
import java.util.Set;

public abstract class ShardFindAllTask<E extends Identifiable, D extends SingleShardDAO<E>> extends SimpleShardTask<E, D, List<E>> {

    public ShardFindAllTask(D dao, Set<ShardTask<E, D, List<E>, List<E>>> tasks) {
        super(dao, tasks, new ShardResultCollectorBase<List<E>>());
    }

    @Override protected List<E> execTask() { return find(); }

    protected abstract List<E> find();

}
