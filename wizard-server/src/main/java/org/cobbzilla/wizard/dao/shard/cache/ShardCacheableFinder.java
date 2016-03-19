package org.cobbzilla.wizard.dao.shard.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.shard.Shardable;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.wizard.dao.shard.AbstractShardedDAO.NULL_CACHE;

@AllArgsConstructor
public abstract class ShardCacheableFinder<E extends Shardable, D extends SingleShardDAO<E>> implements CacheableFinder {

    protected AbstractShardedDAO<E, D> shardedDAO;
    @Getter protected long cacheTimeoutSeconds = TimeUnit.MINUTES.toSeconds(20);

    public E get(String cacheKey, Object... args) {
        final String shardSetName = shardedDAO.getShardConfiguration().getName();
        cacheKey = shardSetName +":" + cacheKey;
        E entity = null;
        final String json = shardedDAO.getShardCache().get(cacheKey);
        if (json == null) {
            entity = (E) find(args);
            if (entity == null) {
                shardedDAO.getShardCache().set(cacheKey, NULL_CACHE, "EX", getCacheTimeoutSeconds());
                shardedDAO.getShardCache().lpush(shardedDAO.getCacheRefsKey(NULL_CACHE), cacheKey);
            } else {
                shardedDAO.getShardCache().set(cacheKey, toJsonOrDie(entity), "EX", getCacheTimeoutSeconds());
                shardedDAO.getShardCache().lpush(shardedDAO.getCacheRefsKey(entity.getUuid()), cacheKey);
            }
        } else if (!json.equals(NULL_CACHE)) {
            entity = JsonUtil.fromJsonOrDie(json, shardedDAO.getEntityClass());
            shardedDAO.getShardCache().set(cacheKey, json, "EX", getCacheTimeoutSeconds());
        }
        return entity;
    }

}
