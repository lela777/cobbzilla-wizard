package org.cobbzilla.wizard.spring.config.rdbms;

import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HibernateConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class RdbmsConfig {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired private HasDatabaseConfiguration configuration;
    private HasDatabaseConfiguration configuration () { return configuration; }

    @Bean
    public DataSource dataSource() {
        DatabaseConfiguration dbConfiguration = configuration().getDatabase();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(dbConfiguration.getDriver());
        ds.setUrl(dbConfiguration.getUrl());
        ds.setUsername(dbConfiguration.getUser());
        ds.setPassword(dbConfiguration.getPassword());
        return ds;
    }

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager htm = new HibernateTransactionManager();
        htm.setSessionFactory(sessionFactory);
        return htm;
    }

    @Bean
    public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
        HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        return hibernateTemplate;
    }

    @Bean
    public AnnotationSessionFactoryBean sessionFactory() {
        AnnotationSessionFactoryBean asfb = new AnnotationSessionFactoryBean();
        asfb.setNamingStrategy(ImprovedNamingStrategy.INSTANCE);
        asfb.setDataSource(dataSource());
        asfb.setHibernateProperties(hibernateProperties());
        asfb.setPackagesToScan(configuration().getDatabase().getHibernate().getEntityPackages());
        return asfb;
    }

    @Bean
    public Properties hibernateProperties() {
        HibernateConfiguration hibernateConfiguration = configuration().getDatabase().getHibernate();
        Properties properties = new Properties();
        properties.put("hibernate.dialect", hibernateConfiguration.getDialect());
        properties.put("hibernate.show_sql", hibernateConfiguration.isShowSql());
        properties.put("hibernate.hbm2ddl.auto", hibernateConfiguration.getHbm2ddlAuto());
        properties.put("javax.persistence.verification.mode", hibernateConfiguration.getValidationMode());
        return properties;
    }
}
