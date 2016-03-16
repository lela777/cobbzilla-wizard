package org.cobbzilla.wizard.dao.shard.task;

import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Set;

public interface ShardTaskFactory<E extends Identifiable, D extends SingleShardDAO<E>, T, R> {

    ShardTask<E, D, T, R> newTask (D dao);

    Set<ShardTask<E, D, T, R>> getTasks ();

    void cancelTasks();
}
