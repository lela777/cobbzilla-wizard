package org.cobbzilla.wizard.dao.shard;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;

@Slf4j
public abstract class AbstractSingleShardDAO<E extends Identifiable>
        extends AbstractCRUDDAO<E>
        implements SingleShardDAO<E> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired private RestServerConfiguration configuration;

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
