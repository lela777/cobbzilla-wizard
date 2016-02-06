package org.cobbzilla.wizard.spring.config.rdbms_archive;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.spring.config.rdbms.RdbmsConfigCommon;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

@Configuration @Slf4j
public class ArchiveRdbmsConfig extends RdbmsConfigCommon {

    @Bean public LocalSessionFactoryBean archiveSessionFactory() {
        final LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setNamingStrategy(ImprovedNamingStrategy.INSTANCE);
        factory.setDataSource(dataSource());
        factory.setHibernateProperties(hibernateProperties());
        // load archive packages only
        factory.setPackagesToScan(configuration().getDatabase().getHibernate().getArchivePackages());
        return factory;
    }

    @Bean public ArchiveHibernateTemplate archiveHibernateTemplate() {
        return new ArchiveHibernateTemplate(new HibernateTemplate(archiveSessionFactory().getObject()));
    }

}
