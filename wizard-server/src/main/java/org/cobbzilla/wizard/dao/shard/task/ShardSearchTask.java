package org.cobbzilla.wizard.dao.shard.task;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.ShardSearch;
import org.cobbzilla.wizard.dao.shard.SimpleShardTask;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.util.ResultCollector;

import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.string.StringUtil.formatDurationFrom;

@Slf4j
public class ShardSearchTask <E extends Shardable, D extends SingleShardDAO<E>, R> extends SimpleShardTask<E, D, List<R>> {

    @Override public List<R> execTask() {
        final String prefix = "execTask(" + dao.getShard().getDbName() + "): ";
        long start = now();
        log.info(prefix+"starting");
        final ResultCollector collector = search.getCollector();
        for (Object entity : dao.query(search.getMaxResultsPerShard(), search.getHsql(), search.getArgs())) {
             if (cancelled.get()) break;
             if (!collector.addResult(entity)) {
                cancelTasks(); break;
            }
        }
        final List<R> sorted = search.sort(collector.getResults());
        log.info(prefix + "completed in " + formatDurationFrom(start));
        return sorted;
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
