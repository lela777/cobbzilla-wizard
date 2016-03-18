package org.cobbzilla.wizard.dao.shard.task;

import lombok.Getter;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class ShardTaskFactoryBase<E extends Identifiable, D extends SingleShardDAO<E>, R> implements ShardTaskFactory<E, D, R> {

    @Getter protected final Set<ShardTask<E, D, R>> tasks = new ConcurrentSkipListSet<>();

    @Override public void cancelTasks() { for (ShardTask task : getTasks()) task.cancel(); }

}
