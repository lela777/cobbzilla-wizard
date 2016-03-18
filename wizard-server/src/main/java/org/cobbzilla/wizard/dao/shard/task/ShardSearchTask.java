package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.ShardSearch;
import org.cobbzilla.wizard.dao.shard.SimpleShardTask;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.util.ResultCollector;

import java.util.List;
import java.util.Set;

public class ShardSearchTask <E extends Identifiable, D extends SingleShardDAO<E>, R> extends SimpleShardTask<E, D, List<R>> {

    @Override public List<R> execTask() {
        final ResultCollector collector = search.getCollector();
        for (Object entity : dao.query(search.getMaxResultsPerShard(), search.getHsql(), search.getArgs())) {
             if (cancelled.get()) break;
             if (!collector.addResult(entity)) {
                cancelTasks(); break;
            }
        }
        return search.sort(collector.getResults());
    }

    @AllArgsConstructor
    public static class Factory extends ShardTaskFactoryBase {

        private ShardSearch search;

        @Override public ShardTask newTask(SingleShardDAO dao) {
            return new ShardSearchTask(dao, tasks, search);
        }
    }

    private ShardSearch search;

    public ShardSearchTask(D dao, Set tasks, ShardSearch search) {
        super(dao, tasks, search.getCollector());
        this.search = search;
        this.setCustomCollector(search.getCollector() != null);
    }

    public ShardSearchTask(D dao, ShardSearch search) {
        super(dao, null, search.getCollector());
        this.search = search;
        this.setCustomCollector(search.getCollector() != null);
    }
}
