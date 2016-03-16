package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.List;
import java.util.Set;

public class ShardFindByFieldTask<E extends Identifiable, D extends SingleShardDAO<E>> extends ShardFindAllTask<E, D> {

    @Override protected List<E> find() { return dao.findByField(field, value); }

    @AllArgsConstructor
    public static class Factory<E extends Identifiable, D extends SingleShardDAO<E>>
            extends ShardTaskFactoryBase<E, D, List<E>, List<E>> {
        private String field;
        private Object value;

        @Override public ShardFindByFieldTask<E, D> newTask(D dao) {
            return new ShardFindByFieldTask(dao, tasks, field, value);
        }
    }

    private String field;
    private Object value;

    public ShardFindByFieldTask(D dao, Set<ShardTask<E, D, List<E>, List<E>>> tasks, String field, Object value) {
        super(dao, tasks);
        this.field = field;
        this.value = value;
    }

}
