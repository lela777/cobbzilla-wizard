package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.SimpleShardTask;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Set;

public class ShardFindFirstByFieldTask<E extends Identifiable, D extends SingleShardDAO<E>> extends SimpleShardTask<E, D, E> {

    @Override protected E execTask() { return dao.findByUniqueField(f1, v1); }

    @AllArgsConstructor
    public static class Factory<E extends Identifiable, D extends SingleShardDAO<E>>
            extends ShardTaskFactoryBase<E, D, E, E> {
        private String f1;
        private Object v1;

        @Override public ShardFindFirstByFieldTask<E, D> newTask(D dao) {
            return new ShardFindFirstByFieldTask<>(dao, tasks, f1, v1);
        }
    }

    private String f1;
    private Object v1;

    public ShardFindFirstByFieldTask(D dao, Set<ShardTask<E, D, E, E>> tasks, String f1, Object v1) {
        super(dao, tasks, new ShardResultCollectorBase<E>());
        this.f1 = f1;
        this.v1 = v1;
    }
}
