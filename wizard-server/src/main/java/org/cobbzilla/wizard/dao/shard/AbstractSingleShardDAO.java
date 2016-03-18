package org.cobbzilla.wizard.dao.shard;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.dao.shard.task.ShardSearchTask;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public abstract class AbstractSingleShardDAO<E extends Identifiable>
        extends AbstractCRUDDAO<E>
        implements SingleShardDAO<E> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private RestServerConfiguration configuration;

    @Override public <R> List<R> search(ShardSearch search) {
        return new ShardSearchTask(this, search).execTask();
    }

    @Override public List query(int maxResults, String hsql, Object... args) {
        return query(maxResults, hsql, Arrays.asList(args));
    }

    @Override public List query(int maxResults, String hsql, List<Object> args) {
        final SessionFactory factory = getHibernateTemplate().getSessionFactory();
        StatelessSession session = null;
        try {
            session = factory.openStatelessSession();
            final Query query = session.createQuery(hsql).setMaxResults(maxResults);
            int i = 0;
            for (Object arg : args) {
                if (arg == null) {
                    die("query: null values no supported.");
                } else if (arg instanceof String) {
                    query.setString(i++, arg.toString());
                } else if (arg instanceof BigDecimal) {
                    query.setBigDecimal(i++, (BigDecimal) arg);
                } else if (arg instanceof BigInteger) {
                    query.setBigInteger(i++, (BigInteger) arg);
                } else if (arg instanceof Double) {
                    query.setDouble(i++, (Double) arg);
                } else {
                    die("query: unsupported argument type: " + arg);
                }

            }
            return query.list();
        } finally {
            if (session != null) session.close();
        }
    }

    @Override public void initialize() {}

    @Override public void cleanup() {
        try {
            getHibernateTemplate().getSessionFactory().close();
        } catch (Exception e) {
            log.warn("cleanup: error destroying session factory: "+e, e);
        }
        try {
            ((AbstractApplicationContext) configuration.getApplicationContext()).close();
        } catch (Exception e) {
            log.warn("cleanup: error destroying Spring ApplicationContext: "+e, e);
        }
    }

}
