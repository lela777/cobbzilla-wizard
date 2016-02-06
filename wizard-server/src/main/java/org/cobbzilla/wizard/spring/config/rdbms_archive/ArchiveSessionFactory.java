package org.cobbzilla.wizard.spring.config.rdbms_archive;

import lombok.Delegate;
import org.hibernate.SessionFactory;

public class ArchiveSessionFactory implements SessionFactory {

    @Delegate SessionFactory delegate;

    public ArchiveSessionFactory (SessionFactory delegate) { this.delegate = delegate; }

}
