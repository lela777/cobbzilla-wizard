package org.cobbzilla.wizard.dao.shard.task;

import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Set;

public interface ShardTaskFactory<E extends Identifiable, D extends SingleShardDAO<E>, R> {

    ShardTask<E, D, R> newTask (D dao);

    Set<ShardTask<E, D, R>> getTasks ();

    void cancelTasks();
}
