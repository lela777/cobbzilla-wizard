package org.cobbzilla.wizard.dao.shard.cache;

import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.shard.Shardable;

public class ShardableShardCacheableIdentityFinder<E extends Shardable, D extends SingleShardDAO<E>> extends ShardCacheableFinder<E, D> {

    public ShardableShardCacheableIdentityFinder(AbstractShardedDAO<E, D> shardedDAO, long timeout) { super(shardedDAO, timeout); }

    @Override public E find(Object... args) {
        final String id = args[0].toString();
        return shardedDAO.getDAO(id).get(id);
    }
}
