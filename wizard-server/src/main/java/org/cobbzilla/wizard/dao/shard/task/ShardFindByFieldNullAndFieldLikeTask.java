package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.shard.Shardable;

import java.util.List;
import java.util.Set;

public class ShardFindByFieldNullAndFieldLikeTask<E extends Shardable, D extends SingleShardDAO<E>> extends ShardFindListTask<E, D> {

    @Override protected List<E> find() { return dao.findByFieldNullAndFieldLike(nullField, likeField, likeValue); }

    @AllArgsConstructor
    public static class Factory<E extends Shardable, D extends SingleShardDAO<E>>
            extends ShardTaskFactoryBase<E, D, List<E>> {
        private String nullField;
        private String likeField;
        private String likeValue;

        @Override public ShardFindByFieldNullAndFieldLikeTask<E, D> newTask(D dao) {
            return new ShardFindByFieldNullAndFieldLikeTask(dao, tasks, nullField, likeField, likeValue);
        }
    }

    private String nullField;
    private String likeField;
    private String likeValue;

    public ShardFindByFieldNullAndFieldLikeTask(D dao, Set<ShardTask<E, D, List<E>>> tasks, String nullField, String likeField, String likeValue) {
        super(dao, tasks);
        this.nullField = nullField;
        this.likeField = likeField;
        this.likeValue = likeValue;
    }

}
