package org.cobbzilla.wizard.spring.config.rdbms;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HibernateConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.poisonProxy;

@Configuration @Slf4j
public class RdbmsConfig {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired private HasDatabaseConfiguration configuration;
    private HasDatabaseConfiguration configuration () { return configuration; }

    @Bean public DataSource dataSource() {
        final DatabaseConfiguration dbConfiguration = configuration().getDatabase();
        final DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(dbConfiguration.getDriver());
        ds.setUrl(dbConfiguration.getUrl());
        ds.setUsername(dbConfiguration.getUser());
        ds.setPassword(dbConfiguration.getPassword());
        return ds;
    }

    @Bean public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        final HibernateTransactionManager htm = new HibernateTransactionManager();
        htm.setSessionFactory(sessionFactory);
        return htm;
    }

    @Bean public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
        final HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        return hibernateTemplate;
    }

    @Bean public LocalSessionFactoryBean sessionFactory() {
        final LocalSessionFactoryBean asfb = new LocalSessionFactoryBean();
        asfb.setNamingStrategy(ImprovedNamingStrategy.INSTANCE);
        asfb.setDataSource(dataSource());
        asfb.setHibernateProperties(hibernateProperties());
        asfb.setPackagesToScan(configuration().getDatabase().getHibernate().getEntityPackages());
        return asfb;
    }

    @Bean public Properties hibernateProperties() {
        final HibernateConfiguration hibernateConfiguration = configuration().getDatabase().getHibernate();
        final Properties properties = new Properties();
        properties.put("hibernate.dialect", hibernateConfiguration.getDialect());
        properties.put("hibernate.show_sql", hibernateConfiguration.isShowSql());
        properties.put("hibernate.hbm2ddl.auto", hibernateConfiguration.getHbm2ddlAuto());
        properties.put("javax.persistence.verification.mode", hibernateConfiguration.getValidationMode());
        return properties;
    }

    public static final int MIN_KEY_LENGTH = 15;
    @Bean public PBEStringEncryptor strongEncryptor () {

        if (!configuration.getDatabase().isEncryptionEnabled()) {
            log.warn("strongEncrypto: encryption is disabled, will not work!");
            return poisonProxy(PBEStringEncryptor.class);
        }

        final String key = configuration.getDatabase().getEncryptionKey();
        if (empty(key) || key.length() < MIN_KEY_LENGTH) die("strongEncryptor: encryption was enabled, but key was too short (min length "+MIN_KEY_LENGTH+"): '"+key+"'");

        final PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(key);
        encryptor.setAlgorithm("PBEWithMD5AndTripleDES");
        encryptor.setPoolSize(configuration.getDatabase().getEncryptorPoolSize());
        return encryptor;
    }

    @Bean public HibernatePBEStringEncryptor hibernateEncryptor() {
        final HibernatePBEStringEncryptor encryptor = new HibernatePBEStringEncryptor();
        encryptor.setEncryptor(strongEncryptor());
        encryptor.setRegisteredName("hibernateEncryptor");
        return encryptor;
    }
}
