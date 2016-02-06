package org.cobbzilla.wizard.spring.config.rdbms_archive;

import lombok.Delegate;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ArchiveHibernateTemplate extends HibernateTemplate {

    @Delegate private HibernateTemplate delegate;

    public ArchiveHibernateTemplate(HibernateTemplate delegate) { this.delegate = delegate; }

}
