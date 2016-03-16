package org.cobbzilla.wizard.dao.shard.task;

import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.ArrayList;
import java.util.List;

public abstract class ShardIteratorFactory<E extends Identifiable, D extends SingleShardDAO<E>, R> {

    public List<ShardIterator<E, D>> iterators(List<D> daos) {
        final List<ShardIterator<E, D>> iterators = new ArrayList<>();
        for (D dao : daos) iterators.add(newIterator(dao));
        return iterators;
    }

    protected abstract ShardIterator<E, D> newIterator(D dao);

    public abstract ShardResultCollector<R> getResultCollector();
}
