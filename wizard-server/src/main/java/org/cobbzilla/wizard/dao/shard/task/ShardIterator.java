package org.cobbzilla.wizard.dao.shard.task;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.Identifiable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @Slf4j
public abstract class ShardIterator<E extends Identifiable, D extends SingleShardDAO<E>> implements Iterator<E>, Closeable {

    @Getter @Setter private D dao;

    protected abstract String getHsql ();
    protected abstract List<Object> getArgs ();
    public abstract Object filter(E entity);

    @Getter(lazy=true) private final Iterator<E> iterator = initIterator();
    private Iterator<E> initIterator () { return dao.iterate(getHsql(), getArgs()); }

    public ShardIterator(D dao) { this.dao = dao; }

    @Override public boolean hasNext() { return getIterator().hasNext(); }

    @Override public E next() {
        try { return getIterator().next(); } catch (Exception e) { return die("next: "+e, e); }
    }

    @Override public void remove() { getIterator().remove(); }

    @Override public void close() throws IOException { dao.closeIterator(getIterator()); }

}
