package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.SimpleShardTask;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ShardIterateTask<E extends Identifiable, D extends SingleShardDAO<E>> extends SimpleShardTask<E, D, Iterator<E>> {

    @Override protected Iterator<E> execTask() {
        Iterator<E> iter = null;
        try {
            iter = dao.iterate(hsql, values);
        } finally {
            dao.closeIterator(iter);
        }
        return iter;
    }

    @AllArgsConstructor
    public static class Factory<E extends Identifiable, D extends SingleShardDAO<E>>
            extends ShardTaskFactoryBase<E, D, Iterator<E>, Iterator<E>> {
        private String hsql;
        private List<Object> values;

        @Override public ShardIterateTask<E, D> newTask(D dao) {
            return new ShardIterateTask(dao, tasks, hsql, values);
        }
    }

    private String hsql;
    private List<Object> values;

    public ShardIterateTask(D dao, Set<ShardTask<E, D, Iterator<E>, Iterator<E>>> tasks, String hsql, List<Object> values) {
        super(dao, tasks, new ShardResultCollectorBase<Iterator<E>>());
        this.hsql = hsql;
        this.values = values;
    }

}
