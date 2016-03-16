package org.cobbzilla.wizard.dao.shard.task;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public class ShardIteratorTask<E extends Identifiable, D extends SingleShardDAO<E>, T, R>
        extends ShardTask<E, D, T, R> {

    @Getter @Setter private ShardIterator<E, D> iterator;

    public ShardIteratorTask(D dao,
                             Set<ShardTask<E, D, T, R>> tasks,
                             ShardIterator<E, D> iterator,
                             ShardResultCollector<R> resultCollector) {
        super(dao, tasks, resultCollector);
        this.iterator = iterator;
    }

    @Override protected T execTask() {
        try {
            E entity;
            while (iterator.hasNext() && !cancelled.get()) {
                try {
                    entity = iterator.next();
                    if (entity != null) {
                        resultCollector.addResult(iterator.filter(entity));
                        if (resultCollector.size() > resultCollector.getMaxResults()) cancelTasks();
                    }

                } catch (Exception e) {
                    return die("execTask: error iterating: "+e, e);
                }
            }
        } finally {
            log.info("execTask: completed");
        }
        return null;
    }

    public List<R> getResults () { return resultCollector.getResults(); }

}
