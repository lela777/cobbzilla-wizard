package org.cobbzilla.wizard.dao.shard;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;

public abstract class AbstractSingleShardDAO<E extends Identifiable>
        extends AbstractCRUDDAO<E>
        implements SingleShardDAO<E> {

    @Getter @Setter private DatabaseConfiguration database;

}
